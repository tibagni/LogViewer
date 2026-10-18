#!/usr/bin/env python3
"""
Automated AI Code Review for GitHub Pull Requests.
Zero third-party dependencies - standard library only.

================================================================================
LOCAL TESTING INSTRUCTIONS
================================================================================
1. Create a local `.env` file in the project root (ignored by git):
     cp .env.example .env
   Then edit `.env` and set your API key:
     LLM_API_KEY=your_gemini_api_key_here

2. Test review on ANY GitHub PR without posting comments (dry-run):
     python3 scripts/ai_pr_review.py --dry-run --pr 101

3. Inspect the full assembled prompt sent to the LLM (-v / --verbose):
     python3 scripts/ai_pr_review.py --dry-run --pr 101 -v

4. Test review on your current local branch changes against master:
     python3 scripts/ai_pr_review.py --dry-run

5. Run against a different base branch:
     python3 scripts/ai_pr_review.py --dry-run --base master

Available CLI options:
  --dry-run, -n      Print review to console without posting or updating PR comments
  --verbose, -v      Print the full assembled prompt to stdout before calling the LLM
  --pr <number>      Review a specific GitHub PR by number (uses `gh pr diff` and `gh pr view`)
  --base <branch>    Base branch to diff against (default: from env or 'master')
  --model <name>     Preferred model name (e.g. 'gemini-flash-latest', 'gemini-flash-lite-latest')
  --author <login>   PR author username (default: from PR_AUTHOR env or gh pr view)
  --auto-approve-authors <logins>
                     Comma-separated GitHub usernames eligible for auto-approval on LGTM
  --timezone <zone>  Timezone for review timestamp (default: from REVIEW_TIMEZONE or 'America/Chicago')
  --env-file <path>  Path to local env file (default: '.env')
================================================================================
"""

import argparse
from datetime import datetime, timezone
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from typing import Any, Dict, List, Optional, Set, Tuple

try:
    from zoneinfo import ZoneInfo
except ImportError:
    ZoneInfo = None  # type: ignore

PRIMARY_COMMENT_MARKER = "<!-- ai-code-review -->"
RECOGNIZED_COMMENT_MARKERS = ("<!-- ai-code-review -->", "<!-- gemini-code-review -->")
EXCLUDED_EXTENSIONS = (
    ".png", ".jpg", ".jpeg", ".gif", ".ico", ".svg",
    ".jar", ".zip", ".tar", ".gz",
    ".class", ".bin"
)

PREFERRED_MODELS_ORDER = [
    # Tier 1: Primary Gemini Flash models (top speed and high-grade reasoning)
    "gemini-flash-latest",
    "gemini-3.8-flash",
    "gemini-3.7-flash",
    "gemini-3.6-flash",
    "gemini-3.5-flash",

    # Tier 2: Gemini Flash-Lite models (resilient high-capacity fallback with fast response)
    "gemini-flash-lite-latest",
    "gemini-3.5-flash-lite",
    "gemini-3.1-flash-lite",

    # Tier 3: Gemini Preview & Pro models
    "gemini-3-flash-preview",
    "gemini-3.1-pro-preview",
    "gemini-pro-latest",
]


def get_project_root() -> str:
    """Finds the git repository root or the parent directory of this script."""
    try:
        res = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            capture_output=True,
            text=True,
            check=True
        )
        root = res.stdout.strip()
        if root and os.path.isdir(root):
            return root
    except Exception:
        pass
    return os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def resolve_repo_path(path: str) -> str:
    """Resolves a file path, checking the current working directory and the repository root."""
    if os.path.isabs(path) or os.path.exists(path):
        return path
    root_relative = os.path.join(get_project_root(), path)
    if os.path.exists(root_relative):
        return root_relative
    return path


