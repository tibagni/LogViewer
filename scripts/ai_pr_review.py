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
  --env-file <path>  Path to local env file (default: '.env')
================================================================================
"""

import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
from typing import Dict, List, Optional, Tuple

PRIMARY_COMMENT_MARKER = "<!-- ai-code-review -->"
RECOGNIZED_COMMENT_MARKERS = ("<!-- ai-code-review -->", "<!-- gemini-code-review -->")
EXCLUDED_EXTENSIONS = (
    ".png", ".jpg", ".jpeg", ".gif", ".ico", ".svg",
    ".jar", ".zip", ".tar", ".gz",
    ".class", ".bin"
)

PREFERRED_MODELS_ORDER = [
    "gemini-flash-latest",
    "gemini-2.0-flash",
    "gemini-1.5-flash",
    "gemini-3.8-flash",
    "gemini-3.7-flash",
    "gemini-3.6-flash",
    "gemini-3.5-flash",
    "gemini-pro-latest",
]


def load_env_file(env_path: str = ".env") -> None:
    """Loads environment variables from a local .env file without overriding existing env vars."""
    if os.path.exists(env_path):
        try:
            with open(env_path, "r", encoding="utf-8") as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith("#") and "=" in line:
                        key, val = line.split("=", 1)
                        key = key.strip()
                        val = val.strip().strip("'\"")
                        if key and key not in os.environ:
                            os.environ[key] = val
        except Exception as e:
            print(f"Warning: Could not read env file '{env_path}': {e}")


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
    """Fetches PR metadata (title, body, commits, human discussion) via GitHub CLI."""
    cmd = ["gh", "pr", "view", str(pr_number), "--json", "title,body,commits,comments"]
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
    if os.path.exists(guidelines_path):
        with open(guidelines_path, "r", encoding="utf-8") as f:
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

    sections.append(f"## Pull Request Code Diff to Review:\n\n```diff\n{diff.strip()}\n```")
    return "\n\n".join(sections)


def get_available_models(api_key: str) -> Tuple[str, List[str]]:
    """Discovers available models supporting generateContent, prioritizing stable flash models."""
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

                ordered_candidates = []
                for pref in PREFERRED_MODELS_ORDER:
                    if pref in models and pref not in ordered_candidates:
                        ordered_candidates.append(pref)

                remaining_flash = [
                    m for m in models
                    if "flash" in m and not any(x in m for x in ["preview", "tts", "image", "omni", "lite"])
                    and m not in ordered_candidates
                ]
                ordered_candidates.extend(sorted(remaining_flash, reverse=True))

                for m in models:
                    if m not in ordered_candidates and not any(x in m for x in ["tts", "image"]):
                        ordered_candidates.append(m)

                if ordered_candidates:
                    return version, ordered_candidates
        except Exception as e:
            print(f"Notice: Could not list models on {version}: {e}")

    return "v1beta", PREFERRED_MODELS_ORDER


def call_llm_api(api_key: str, prompt: str, preferred_model: Optional[str] = None) -> str:
    """Calls the LLM REST API with resilient fallback across available models."""
    version, models = get_available_models(api_key)
    if preferred_model:
        models = [preferred_model] + [m for m in models if m != preferred_model]

    last_error = None
    for model in models:
        print(f"Attempting review with LLM model '{model}' ({version})...")
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
                return text
        except urllib.error.HTTPError as e:
            err_msg = e.read().decode("utf-8", errors="replace")
            last_error = f"HTTP {e.code} for model {model} ({version}): {err_msg}"
            print(f"Model '{model}' failed: {last_error}")
            continue
        except Exception as e:
            last_error = str(e)
            print(f"Model '{model}' unexpected error: {e}")
            continue

    raise RuntimeError(f"All available LLM models failed. Last error: {last_error}")


def find_existing_comment(pr_number: str, repo: str) -> Optional[int]:
    """Finds an existing AI review comment on the PR by searching for recognized markers.

    Note on GitHub REST API:
    In GitHub's API data model, Pull Requests inherit from Issues. The main
    conversation/timeline comments are accessed via:
        repos/{repo}/issues/{pr_number}/comments
    (The `repos/{repo}/pulls/{pr_number}/comments` endpoint is strictly for inline diff line comments).
    """
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


def post_or_update_review_comment(pr_number: str, repo: str, review_text: str) -> None:
    """Posts or updates the PR review comment, and outputs to GITHUB_STEP_SUMMARY if available."""
    full_comment = f"{PRIMARY_COMMENT_MARKER}\n{review_text}"

    # Also log to GitHub Step Summary
    summary_file = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_file:
        try:
            with open(summary_file, "a", encoding="utf-8") as f:
                f.write(f"\n{review_text}\n")
        except Exception as e:
            print(f"Warning: Could not write to GITHUB_STEP_SUMMARY: {e}")

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

    preferred_model = os.environ.get("LLM_MODEL") or os.environ.get("GEMINI_MODEL")
    pr_number = args.pr or os.environ.get("PR_NUMBER", "").strip()
    repo = get_repo_name()
    base_ref = args.base or os.environ.get("BASE_REF", "master").strip()

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
            review_body = "### 🤖 AI Code Review\n\nNo code changes requiring review were detected in this pull request."
            post_or_update_review_comment(pr_number, repo, review_body)
        return 0

    print(f"Extracted diff ({len(diff)} characters). Loading review guidelines...")
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
    review_output = call_llm_api(api_key, prompt, preferred_model=preferred_model)

    formatted_review = f"""### 🤖 AI Code Review

{review_output.strip()}

---
*Reviewed by AI using {source_description}.*
"""

    if args.dry_run:
        print("\n" + "=" * 80)
        print("DRY RUN: Generated AI Code Review (Not posted to GitHub)")
        print("=" * 80)
        print(formatted_review)
        print("=" * 80)
        return 0

    if not pr_number or not repo:
        print("Error: PR_NUMBER and GITHUB_REPOSITORY (or remote.origin.url) are required to post comments.")
        print("Use --dry-run to test locally without posting comments.")
        return 1

    post_or_update_review_comment(pr_number, repo, formatted_review)
    return 0


if __name__ == "__main__":
    sys.exit(main())
