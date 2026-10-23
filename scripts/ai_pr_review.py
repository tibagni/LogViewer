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
from dataclasses import dataclass, field
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
    # Tier 1: Primary Gemini Flash & Frontier Reasoning models
    "gemini-flash-latest",
    "gemini-4-flash",
    "gemini-4-argon",
    "gemini-3.8-flash",
    "gemini-3.7-flash",
    "gemini-3.6-flash",
    "gemini-3.5-flash",

    # Tier 2: Gemini Flash-Lite models (resilient high-capacity fallback with fast response)
    "gemini-flash-lite-latest",
    "gemini-3.5-flash-lite",
    "gemini-3.1-flash-lite",

    # Tier 3: Gemini Preview & Pro models
    "gemini-4-pro",
    "gemini-3-flash-preview",
    "gemini-3.1-pro-preview",
    "gemini-pro-latest",
]

MAX_CONTEXT_FILES = 3
MAX_CONTEXT_FILE_LINES = 600
MAX_CONTEXT_FILE_BYTES = 50 * 1024  # 50 KB
CONTEXT_REQUESTS_PATTERN = r"```(?:context-requests|context_requests)\s*([\s\S]*?)\s*```"


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


def parse_context_requests(review_text: str) -> List[str]:
    """Extracts requested file paths from ```context-requests code block."""
    match = re.search(CONTEXT_REQUESTS_PATTERN, review_text, re.IGNORECASE)
    if not match:
        return []
    raw_block = match.group(1).strip()
    files: List[str] = []
    for line in raw_block.splitlines():
        line = line.strip().strip("-*` ")
        if line and not line.startswith("#"):
            normalized = normalize_path(line)
            if normalized and normalized not in files:
                files.append(normalized)
    return files


def strip_context_requests(text: str) -> str:
    """Removes any ```context-requests ... ``` blocks from the review text."""
    cleaned = re.sub(CONTEXT_REQUESTS_PATTERN, "", text, flags=re.IGNORECASE)
    return re.sub(r"\n{3,}", "\n\n", cleaned).strip()