def load_env_file(env_path: str = ".env") -> None:
    """Loads environment variables from a local .env file without overriding existing env vars."""
    resolved_path = resolve_repo_path(env_path)
    if os.path.exists(resolved_path):
        try:
            with open(resolved_path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith("#") and "=" in line:
                        key, val = line.split("=", 1)
                        key = key.strip()
                        val = val.strip().strip("'\"")
                        if key and key not in os.environ:
                            os.environ[key] = val
        except Exception as e:
            print(f"Warning: Could not read env file '{resolved_path}': {e}")


def get_repo_name() -> str:
    """Gets repository name from GITHUB_REPOSITORY env var or git remote origin."""
    repo = os.environ.get("GITHUB_REPOSITORY", "").strip()
    if repo:
        return repo
    try:
        res = subprocess.run(
            ["git", "config", "--get", "remote.origin.url"],
            capture_output=True,
            text=True,
            check=True
        )
        url = res.stdout.strip()
        if "github.com" in url:
            part = url.split("github.com")[-1].lstrip("/:")
            if part.endswith(".git"):
                part = part[:-4]
            return part
    except Exception:
        pass
    return ""


def get_head_sha(pr_number: Optional[str] = None) -> str:
    """Finds the HEAD commit SHA for the PR."""
    sha = os.environ.get("HEAD_SHA", "").strip()
    if sha:
        return sha
    if pr_number:
        try:
            res = subprocess.run(
                ["gh", "pr", "view", str(pr_number), "--json", "headRefOid", "-q", ".headRefOid"],
                capture_output=True,
                text=True,
                check=True
            )
            val = res.stdout.strip()
            if val:
                return val
        except Exception:
            pass
    try:
        res = subprocess.run(
            ["git", "rev-parse", "HEAD"],
            capture_output=True,
            text=True,
            check=True
        )
        return res.stdout.strip()
    except Exception:
        return ""


def parse_valid_diff_lines(diff: str) -> Dict[str, Set[int]]:
    """Parses a unified diff and returns a mapping of file_path -> set of valid line numbers (side RIGHT)."""
    valid_lines: Dict[str, Set[int]] = {}
    current_file: Optional[str] = None
    current_line = 0

    for line in diff.splitlines():
        if line.startswith("+++ b/"):
            current_file = line[6:].strip()
            if current_file not in valid_lines:
                valid_lines[current_file] = set()
        elif line.startswith("+++ /dev/null"):
            current_file = None
        elif line.startswith("@@ "):
            # Format: @@ -old_start,old_len +new_start,new_len @@
            m = re.search(r"\+(\d+)(?:,(\d+))?", line)
            if m:
                current_line = int(m.group(1))
        elif current_file and current_line:
            if line.startswith("+") or line.startswith(" "):
                valid_lines[current_file].add(current_line)
                current_line += 1
            elif line.startswith("-"):
                pass  # Deleted line in old file, does not advance current_line in new file

    return valid_lines


def normalize_path(path: str) -> str:
    """Normalizes relative file path from model output."""
    path = path.strip().lstrip("/")
    if path.startswith("b/"):
        path = path[2:]
    elif path.startswith("./"):
        path = path[2:]
    return path


def extract_inline_comments(text: str) -> Tuple[str, List[Dict]]:
    """Extracts and removes ```inline-comments ... ``` JSON block from review text."""
    pattern = r"```inline-comments\s*([\s\S]*?)\s*```"
    match = re.search(pattern, text)
    if not match:
        return text.strip(), []

    clean_text = re.sub(pattern, "", text).strip()
    json_payload = match.group(1).strip()
    try:
        data = json.loads(json_payload)
        if isinstance(data, list):
            valid = []
            for item in data:
                if isinstance(item, dict) and "path" in item and "line" in item and "body" in item:
                    try:
                        line_num = int(item["line"])
                        valid.append({
                            "path": normalize_path(str(item["path"])),
                            "line": line_num,
                            "body": str(item["body"]).strip()
                        })
                    except (ValueError, TypeError):
                        continue
            return clean_text, valid
    except Exception as e:
        print(f"Notice: Could not parse inline comments JSON: {e}")
    return clean_text, []


def filter_inline_comments(
    comments: List[Dict],
    valid_diff_lines: Dict[str, Set[int]]
) -> Tuple[List[Dict], List[Dict]]:
    """Filters comments into (valid_comments, discarded_comments) based on diff hunk lines."""
    valid = []
    discarded = []
    for c in comments:
        path = c.get("path", "")
        line = c.get("line")
        if path in valid_diff_lines and line in valid_diff_lines[path]:
            valid.append(c)
        else:
            discarded.append(c)
    return valid, discarded


def parse_auto_approve_authors(raw: Optional[str]) -> Set[str]:
    """Parses a comma- or whitespace-separated list of GitHub usernames into a lowercase set."""
    if not raw:
        return set()
    parts = re.split(r"[,\s]+", raw.strip())
    return {p.strip().lower() for p in parts if p.strip()}


def is_verdict_lgtm(review_text: str) -> bool:
    """Checks if the review verdict indicates approval (LGTM or LGTM with Suggestions).

    Returns True if verdict contains LGTM and does not indicate Needs Work.
    """
    verdict_match = re.search(
        r"(?:###|\*\*|##)?\s*(?:\d+\.\s*)?Overall Verdict[:\s]*([\s\S]*?)(?=(?:###|\*\*|##\s+[A-Za-z]|\n\n\d+\.|\Z))",
        review_text,
        re.IGNORECASE
    )
    if not verdict_match:
        return False

    target = verdict_match.group(1)

    # If the verdict explicitly contains "needs work", do not approve
    if re.search(r"\bneeds\s+work\b", target, re.IGNORECASE):
        return False

    # If the verdict contains "lgtm" (matches both "LGTM" and "LGTM with Suggestions")
    if re.search(r"\blgtm\b", target, re.IGNORECASE):
        return True

    return False


def get_git_diff(base_ref: str) -> str:
    """Extracts git diff against origin/<base_ref>, filtering out binary/asset files."""
    try:
        subprocess.run(
            ["git", "rev-parse", "--verify", f"origin/{base_ref}"],
            check=True,
            capture_output=True,
            text=True
        )
        target_ref = f"origin/{base_ref}"
    except subprocess.CalledProcessError:
        target_ref = base_ref

    result = subprocess.run(
        ["git", "diff", "--name-only", f"{target_ref}...HEAD"],
        capture_output=True,
        text=True,
        check=True
    )
    changed_files = [
        f for f in result.stdout.strip().splitlines()
        if f and not f.lower().endswith(EXCLUDED_EXTENSIONS)
    ]

    if not changed_files:
        return ""

    diff_cmd = ["git", "diff", f"{target_ref}...HEAD", "--"] + changed_files
    diff_res = subprocess.run(diff_cmd, capture_output=True, text=True, check=True)
    return diff_res.stdout


def get_pr_diff(pr_number: str) -> str:
    """Fetches diff for a specific PR using GitHub CLI, filtering out excluded file extensions."""
    res = subprocess.run(
        ["gh", "pr", "diff", str(pr_number), "--name-only"],
        capture_output=True,
        text=True,
        check=True
    )
    changed_files = [
        f for f in res.stdout.strip().splitlines()
        if f and not f.lower().endswith(EXCLUDED_EXTENSIONS)
    ]
    if not changed_files:
        return ""

    diff_res = subprocess.run(
        ["gh", "pr", "diff", str(pr_number)],
        capture_output=True,
        text=True,
        check=True
    )
    return diff_res.stdout


def get_pr_metadata(pr_number: str) -> Dict:
    """Fetches PR metadata (title, body, commits, human discussion, headRefOid, author) via GitHub CLI."""
    cmd = ["gh", "pr", "view", str(pr_number), "--json", "title,body,commits,comments,headRefOid,author"]
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, check=True)
        data = json.loads(res.stdout)

        # Filter out comments containing our review markers
        filtered_comments = []
        for c in data.get("comments", []):
            body = c.get("body", "")
            if not any(marker in body for marker in RECOGNIZED_COMMENT_MARKERS):
                author = c.get("author", {}).get("login", "unknown")
                filtered_comments.append({"author": author, "body": body})
        data["filtered_comments"] = filtered_comments
        return data
    except Exception as e:
        print(f"Warning: Could not fetch metadata for PR #{pr_number}: {e}")
        return {}


