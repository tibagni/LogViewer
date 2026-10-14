#!/usr/bin/env python3
"""
Automated AI Code Review using Google Gemini API for GitHub Pull Requests.
Zero third-party dependencies - standard library only.
"""

import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
from typing import Optional, Tuple

COMMENT_MARKER = "<!-- gemini-code-review -->"
DEFAULT_MODEL = os.environ.get("GEMINI_MODEL", "gemini-2.5-flash")
FALLBACK_MODEL = "gemini-1.5-flash"
EXCLUDED_EXTENSIONS = (
    ".png", ".jpg", ".jpeg", ".gif", ".ico", ".svg",
    ".jar", ".zip", ".tar", ".gz",
    ".class", ".bin"
)


def get_git_diff(base_ref: str) -> str:
    """Extracts git diff against origin/<base_ref>, filtering out binary/asset files."""
    try:
        # Check if remote base ref exists
        subprocess.run(
            ["git", "rev-parse", "--verify", f"origin/{base_ref}"],
            check=True,
            capture_output=True,
            text=True
        )
        target_ref = f"origin/{base_ref}"
    except subprocess.CalledProcessError:
        target_ref = base_ref

    # Get list of changed files
    result = subprocess.run(
        ["git", "diff", "--name-only", f"{target_ref}...HEAD"],
        capture_output=True,
        text=True,
        check=True
    )
    changed_files = [
        f for f in result.stdout.strip().splitlines()
        if f and not f.endswith(EXCLUDED_EXTENSIONS)
    ]

    if not changed_files:
        return ""

    # Get diff for filtered files
    diff_cmd = ["git", "diff", f"{target_ref}...HEAD", "--"] + changed_files
    diff_res = subprocess.run(diff_cmd, capture_output=True, text=True, check=True)
    return diff_res.stdout


def load_review_guidelines(guidelines_path: str = ".github/review_guidelines.md") -> str:
    """Loads the review instructions from a markdown file if present."""
    if os.path.exists(guidelines_path):
        with open(guidelines_path, "r", encoding="utf-8") as f:
            return f.read()
    return "Perform a thorough, expert-level code review focusing on concurrency, architecture, and edge cases."


def build_prompt(guidelines: str, diff: str) -> str:
    """Assembles the system guidelines and PR diff into the Gemini review prompt."""
    return f"""{guidelines}

---
## Pull Request Code Diff to Review:

```diff
{diff}
```
"""


def call_gemini_api(api_key: str, prompt: str, model: str = DEFAULT_MODEL) -> str:
    """Calls the Google Gemini REST API with the prompt and returns the text response."""
    models_to_try = [model]
    if model != FALLBACK_MODEL:
        models_to_try.append(FALLBACK_MODEL)

    last_error = None
    for target_model in models_to_try:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{target_model}:generateContent?key={api_key}"
        headers = {"Content-Type": "application/json"}
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
                    raise ValueError(f"No candidates returned by Gemini: {body}")
                text = candidates[0].get("content", {}).get("parts", [{}])[0].get("text", "")
                if not text:
                    raise ValueError(f"Empty text in candidate response: {body}")
                return text
        except urllib.error.HTTPError as e:
            err_msg = e.read().decode("utf-8", errors="replace")
            last_error = f"HTTP {e.code} for model {target_model}: {err_msg}"
            # If 404 (model not found), try the fallback model
            if e.code == 404 and target_model != models_to_try[-1]:
                print(f"Model '{target_model}' returned 404, attempting fallback to '{FALLBACK_MODEL}'...")
                continue
            raise RuntimeError(last_error) from e
        except Exception as e:
            raise RuntimeError(f"Error calling Gemini API: {e}") from e

    raise RuntimeError(f"All Gemini models failed. Last error: {last_error}")


def find_existing_comment(pr_number: str, repo: str) -> Optional[int]:
    """Finds an existing Gemini review comment on the PR by searching for COMMENT_MARKER."""
    cmd = ["gh", "api", f"repos/{repo}/issues/{pr_number}/comments", "--paginate"]
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, check=True)
        comments = json.loads(res.stdout)
        for comment in comments:
            if COMMENT_MARKER in comment.get("body", ""):
                return comment.get("id")
    except Exception as e:
        print(f"Warning: Could not search existing PR comments: {e}")
    return None


def post_or_update_review_comment(pr_number: str, repo: str, review_text: str) -> None:
    """Posts or updates the PR review comment, and outputs to GITHUB_STEP_SUMMARY if available."""
    full_comment = f"{COMMENT_MARKER}\n{review_text}"

    # Also log to GitHub Step Summary
    summary_file = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_file:
        try:
            with open(summary_file, "a", encoding="utf-8") as f:
                f.write(f"\n{review_text}\n")
        except Exception as e:
            print(f"Warning: Could not write to GITHUB_STEP_SUMMARY: {e}")

    existing_id = find_existing_comment(pr_number, repo)
    if existing_id:
        print(f"Updating existing Gemini review comment #{existing_id} on PR #{pr_number}...")
        cmd = [
            "gh", "api",
            "--method", "PATCH",
            f"repos/{repo}/issues/comments/{existing_id}",
            "-f", f"body={full_comment}"
        ]
        subprocess.run(cmd, check=True)
        print("Review comment updated successfully.")
    else:
        print(f"Posting new Gemini review comment on PR #{pr_number}...")
        cmd = [
            "gh", "api",
            "--method", "POST",
            f"repos/{repo}/issues/{pr_number}/comments",
            "-f", f"body={full_comment}"
        ]
        subprocess.run(cmd, check=True)
        print("Review comment posted successfully.")


def main() -> int:
    api_key = os.environ.get("GEMINI_API_KEY", "").strip()
    if not api_key:
        print("⚠️ Notice: GEMINI_API_KEY is not set. Skipping Gemini automated code review.")
        return 0

    pr_number = os.environ.get("PR_NUMBER", "").strip()
    repo = os.environ.get("GITHUB_REPOSITORY", "").strip()
    base_ref = os.environ.get("BASE_REF", "master").strip()

    if not pr_number or not repo:
        print("Error: PR_NUMBER and GITHUB_REPOSITORY environment variables are required.")
        return 1

    print(f"Fetching diff against base branch '{base_ref}' for {repo} PR #{pr_number}...")
    diff = get_git_diff(base_ref)
    if not diff.strip():
        print("No reviewable code changes found in this PR.")
        review_body = "### 🤖 Gemini Code Review\n\nNo code changes requiring review were detected in this pull request."
        post_or_update_review_comment(pr_number, repo, review_body)
        return 0

    print(f"Extracted diff ({len(diff)} characters). Loading review guidelines...")
    guidelines = load_review_guidelines()
    prompt = build_prompt(guidelines, diff)

    print("Requesting code review from Gemini API...")
    review_output = call_gemini_api(api_key, prompt)

    formatted_review = f"""### 🤖 Gemini Code Review

{review_output.strip()}

---
*Reviewed by Google Gemini using guidelines defined in [`.github/review_guidelines.md`](.github/review_guidelines.md).*
"""

    post_or_update_review_comment(pr_number, repo, formatted_review)
    return 0


if __name__ == "__main__":
    sys.exit(main())