def fetch_file_context(
        repo_root: str,
        requested_files: List[str],
        max_files: int = MAX_CONTEXT_FILES,
        max_lines: int = MAX_CONTEXT_FILE_LINES,
        max_bytes: int = MAX_CONTEXT_FILE_BYTES
) -> Dict[str, str]:
    """Safely reads requested files from the repository root with strict bounds."""
    results: Dict[str, str] = {}
    normalized_root = os.path.realpath(repo_root)

    for raw_path in requested_files[:max_files]:
        clean_path = normalize_path(raw_path.strip().strip("'\"`"))
        if not clean_path:
            continue

        candidate_path = os.path.realpath(os.path.join(normalized_root, clean_path))
        if not candidate_path.startswith(normalized_root):
            print(f"Warning: Rejected context file outside repo root: {clean_path}")
            continue

        if not os.path.isfile(candidate_path):
            print(f"Notice: Requested context file not found: {clean_path}")
            continue

        _, ext = os.path.splitext(candidate_path)
        if ext.lower() in EXCLUDED_EXTENSIONS:
            print(f"Notice: Skipping binary or excluded file: {clean_path}")
            continue

        try:
            with open(candidate_path, "r", encoding="utf-8", errors="replace") as f:
                content = f.read(max_bytes)
                lines = content.splitlines()
                if len(lines) > max_lines:
                    content = "\n".join(
                        lines[:max_lines]) + f"\n\n// ... [truncated after {max_lines} lines for context limits] ..."
                results[clean_path] = content
        except Exception as e:
            print(f"Warning: Could not read context file '{clean_path}': {e}")

    return results


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
        comments: Optional[List[Dict[str, str]]] = None,
        allow_context_requests: bool = True
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

    if allow_context_requests:
        context_request_instructions = """### 📂 Optional Context Request (Two-Pass Review):
If you need to verify method signatures, interface contracts, or class implementations from unmodified files in the repository before finalizing your findings, you may request up to 3 files using a ```context-requests``` code block:
```context-requests
path/to/File1.kt
path/to/File2.java
```
Rules for context requests:
- Specify exact repository-relative paths (e.g. `src/main/java/com/tibagni/logviewer/MainView.kt`).
- Only request files whose declarations or contracts you directly need to verify.
- Maximum 3 files.
- Always provide your best provisional review in this response regardless of whether you request context."""
        sections.append(context_request_instructions)

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
- Only emit inline comments for genuine logic defects, concurrency violations, or high-risk design flaws.
- Never emit inline comments for cosmetic preferences, pixel dimensions, defensive null checks, or speculative nitpicks.
- Keep the comment body concise, polite, and actionable.
- If there are no specific line-level issues, omit the ```inline-comments``` block completely."""

    sections.append(inline_instructions)
    sections.append(f"## Pull Request Code Diff to Review:\n\n```diff\n{diff.strip()}\n```")
    return "\n\n".join(sections)


def build_second_pass_prompt(
        guidelines: str,
        diff: str,
        first_pass_review: str,
        file_contexts: Dict[str, str],
        title: Optional[str] = None,
        description: Optional[str] = None
) -> str:
    """Assembles the second-pass prompt including original diff, draft review, and requested file contents."""
    sections = [guidelines.strip(), "\n---"]

    if title or description:
        sections.append("## Pull Request Context:")
        if title:
            sections.append(f"- **Title**: {title.strip()}")
        if description and description.strip():
            sections.append(f"- **Description**:\n{description.strip()}")
        sections.append("\n---")

    sections.append(f"## Pull Request Code Diff:\n\n```diff\n{diff.strip()}\n```\n\n---")

    sections.append("## Your Provisional Draft Review (Pass 1):\n\n" + first_pass_review.strip() + "\n\n---")

    files_section = ["## Requested Repository File Context:"]
    for path, content in sorted(file_contexts.items()):
        files_section.append(f"### File: `{path}`\n```\n{content}\n```")
    sections.append("\n\n".join(files_section) + "\n\n---")

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
- Only emit inline comments for genuine logic defects, concurrency violations, or high-risk design flaws.
- Never emit inline comments for cosmetic preferences, pixel dimensions, defensive null checks, or speculative nitpicks.
- Keep the comment body concise, polite, and actionable.
- If there are no specific line-level issues, omit the ```inline-comments``` block completely."""

    pass2_instructions = f"""## Second-Pass Verification Instructions:
You previously generated the draft review above and requested additional repository file context to verify method signatures, interface contracts, or class implementations. The exact contents of the requested files are provided above.

Review Directives:
1. **Contract & API Verification**: Cross-reference any findings, suggested method calls, properties, or lifecycle assumptions in your draft against the confirmed declarations in the provided file context.
2. **Eliminate Ungrounded Assumptions**: If any method, property, or lifecycle behavior you recommended in your draft does not actually exist in the provided source code, eliminate or adjust that finding.
3. **Refine Findings**: Update your Overall Verdict, Key Findings, and inline comments based on confirmed codebase realities. If all concerns are resolved and the code is sound, elevate your verdict to ✅ LGTM.
4. **Finalized Output**: Output your complete, finalized review in the required GitHub Flavored Markdown format.
5. Do not include any ```context-requests``` block in this second-pass response.

{inline_instructions}"""

    sections.append(pass2_instructions)
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
            print(
                "💡 Tip: To enable automatic PR approvals, ensure 'Allow GitHub Actions to create and approve pull requests' is enabled in repo Settings -> Actions -> General.")
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
    parser.add_argument(
        "--no-two-pass",
        action="store_true",
        help="Disable two-pass context retrieval (forces single-pass review)."
    )
    return parser.parse_args(argv)


@dataclass
class ChangeContext:
    diff: str
    title: Optional[str] = None
    description: Optional[str] = None
    commits: List[str] = field(default_factory=list)
    comments: List[Dict[str, str]] = field(default_factory=list)
    pr_number: Optional[str] = None
    head_sha: Optional[str] = None
    pr_author: Optional[str] = None