def get_local_commit_log(base_ref: str) -> List[str]:
    """Gets commit messages for local branch against base_ref."""
    target_ref = f"origin/{base_ref}"
    try:
        subprocess.run(
            ["git", "rev-parse", "--verify", target_ref],
            check=True,
            capture_output=True,
            text=True
        )
    except Exception:
        target_ref = base_ref

    try:
        res = subprocess.run(
            ["git", "log", f"{target_ref}...HEAD", "--oneline"],
            capture_output=True,
            text=True,
            check=True
        )
        return [line.strip() for line in res.stdout.strip().splitlines() if line.strip()]
    except Exception:
        return []


def load_review_guidelines(guidelines_path: str = ".github/review_guidelines.md") -> Tuple[str, str]:
    """Loads review instructions from markdown file if present, returning (content, source_description)."""
    resolved_path = resolve_repo_path(guidelines_path)
    if os.path.exists(resolved_path):
        with open(resolved_path, "r", encoding="utf-8") as f:
            return f.read(), f"guidelines defined in [`{guidelines_path}`]({guidelines_path})"
    return (
        "Perform a thorough, expert-level code review focusing on concurrency, architecture, and edge cases.",
        "default built-in guidelines"
    )


def build_prompt(
    guidelines: str,
    diff: str,
    title: Optional[str] = None,
    description: Optional[str] = None,
    commits: Optional[List[str]] = None,
    comments: Optional[List[Dict[str, str]]] = None
) -> str:
    """Assembles the system guidelines, PR context, and code diff into the LLM review prompt."""
    sections = [guidelines.strip(), "\n---"]

    has_context = bool(title or description or commits or comments)
    if has_context:
        sections.append("## Pull Request Context:")
        if title:
            sections.append(f"- **Title**: {title.strip()}")
        if description and description.strip():
            sections.append(f"- **Description**:\n{description.strip()}")
        if commits:
            commits_text = "\n".join(f"- {c}" for c in commits)
            sections.append(f"### Commits:\n{commits_text}")
        if comments:
            comments_text = "\n".join(f"- @{c.get('author', 'user')}: {c.get('body', '').strip()}" for c in comments)
            sections.append(f"### Existing Discussion:\n{comments_text}")
        sections.append("\n---")

    inline_instructions = """### 💬 Optional Inline Comments:
If you have specific line-level recommendations for modified code lines, you may include an optional JSON block at the very end of your review inside a ```inline-comments``` code block:
```inline-comments
[
  {
    "path": "path/to/File.kt",
    "line": 42,
    "body": "Specific recommendation for this line"
  }
]
```
Rules for inline comments:
- Only reference files and line numbers that appear within the provided code diff hunks.
- Keep the comment body concise, polite, and actionable.
- If there are no specific line-level issues, omit the ```inline-comments``` block completely."""

    sections.append(inline_instructions)
    sections.append(f"## Pull Request Code Diff to Review:\n\n```diff\n{diff.strip()}\n```")
    return "\n\n".join(sections)


