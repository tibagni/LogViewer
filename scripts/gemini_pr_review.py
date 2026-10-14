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
from typing import List, Optional, Tuple

COMMENT_MARKER = "<!-- gemini-code-review -->"
USER_PREFERRED_MODEL = os.environ.get("GEMINI_MODEL")
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
        if f and not f.lower().endswith(EXCLUDED_EXTENSIONS)
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
                # First add preferred models in order if available
                for pref in PREFERRED_MODELS_ORDER:
                    if pref in models and pref not in ordered_candidates:
                        ordered_candidates.append(pref)

                # Then add any other standard flash models (avoiding preview/tts/image/omni)
                remaining_flash = [
                    m for m in models
                    if "flash" in m and not any(x in m for x in ["preview", "tts", "image", "omni", "lite"])
                    and m not in ordered_candidates
                ]
                ordered_candidates.extend(sorted(remaining_flash, reverse=True))

                # Then any remaining general models
                for m in models:
                    if m not in ordered_candidates and not any(x in m for x in ["tts", "image"]):
                        ordered_candidates.append(m)

                if ordered_candidates:
                    return version, ordered_candidates
        except Exception as e:
            print(f"Notice: Could not list models on {version}: {e}")

    return "v1beta", PREFERRED_MODELS_ORDER


def call_gemini_api(api_key: str, prompt: str, preferred_model: Optional[str] = USER_PREFERRED_MODEL) -> str:
    """Calls the Google Gemini REST API with fallback across available models."""
    version, models = get_available_models(api_key)
    if preferred_model:
        models = [preferred_model] + [m for m in models if m != preferred_model]

    last_error = None
    for model in models:
        print(f"Attempting review with Gemini model '{model}' ({version})...")
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
                    raise ValueError(f"No candidates returned by Gemini: {body}")
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

    raise RuntimeError(f"All available Gemini models failed. Last error: {last_error}")


def find_existing_comment(pr_number: str, repo: str) -> Optional[int]:
    """Finds an existing Gemini review comment on the PR by searching for COMMENT_MARKER."""
    cmd = ["gh", "api", f"repos/{repo}/issues/{pr_number}/comments", "--paginate", "--slurp"]
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, check=True)
        raw_output = res.stdout.strip()
        if not raw_output:
            return None
        pages = json.loads(raw_output)
        # --slurp wraps responses in a list of page arrays: [[comment, ...], [comment, ...]]
        # or a single flat array if only 1 page was returned
        if pages and isinstance(pages[0], list):
            comments = [c for page in pages for c in page]
        else:
            comments = pages

        for comment in comments:
            if isinstance(comment, dict) and COMMENT_MARKER in comment.get("body", ""):
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
    payload = json.dumps({"body": full_comment}).encode("utf-8")
    if existing_id:
        print(f"Updating existing Gemini review comment #{existing_id} on PR #{pr_number}...")
        cmd = [
            "gh", "api",
            "--method", "PATCH",
            f"repos/{repo}/issues/comments/{existing_id}",
            "--input", "-"
        ]
        subprocess.run(cmd, input=payload, check=True)
        print("Review comment updated successfully.")
    else:
        print(f"Posting new Gemini review comment on PR #{pr_number}...")
        cmd = [
            "gh", "api",
            "--method", "POST",
            f"repos/{repo}/issues/{pr_number}/comments",
            "--input", "-"
        ]
        subprocess.run(cmd, input=payload, check=True)
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