def get_api_key() -> str:
    return (os.environ.get("LLM_API_KEY", "").strip() or
            os.environ.get("GEMINI_API_KEY", "").strip())


def get_preferred_model(args: argparse.Namespace) -> Optional[str]:
    return (args.model or
            os.environ.get("LLM_MODEL", "").strip() or
            os.environ.get("GEMINI_MODEL", "").strip() or
            os.environ.get("AI_REVIEW_MODEL", "").strip() or
            None)


def extract_change_context(
    pr_number: Optional[str],
    base_ref: str,
    head_sha: Optional[str] = None,
    explicit_author: Optional[str] = None
) -> ChangeContext:
    """Extracts git diff and metadata for a specific PR or local branch comparison."""
    if pr_number:
        print(f"Fetching diff and context for PR #{pr_number}...")
        diff = get_pr_diff(pr_number)
        meta = get_pr_metadata(pr_number)
        title = meta.get("title")
        description = meta.get("body")
        commits: List[str] = []
        for c in meta.get("commits", []):
            oid = c.get("oid", "")[:7]
            headline = c.get("messageHeadline", "")
            commits.append(f"`{oid}`: {headline}" if oid else headline)
        comments = meta.get("filtered_comments", [])
        pr_author = (
            explicit_author or
            meta.get("author", {}).get("login", "").strip() or
            os.environ.get("PR_AUTHOR", "").strip() or
            None
        )
        return ChangeContext(
            diff=diff,
            title=title,
            description=description,
            commits=commits,
            comments=comments,
            pr_number=pr_number,
            head_sha=head_sha,
            pr_author=pr_author
        )

    print(f"Fetching diff against base branch '{base_ref}'...")
    diff = get_git_diff(base_ref)
    commits = get_local_commit_log(base_ref)
    pr_author = explicit_author or os.environ.get("PR_AUTHOR", "").strip() or None
    return ChangeContext(
        diff=diff,
        title=None,
        description=None,
        commits=commits,
        comments=[],
        pr_number=None,
        head_sha=head_sha,
        pr_author=pr_author
    )


def handle_empty_diff(
    change: ChangeContext,
    repo: Optional[str],
    dry_run: bool,
    timezone_name: Optional[str]
) -> bool:
    """Checks if the diff is empty. If empty, optionally posts notification and returns True to finish early."""
    if change.diff.strip():
        return False

    print("No reviewable code changes found.")
    if not dry_run and change.pr_number and repo:
        timestamp_str = get_review_timestamp(tz_name=timezone_name, commit_sha=change.head_sha)
        review_body = (
            f"### 🤖 AI Code Review\n\n"
            f"No code changes requiring review were detected in this pull request.\n\n"
            f"---\n"
            f"*Reviewed at: {timestamp_str}*"
        )
        post_or_update_review_comment(change.pr_number, repo, review_body)
    return True


def resolve_context_files(
    review_output: str,
    repo_root: str
) -> Dict[str, str]:
    """Parses requested context files from review output and loads their contents from disk."""
    requested_files = parse_context_requests(review_output)
    if not requested_files:
        return {}

    print(f"Reviewer requested additional context for {len(requested_files)} file(s): {', '.join(requested_files)}")
    file_contexts = fetch_file_context(repo_root, requested_files)
    if not file_contexts:
        print("Notice: None of the requested context files could be loaded. Proceeding with Pass 1 review.")
        return {}

    print(f"Loaded {len(file_contexts)} context file(s). Executing Pass 2 verification...")
    return file_contexts