def get_available_models(api_key: str) -> Tuple[str, List[str]]:
    """Discovers available models supporting generateContent, prioritizing stable flash and flash-lite models."""
    for version in ["v1beta", "v1"]:
        url = f"https://generativelanguage.googleapis.com/{version}/models"
        headers = {"x-goog-api-key": api_key}
        try:
            req = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req, timeout=15) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                models = [
                    m["name"].replace("models/", "")
                    for m in data.get("models", [])
                    if "generateContent" in m.get("supportedGenerationMethods", [])
                ]
                print(f"Discovered {len(models)} models for API version {version}.")

                # Disallow specialized/non-chat models (audio, vision-only, robotics, etc.)
                disallowed_keywords = [
                    "tts", "image", "transcribe", "clip", "banana", "robotics", "computer-use", "lyria"
                ]
                usable_models = [m for m in models if not any(x in m for x in disallowed_keywords)]

                ordered_candidates = []
                # 1. Preferred models in explicit tier order
                for pref in PREFERRED_MODELS_ORDER:
                    if pref in usable_models and pref not in ordered_candidates:
                        ordered_candidates.append(pref)

                # 2. Any other remaining Gemini Flash models (non-lite first, then lite)
                for m in sorted(usable_models, reverse=True):
                    if "gemini" in m and "flash" in m and "lite" not in m and m not in ordered_candidates:
                        ordered_candidates.append(m)
                for m in sorted(usable_models, reverse=True):
                    if "gemini" in m and "flash" in m and "lite" in m and m not in ordered_candidates:
                        ordered_candidates.append(m)

                # 3. Any other remaining Gemini models (e.g., Pro, Preview, etc.)
                for m in sorted(usable_models, reverse=True):
                    if "gemini" in m and m not in ordered_candidates:
                        ordered_candidates.append(m)

                # 4. Any other non-Gemma models
                for m in usable_models:
                    if not m.startswith("gemma") and m not in ordered_candidates:
                        ordered_candidates.append(m)

                # 5. Gemma models strictly as last resort
                for m in sorted(usable_models, reverse=True):
                    if m.startswith("gemma") and m not in ordered_candidates:
                        ordered_candidates.append(m)

                if ordered_candidates:
                    return version, ordered_candidates
        except Exception as e:
            print(f"Notice: Could not list models on {version}: {e}")

    return "v1beta", PREFERRED_MODELS_ORDER


def call_llm_api(
    api_key: str,
    prompt: str,
    preferred_model: Optional[str] = None,
    max_retries: int = 2,
    retry_delay_seconds: float = 3.0
) -> Tuple[str, str]:
    """Calls the LLM REST API with resilient fallback across available models, retrying transient 503 errors.

    Returns:
        Tuple of (review_text, model_name_used)
    """
    version, models = get_available_models(api_key)
    if preferred_model:
        models = [preferred_model] + [m for m in models if m != preferred_model]

    last_error = None
    for model in models:
        attempts = 0
        while attempts <= max_retries:
            attempts += 1
            if attempts > 1:
                backoff = retry_delay_seconds * (2 ** (attempts - 2))
                print(f"Waiting {backoff:.1f}s before retrying '{model}'...")
                time.sleep(backoff)

            attempt_str = f" [attempt {attempts}/{max_retries + 1}]" if max_retries > 0 else ""
            print(f"Attempting review with LLM model '{model}' ({version}){attempt_str}...")
            url = f"https://generativelanguage.googleapis.com/{version}/models/{model}:generateContent"
            headers = {
                "Content-Type": "application/json",
                "x-goog-api-key": api_key,
            }
            payload = {
                "contents": [
                    {
                        "role": "user",
                        "parts": [{"text": prompt}]
                    }
                ],
                "generationConfig": {
                    "temperature": 0.2
                }
            }

            req = urllib.request.Request(
                url,
                data=json.dumps(payload).encode("utf-8"),
                headers=headers,
                method="POST"
            )

            try:
                with urllib.request.urlopen(req, timeout=120) as response:
                    body = json.loads(response.read().decode("utf-8"))
                    candidates = body.get("candidates", [])
                    if not candidates:
                        raise ValueError(f"No candidates returned by LLM: {body}")
                    text = candidates[0].get("content", {}).get("parts", [{}])[0].get("text", "")
                    if not text:
                        raise ValueError(f"Empty text in candidate response: {body}")
                    print(f"✅ Successfully generated review with '{model}'!")
                    return text, model
            except urllib.error.HTTPError as e:
                err_msg = e.read().decode("utf-8", errors="replace")
                try:
                    parsed_err = json.loads(err_msg)
                    detail = parsed_err.get("error", {}).get("message", err_msg.strip()[:200])
                except Exception:
                    detail = err_msg.strip()[:200]

                last_error = f"HTTP {e.code} for model {model} ({version}): {detail}"
                if e.code == 503 and attempts <= max_retries:
                    print(f"Model '{model}' temporarily unavailable (HTTP 503): High demand. Retrying...")
                    continue
                if e.code == 429:
                    print(f"Model '{model}' rate-limited (HTTP 429: quota exceeded). Trying next model...")
                    break
                print(f"Model '{model}' failed (HTTP {e.code}): {detail}")
                break
            except Exception as e:
                last_error = str(e)
                print(f"Model '{model}' unexpected error: {e}")
                break

    raise RuntimeError(f"All available LLM models failed. Last error: {last_error}")


def find_existing_comment(pr_number: str, repo: str) -> Optional[int]:
    """Finds an existing AI review comment on the PR by searching for recognized markers."""
    cmd = ["gh", "api", f"repos/{repo}/issues/{pr_number}/comments", "--paginate", "--slurp"]
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, check=True)
        raw_output = res.stdout.strip()
        if not raw_output:
            return None
        pages = json.loads(raw_output)
        if pages and isinstance(pages[0], list):
            comments = [c for page in pages for c in page]
        else:
            comments = pages

        for comment in comments:
            if isinstance(comment, dict):
                body = comment.get("body", "")
                if any(marker in body for marker in RECOGNIZED_COMMENT_MARKERS):
                    return comment.get("id")
    except Exception as e:
        print(f"Warning: Could not search existing PR comments: {e}")
    return None