def run_second_pass_if_needed(
    api_key: str,
    guidelines: str,
    change: ChangeContext,
    first_pass_review: str,
    model_used: str,
    preferred_model: Optional[str] = None,
    verbose: bool = False
) -> Tuple[str, str, List[str]]:
    """Checks if the reviewer requested additional context files and executes Pass 2 verification if so.

    Returns:
        Tuple of (final_review_text, model_name_used, list_of_verified_file_paths)
    """
    file_contexts = resolve_context_files(first_pass_review, get_project_root())
    if not file_contexts:
        return first_pass_review, model_used, []

    try:
        pass2_prompt = build_second_pass_prompt(
            guidelines=guidelines,
            diff=change.diff,
            first_pass_review=first_pass_review,
            file_contexts=file_contexts,
            title=change.title,
            description=change.description
        )
        if verbose:
            print("\n" + "=" * 80)
            print("VERBOSE: Pass 2 Prompt Sent to LLM")
            print("=" * 80)
            print(pass2_prompt)
            print("=" * 80 + "\n")

        pass2_output, pass2_model = call_llm_api(
            api_key=api_key,
            prompt=pass2_prompt,
            preferred_model=preferred_model or model_used
        )
        verified_files = sorted(file_contexts.keys())
        print(f"Pass 2 verification completed successfully using {pass2_model}.")
        return pass2_output, pass2_model, verified_files
    except Exception as e:
        print(f"Warning: Pass 2 verification failed: {e}. Falling back to Pass 1 review.")
        return first_pass_review, model_used, []


def print_dry_run_results(
    formatted_review: str,
    valid_inline: List[Dict[str, Any]],
    discarded_inline: List[Dict[str, Any]],
    can_auto_approve: bool,
    pr_author: Optional[str],
    auto_approve_authors: Set[str]
) -> None:
    """Prints the full dry-run review, inline comments, and auto-approval evaluation to stdout."""
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
        print("   [DRY RUN] Would submit PR review with event: APPROVE")
    elif pr_author and pr_author.lower() in auto_approve_authors:
        print(
            f"\n⚠️ Auto-Approval Check: Author '{pr_author}' matches AUTO_APPROVE_AUTHORS, but verdict is not LGTM. Skipping approval."
        )
    elif auto_approve_authors:
        print(
            f"\nℹ️ Auto-Approval Check: Author '{pr_author or 'unknown'}' is not in AUTO_APPROVE_AUTHORS ({auto_approve_authors}). Skipping approval."
        )
    print("=" * 80)