def get_review_timestamp(
    dt: Optional[datetime] = None,
    tz_name: Optional[str] = None,
    commit_sha: Optional[str] = None
) -> str:
    """
    Formats a review timestamp showing local time and UTC in parentheses.
    Default timezone is America/Chicago (or REVIEW_TIMEZONE env var).
    Optionally appends the commit SHA if provided.

    Example outputs:
      "2026-10-17 20:51:02 CDT (2026-10-18 01:51:02 UTC) • commit `abc1234`"
      "2026-10-17 20:51:02 UTC • commit `abc1234`"
    """
    if dt is None:
        dt = datetime.now(timezone.utc)
    elif dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)

    target_tz_name = (
        tz_name or
        os.environ.get("REVIEW_TIMEZONE", "America/Chicago")
    ).strip()

    local_str: Optional[str] = None
    local_dt: Optional[datetime] = None
    if ZoneInfo and target_tz_name and target_tz_name.upper() != "UTC":
        try:
            target_tz = ZoneInfo(target_tz_name)
            local_dt = dt.astimezone(target_tz)
            local_str = local_dt.strftime("%Y-%m-%d %H:%M:%S %Z")
        except Exception:
            local_str = None

    utc_dt = dt.astimezone(timezone.utc)
    utc_full_str = utc_dt.strftime("%Y-%m-%d %H:%M:%S UTC")

    if not local_str or not local_dt:
        time_part = utc_full_str
    else:
        if local_dt.date() == utc_dt.date():
            utc_part = utc_dt.strftime("%H:%M:%S UTC")
        else:
            utc_part = utc_full_str
        time_part = f"{local_str} ({utc_part})"

    if commit_sha:
        short_sha = commit_sha.strip()[:7]
        return f"{time_part} • commit `{short_sha}`"
    return time_part


def post_or_update_review_comment(pr_number: str, repo: str, review_text: str) -> None:
    """Posts or updates the top-level PR review comment in the issue timeline."""
    full_comment = f"{PRIMARY_COMMENT_MARKER}\n{review_text}"

    existing_id = find_existing_comment(pr_number, repo)
    payload = json.dumps({"body": full_comment}).encode("utf-8")
    if existing_id:
        print(f"Updating existing AI review comment #{existing_id} on PR #{pr_number}...")
        cmd = [
            "gh", "api",
            "--method", "PATCH",
            f"repos/{repo}/issues/comments/{existing_id}",
            "--input", "-"
        ]
        subprocess.run(cmd, input=payload, check=True)
        print("Review comment updated successfully.")
    else:
        print(f"Posting new AI review comment on PR #{pr_number}...")
        cmd = [
            "gh", "api",
            "--method", "POST",
            f"repos/{repo}/issues/{pr_number}/comments",
            "--input", "-"
        ]
        subprocess.run(cmd, input=payload, check=True)
        print("Review comment posted successfully.")


def post_pull_request_review(
    pr_number: str,
    repo: str,
    commit_sha: Optional[str],
    body: str,
    event: str = "COMMENT",
    inline_comments: Optional[List[Dict]] = None
) -> bool:
    """Submits a pull request review (APPROVE or COMMENT) with optional inline comments."""
    review_payload: Dict[str, Any] = {
        "event": event,
        "body": body,
    }
    if commit_sha:
        review_payload["commit_id"] = commit_sha
    if inline_comments:
        review_payload["comments"] = [
            {
                "path": c["path"],
                "line": c["line"],
                "side": "RIGHT",
                "body": c["body"]
            }
            for c in inline_comments
        ]

    data = json.dumps(review_payload)
    cmd = [
        "gh", "api",
        "--method", "POST",
        f"repos/{repo}/pulls/{pr_number}/reviews",
        "--input", "-"
    ]
    try:
        subprocess.run(cmd, input=data, capture_output=True, text=True, check=True)
        action_desc = "approved pull request" if event == "APPROVE" else (
            f"posted {len(inline_comments)} inline review comments" if inline_comments else "submitted review"
        )
        print(f"Successfully {action_desc} on PR #{pr_number}.")
        return True
    except subprocess.CalledProcessError as e:
        err_msg = (e.stderr or e.stdout or str(e)).strip()
        print(f"Warning: Failed to submit PR review (event={event}): {err_msg}")
        if event == "APPROVE":
            print("💡 Tip: To enable automatic PR approvals, ensure 'Allow GitHub Actions to create and approve pull requests' is enabled in repo Settings -> Actions -> General.")
        return False
    except Exception as e:
        print(f"Warning: Failed to submit PR review: {e}")
        return False


def post_inline_review_comments(
    pr_number: str,
    repo: str,
    commit_sha: str,
    inline_comments: List[Dict],
    event: str = "COMMENT",
    body: str = "### 🤖 AI Code Review (Inline Feedback)\nSee inline suggestions on modified lines below."
) -> bool:
    """Posts inline comments on the pull request diff via GitHub Reviews API."""
    return post_pull_request_review(
        pr_number=pr_number,
        repo=repo,
        commit_sha=commit_sha,
        body=body,
        event=event,
        inline_comments=inline_comments
    )


def log_to_step_summary(review_text: str) -> None:
    """Appends the review to GitHub Step Summary if running in GitHub Actions."""
    summary_file = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_file:
        try:
            with open(summary_file, "a", encoding="utf-8") as f:
                f.write(f"\n{review_text}\n")
        except Exception as e:
            print(f"Warning: Could not write to GITHUB_STEP_SUMMARY: {e}")


def parse_args(argv: Optional[List[str]] = None) -> argparse.Namespace:
    """Parses command-line arguments."""
    parser = argparse.ArgumentParser(
        description="Automated AI Code Review for GitHub Pull Requests.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    parser.add_argument(
        "--dry-run", "-n",
        action="store_true",
        help="Print the review to stdout without posting or updating comments on GitHub."
    )
    parser.add_argument(
        "--verbose", "-v",
        action="store_true",
        help="Print the full assembled prompt sent to the LLM."
    )
    parser.add_argument(
        "--pr",
        type=str,
        default=None,
        help="Specific GitHub Pull Request number to review (fetches diff via `gh pr diff`)."
    )
    parser.add_argument(
        "--base",
        type=str,
        default=None,
        help="Base branch to diff against when not reviewing a specific PR (default: from env or 'master')."
    )
    parser.add_argument(
        "--model",
        type=str,
        default=None,
        help="Preferred model name (e.g. 'gemini-flash-latest' or 'gemini-flash-lite-latest')."
    )
    parser.add_argument(
        "--author",
        type=str,
        default=None,
        help="PR author username (default: from PR_AUTHOR env or gh pr view)."
    )
    parser.add_argument(
        "--auto-approve-authors",
        type=str,
        default=None,
        help="Comma-separated GitHub usernames eligible for auto-approval on LGTM (default: from AUTO_APPROVE_AUTHORS env)."
    )
    parser.add_argument(
        "--timezone",
        type=str,
        default=None,
        help="Timezone for review timestamp in comments (default: from REVIEW_TIMEZONE env or 'America/Chicago')."
    )
    parser.add_argument(
        "--env-file",
        type=str,
        default=".env",
        help="Path to local .env file (default: '.env')."
    )
    return parser.parse_args(argv)


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)

    # 1. Load local env file if present
    load_env_file(args.env_file)

    # 2. Check API key
    api_key = (os.environ.get("LLM_API_KEY", "").strip() or
               os.environ.get("GEMINI_API_KEY", "").strip())
    if not api_key:
        print("⚠️ Notice: Neither LLM_API_KEY nor GEMINI_API_KEY is set. Skipping automated code review.")
        return 0

    preferred_model = (
        args.model or
        os.environ.get("LLM_MODEL", "").strip() or
        os.environ.get("GEMINI_MODEL", "").strip() or
        os.environ.get("AI_REVIEW_MODEL", "").strip() or
        None
    )
    pr_number = args.pr or os.environ.get("PR_NUMBER", "").strip()
    repo = get_repo_name()
    base_ref = args.base or os.environ.get("BASE_REF", "master").strip()
    head_sha = get_head_sha(pr_number)

    title = None
    description = None
    commits = []
    comments = []

    # 3. Extract diff and PR context
    if pr_number:
        print(f"Fetching diff and context for PR #{pr_number}...")
        diff = get_pr_diff(pr_number)
        meta = get_pr_metadata(pr_number)
        title = meta.get("title")
        description = meta.get("body")
        for c in meta.get("commits", []):
            oid = c.get("oid", "")[:7]
            headline = c.get("messageHeadline", "")
            commits.append(f"`{oid}`: {headline}" if oid else headline)
        comments = meta.get("filtered_comments", [])
    else:
        print(f"Fetching diff against base branch '{base_ref}'...")
        diff = get_git_diff(base_ref)
        commits = get_local_commit_log(base_ref)

    if not diff.strip():
        print("No reviewable code changes found.")
        if not args.dry_run and pr_number and repo:
            timestamp_str = get_review_timestamp(tz_name=args.timezone, commit_sha=head_sha)
            review_body = (
                f"### 🤖 AI Code Review\n\n"
                f"No code changes requiring review were detected in this pull request.\n\n"
                f"---\n"
                f"*Reviewed at: {timestamp_str}*"
            )
            post_or_update_review_comment(pr_number, repo, review_body)
        return 0

    print(f"Extracted diff ({len(diff)} characters). Loading review guidelines...")
    valid_diff_lines = parse_valid_diff_lines(diff)
    guidelines, source_description = load_review_guidelines()
    prompt = build_prompt(
        guidelines=guidelines,
        diff=diff,
        title=title,
        description=description,
        commits=commits,
        comments=comments
    )

    if args.verbose:
        print("\n" + "=" * 80)
        print("VERBOSE: Full Prompt Sent to LLM")
        print("=" * 80)
        print(prompt)
        print("=" * 80 + "\n")

    print("Requesting code review from LLM API...")
    llm_output = call_llm_api(api_key, prompt, preferred_model=preferred_model)
    if isinstance(llm_output, tuple):
        raw_review_output, model_used = llm_output
    else:
        raw_review_output = llm_output
        model_used = preferred_model or "unknown"

    clean_review_text, raw_inline_comments = extract_inline_comments(raw_review_output)
    valid_inline, discarded_inline = filter_inline_comments(raw_inline_comments, valid_diff_lines)

    if discarded_inline:
        print(f"Notice: Omitted {len(discarded_inline)} inline comments because target lines were outside diff hunks.")

    auto_approve_authors = parse_auto_approve_authors(
        args.auto_approve_authors or os.environ.get("AUTO_APPROVE_AUTHORS")
    )
    pr_author = (
        args.author or
        meta.get("author", {}).get("login", "").strip() or
        os.environ.get("PR_AUTHOR", "").strip()
    )
    is_lgtm = is_verdict_lgtm(clean_review_text)
    can_auto_approve = bool(
        pr_author and
        pr_author.lower() in auto_approve_authors and
        is_lgtm
    )

    model_attribution = f" (`{model_used}`)" if model_used else ""
    timestamp_str = get_review_timestamp(tz_name=args.timezone, commit_sha=head_sha)
    formatted_review = f"""### 🤖 AI Code Review

{clean_review_text.strip()}

---
*Reviewed by AI{model_attribution} using {source_description}.*  
*Reviewed at: {timestamp_str}*
"""

    if args.dry_run:
        print("\n" + "=" * 80)
        print("DRY RUN: Generated AI Code Review (Not posted to GitHub)")
        print("=" * 80)
        print(formatted_review)
        if valid_inline:
            print(f"\n💬 Inline Comments to Post ({len(valid_inline)}):")
            for c in valid_inline:
                print(f"  • {c['path']}:{c['line']} -> {c['body']}")
        if discarded_inline:
            print(f"\n⚠️ Discarded Inline Comments (Outside diff hunks - {len(discarded_inline)}):")
            for c in discarded_inline:
                print(f"  • {c['path']}:{c['line']} -> {c['body']}")
        if can_auto_approve:
            print(f"\n🎉 Auto-Approval Check: Author '{pr_author}' matches AUTO_APPROVE_AUTHORS and verdict is LGTM.")
            print(f"   [DRY RUN] Would submit PR review with event: APPROVE")
        elif pr_author and pr_author.lower() in auto_approve_authors:
            print(f"\n⚠️ Auto-Approval Check: Author '{pr_author}' matches AUTO_APPROVE_AUTHORS, but verdict is not LGTM. Skipping approval.")
        elif auto_approve_authors:
            print(f"\nℹ️ Auto-Approval Check: Author '{pr_author or 'unknown'}' is not in AUTO_APPROVE_AUTHORS ({auto_approve_authors}). Skipping approval.")
        print("=" * 80)
        return 0

    log_to_step_summary(formatted_review)

    if not pr_number or not repo:
        print("Error: PR_NUMBER and GITHUB_REPOSITORY (or remote.origin.url) are required to post comments.")
        print("Use --dry-run to test locally without posting comments.")
        return 1

    # 1. ALWAYS post or update the single persistent top-level review comment in the conversation timeline
    post_or_update_review_comment(pr_number, repo, formatted_review)

    # 2. If eligible for auto-approval, submit a Pull Request Review with event APPROVE
    if can_auto_approve:
        approval_body = (
            f"### 🤖 AI Code Review (Approved)\n\n"
            f"Approved pull request based on automated review verdict (LGTM).\n\n"
            f"---\n"
            f"*Reviewed by AI{model_attribution} using {source_description}.*  \n"
            f"*Reviewed at: {timestamp_str}*"
        )
        post_pull_request_review(
            pr_number=pr_number,
            repo=repo,
            commit_sha=head_sha,
            body=approval_body,
            event="APPROVE",
            inline_comments=valid_inline if valid_inline else None
        )
    elif valid_inline and head_sha:
        # If there are valid inline comments and a commit SHA, post inline comments to the diff
        post_inline_review_comments(pr_number, repo, head_sha, valid_inline)

    return 0


if __name__ == "__main__":
    sys.exit(main())