def main(argv: Optional[List[str]] = None) -> int:
    args = parse_args(argv)

    # 1. Load local env file if present
    load_env_file(args.env_file)

    # 2. Retrieve variables and parameters
    api_key = get_api_key()
    preferred_model = get_preferred_model(args)
    pr_number = args.pr or os.environ.get("PR_NUMBER", "").strip()
    repo = get_repo_name()
    base_ref = args.base or os.environ.get("BASE_REF", "master").strip()
    head_sha = get_head_sha(pr_number)
    two_pass_enabled = not (
        args.no_two_pass or
        os.environ.get("AI_REVIEW_DISABLE_TWO_PASS", "").strip().lower() in ("1", "true", "yes")
    )

    # 3. Check if we have what is needed to continue. Finish early if we don't
    if not api_key:
        print("⚠️ Notice: Neither LLM_API_KEY nor GEMINI_API_KEY is set. Skipping automated code review.")
        return 0

    # 4. Extract diff and change context
    change = extract_change_context(
        pr_number=pr_number,
        base_ref=base_ref,
        head_sha=head_sha,
        explicit_author=args.author
    )

    # 5. Check if we have what to review. Finish early if we don't
    if handle_empty_diff(change, repo, args.dry_run, args.timezone):
        return 0

    # 6. Load review guidelines and assemble prompt
    print(f"Extracted diff ({len(change.diff)} characters). Loading review guidelines...")
    valid_diff_lines = parse_valid_diff_lines(change.diff)
    guidelines, source_description = load_review_guidelines()
    prompt = build_prompt(
        guidelines=guidelines,
        diff=change.diff,
        title=change.title,
        description=change.description,
        commits=change.commits,
        comments=change.comments,
        allow_context_requests=two_pass_enabled
    )

    if args.verbose:
        print("\n" + "=" * 80)
        print("VERBOSE: Full Prompt Sent to LLM" + (" (Pass 1)" if two_pass_enabled else ""))
        print("=" * 80)
        print(prompt)
        print("=" * 80 + "\n")

    # 7. Request code review (Pass 1)
    print("Requesting code review from LLM API" + (" (Pass 1)..." if two_pass_enabled else "..."))
    raw_review_output, model_used = call_llm_api(api_key, prompt, preferred_model=preferred_model)

    # 8. Second-pass context verification (if requested and enabled)
    verified_files: List[str] = []
    if two_pass_enabled:
        raw_review_output, model_used, verified_files = run_second_pass_if_needed(
            api_key=api_key,
            guidelines=guidelines,
            change=change,
            first_pass_review=raw_review_output,
            model_used=model_used,
            preferred_model=preferred_model,
            verbose=args.verbose
        )

    # 9. Clean output and extract inline comments
    raw_review_output = strip_context_requests(raw_review_output)
    clean_review_text, raw_inline_comments = extract_inline_comments(raw_review_output)
    clean_review_text = strip_context_requests(clean_review_text)
    valid_inline, discarded_inline = filter_inline_comments(raw_inline_comments, valid_diff_lines)

    if discarded_inline:
        print(f"Notice: Omitted {len(discarded_inline)} inline comments because target lines were outside diff hunks.")

    # 10. Check auto-approval eligibility
    auto_approve_authors = parse_auto_approve_authors(
        args.auto_approve_authors or os.environ.get("AUTO_APPROVE_AUTHORS")
    )
    is_lgtm = is_verdict_lgtm(clean_review_text)
    can_auto_approve = bool(
        change.pr_author and
        change.pr_author.lower() in auto_approve_authors and
        is_lgtm
    )

    # 11. Format final review markdown
    context_note = f", context-verified: {', '.join(f'`{f}`' for f in verified_files)}" if verified_files else ""
    model_attribution = f" (`{model_used}`{context_note})" if model_used else ""
    timestamp_str = get_review_timestamp(tz_name=args.timezone, commit_sha=change.head_sha)
    formatted_review = f"""### 🤖 AI Code Review

{clean_review_text.strip()}

---
*Reviewed by AI{model_attribution} using {source_description}.*  
*Reviewed at: {timestamp_str}*
"""

    # 12. Handle dry-run or local execution (print to stdout and finish)
    is_dry_run = args.dry_run or not change.pr_number or not repo
    if is_dry_run:
        if not args.dry_run:
            print("\nNotice: No PR number or repository specified. Running in dry-run mode.")
        print_dry_run_results(
            formatted_review=formatted_review,
            valid_inline=valid_inline,
            discarded_inline=discarded_inline,
            can_auto_approve=can_auto_approve,
            pr_author=change.pr_author,
            auto_approve_authors=auto_approve_authors
        )
        return 0

    log_to_step_summary(formatted_review)

    # 13. Post or update the persistent top-level review comment
    post_or_update_review_comment(change.pr_number, repo, formatted_review)

    # 14. Submit auto-approval review or post inline comments
    if can_auto_approve:
        approval_body = (
            f"### 🤖 AI Code Review (Approved)\n\n"
            f"Approved pull request based on automated review verdict (LGTM).\n\n"
            f"---\n"
            f"*Reviewed by AI{model_attribution} using {source_description}.*  \n"
            f"*Reviewed at: {timestamp_str}*"
        )
        post_pull_request_review(
            pr_number=change.pr_number,
            repo=repo,
            commit_sha=change.head_sha,
            body=approval_body,
            event="APPROVE",
            inline_comments=valid_inline if valid_inline else None
        )
    elif valid_inline and change.head_sha:
        # If there are valid inline comments and a commit SHA, post inline comments to the diff
        post_inline_review_comments(change.pr_number, repo, change.head_sha, valid_inline)

    return 0


if __name__ == "__main__":
    sys.exit(main())
