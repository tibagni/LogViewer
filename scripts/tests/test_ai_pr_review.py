#!/usr/bin/env python3
"""
Unit tests for scripts/ai_pr_review.py
"""

import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import MagicMock, patch

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

import scripts.ai_pr_review as ai_review


class TestAiPrReview(unittest.TestCase):

    def test_load_env_file(self):
        with tempfile.NamedTemporaryFile("w+", delete=False) as f:
            f.write("# comment\nTEST_LLM_KEY=secret_123\nANOTHER_KEY=\"val_456\"\n")
            temp_path = f.name

        try:
            self.assertNotIn("TEST_LLM_KEY", os.environ)
            ai_review.load_env_file(temp_path)
            self.assertEqual(os.environ.get("TEST_LLM_KEY"), "secret_123")
            self.assertEqual(os.environ.get("ANOTHER_KEY"), "val_456")
        finally:
            os.environ.pop("TEST_LLM_KEY", None)
            os.environ.pop("ANOTHER_KEY", None)
            os.remove(temp_path)

    def test_load_review_guidelines_exists(self):
        with tempfile.NamedTemporaryFile("w+", delete=False) as f:
            f.write("# Custom Guidelines\nCheck EDT rules.")
            temp_path = f.name

        try:
            content, source = ai_review.load_review_guidelines(temp_path)
            self.assertIn("Check EDT rules.", content)
            self.assertIn(temp_path, source)
        finally:
            os.remove(temp_path)

    def test_load_review_guidelines_missing_fallback(self):
        content, source = ai_review.load_review_guidelines("/non/existent/path/file.md")
        self.assertIn("Perform a thorough, expert-level code review", content)
        self.assertEqual(source, "default built-in guidelines")

    def test_build_prompt_basic(self):
        prompt = ai_review.build_prompt(
            guidelines="Rules here",
            diff="+ val x = 1"
        )
        self.assertIn("Rules here", prompt)
        self.assertIn("+ val x = 1", prompt)
        self.assertNotIn("## Pull Request Context:", prompt)
        self.assertIn("## Pull Request Code Diff to Review:", prompt)
        self.assertIn("Optional Inline Comments:", prompt)

    def test_build_prompt_with_full_context(self):
        prompt = ai_review.build_prompt(
            guidelines="Rules here",
            diff="+ val x = 1",
            title="Fix concurrency bug",
            description="Solves race condition on EDT",
            commits=["`abc1234`: fix race condition"],
            comments=[{"author": "tibagni", "body": "Verified on Linux"}]
        )
        self.assertIn("## Pull Request Context:", prompt)
        self.assertIn("- **Title**: Fix concurrency bug", prompt)
        self.assertIn("- **Description**:\nSolves race condition on EDT", prompt)
        self.assertIn("### Commits:\n- `abc1234`: fix race condition", prompt)
        self.assertIn("### Existing Discussion:\n- @tibagni: Verified on Linux", prompt)
        self.assertIn("## Pull Request Code Diff to Review:", prompt)

    def test_parse_valid_diff_lines(self):
        diff = """--- a/src/FileA.kt
+++ b/src/FileA.kt
@@ -10,3 +10,4 @@
 val a = 1
-val b = 2
+val b = 20
+val c = 30
 val d = 4
--- a/src/FileB.kt
+++ b/src/FileB.kt
@@ -1,2 +1,2 @@
-val old = 0
+val new = 1
"""
        valid = ai_review.parse_valid_diff_lines(diff)
        self.assertIn("src/FileA.kt", valid)
        self.assertIn("src/FileB.kt", valid)
        # FileA lines: 10 (context), 11 (mod), 12 (add), 13 (context)
        self.assertEqual(valid["src/FileA.kt"], {10, 11, 12, 13})
        # FileB lines: 1 (mod)
        self.assertEqual(valid["src/FileB.kt"], {1})

    def test_normalize_path(self):
        self.assertEqual(ai_review.normalize_path("b/src/Main.kt"), "src/Main.kt")
        self.assertEqual(ai_review.normalize_path("./src/Main.kt"), "src/Main.kt")
        self.assertEqual(ai_review.normalize_path("/src/Main.kt"), "src/Main.kt")
        self.assertEqual(ai_review.normalize_path("src/Main.kt"), "src/Main.kt")

    def test_extract_inline_comments(self):
        text = """### 🤖 Review
Looks great!

```inline-comments
[
  {"path": "b/src/Main.kt", "line": 42, "body": "Prefer val over var"}
]
```
"""
        clean_text, comments = ai_review.extract_inline_comments(text)
        self.assertEqual(clean_text, "### 🤖 Review\nLooks great!")
        self.assertEqual(len(comments), 1)
        self.assertEqual(comments[0]["path"], "src/Main.kt")
        self.assertEqual(comments[0]["line"], 42)
        self.assertEqual(comments[0]["body"], "Prefer val over var")

    def test_extract_inline_comments_none(self):
        text = "### 🤖 Review\nNo inline comments here."
        clean_text, comments = ai_review.extract_inline_comments(text)
        self.assertEqual(clean_text, text)
        self.assertEqual(comments, [])

    def test_filter_inline_comments(self):
        valid_map = {"src/Main.kt": {10, 11, 12}}
        raw_comments = [
            {"path": "src/Main.kt", "line": 11, "body": "Valid line"},
            {"path": "src/Main.kt", "line": 99, "body": "Outside hunk"},
            {"path": "src/Other.kt", "line": 5, "body": "Untouched file"}
        ]
        valid, discarded = ai_review.filter_inline_comments(raw_comments, valid_map)
        self.assertEqual(len(valid), 1)
        self.assertEqual(valid[0]["line"], 11)
        self.assertEqual(len(discarded), 2)

    def test_parse_args(self):
        args = ai_review.parse_args(["--dry-run", "-v", "--pr", "101", "--base", "master"])
        self.assertTrue(args.dry_run)
        self.assertTrue(args.verbose)
        self.assertEqual(args.pr, "101")
        self.assertEqual(args.base, "master")

    @patch("subprocess.run")
    def test_get_pr_diff(self, mock_run):
        mock_files = MagicMock(stdout="file1.kt\nimage.png\nfile2.java\n")
        mock_diff = MagicMock(stdout="diff content")
        mock_run.side_effect = [mock_files, mock_diff]

        diff = ai_review.get_pr_diff("101")
        self.assertEqual(diff, "diff content")
        self.assertEqual(mock_run.call_count, 2)

    @patch("subprocess.run")
    def test_get_pr_metadata_filters_bot_comments(self, mock_run):
        raw_json = json.dumps({
            "title": "My PR Title",
            "body": "PR description text",
            "headRefOid": "abc12345",
            "commits": [{"oid": "1234567890", "messageHeadline": "Commit headline"}],
            "comments": [
                {"author": {"login": "human_reviewer"}, "body": "Looks good so far!"},
                {"author": {"login": "bot"}, "body": "<!-- ai-code-review -->\nOld review"},
                {"author": {"login": "bot"}, "body": "<!-- gemini-code-review -->\nOlder review"},
                {"author": {"login": "tibagni"}, "body": "Fixed in latest commit"}
            ]
        })
        mock_run.return_value = MagicMock(stdout=raw_json)

        meta = ai_review.get_pr_metadata("103")
        self.assertEqual(meta["title"], "My PR Title")
        self.assertEqual(meta["headRefOid"], "abc12345")
        filtered_comments = meta["filtered_comments"]
        self.assertEqual(len(filtered_comments), 2)
        self.assertEqual(filtered_comments[0]["author"], "human_reviewer")
        self.assertEqual(filtered_comments[0]["body"], "Looks good so far!")
        self.assertEqual(filtered_comments[1]["author"], "tibagni")
        self.assertEqual(filtered_comments[1]["body"], "Fixed in latest commit")

    @patch("urllib.request.urlopen")
    def test_get_available_models_success(self, mock_urlopen):
        mock_response = MagicMock()
        mock_response.read.return_value = json.dumps({
            "models": [
                {"name": "models/gemini-flash-latest", "supportedGenerationMethods": ["generateContent"]},
                {"name": "models/gemini-pro-latest", "supportedGenerationMethods": ["generateContent"]}
            ]
        }).encode("utf-8")
        mock_urlopen.return_value.__enter__.return_value = mock_response

        version, models = ai_review.get_available_models("dummy_key")
        self.assertEqual(version, "v1beta")
        self.assertIn("gemini-flash-latest", models)

    @patch("urllib.request.urlopen")
    def test_get_available_models_prioritizes_flash_and_lite_over_gemma(self, mock_urlopen):
        # Raw API response with Gemma before Flash-Lite
        mock_response = MagicMock()
        mock_response.read.return_value = json.dumps({
            "models": [
                {"name": "models/gemma-4-26b-a4b-it", "supportedGenerationMethods": ["generateContent"]},
                {"name": "models/gemini-flash-lite-latest", "supportedGenerationMethods": ["generateContent"]},
                {"name": "models/gemini-3.8-flash", "supportedGenerationMethods": ["generateContent"]},
                {"name": "models/gemini-pro-latest", "supportedGenerationMethods": ["generateContent"]},
            ]
        }).encode("utf-8")
        mock_urlopen.return_value.__enter__.return_value = mock_response

        _, models = ai_review.get_available_models("dummy_key")
        # Flash should be first, then Flash-Lite, then Pro, with Gemma last
        self.assertEqual(
            models,
            ["gemini-3.8-flash", "gemini-flash-lite-latest", "gemini-pro-latest", "gemma-4-26b-a4b-it"]
        )

    @patch("urllib.request.urlopen")
    def test_call_llm_api_success(self, mock_urlopen):
        mock_response = MagicMock()
        mock_response.read.return_value = json.dumps({
            "candidates": [
                {
                    "content": {
                        "parts": [{"text": "Review feedback: LGTM!"}]
                    }
                }
            ]
        }).encode("utf-8")
        mock_urlopen.return_value.__enter__.return_value = mock_response

        result_text, model_used = ai_review.call_llm_api("dummy_key", "prompt here", preferred_model="gemini-3.8-flash")
        self.assertEqual(result_text, "Review feedback: LGTM!")
        self.assertEqual(model_used, "gemini-3.8-flash")

    @patch("scripts.ai_pr_review.get_available_models", return_value=("v1beta", ["gemini-3.8-flash"]))
    @patch("time.sleep")
    @patch("urllib.request.urlopen")
    def test_call_llm_api_retries_on_503(self, mock_urlopen, mock_sleep, mock_models):
        import urllib.error
        import io

        error_503 = urllib.error.HTTPError(
            url="http://example.com",
            code=503,
            msg="Service Unavailable",
            hdrs={},
            fp=io.BytesIO(b'{"error": {"message": "High demand"}}')
        )

        mock_success = MagicMock()
        mock_success.read.return_value = json.dumps({
            "candidates": [
                {
                    "content": {
                        "parts": [{"text": "Success after retry!"}]
                    }
                }
            ]
        }).encode("utf-8")

        mock_success_cm = MagicMock()
        mock_success_cm.__enter__.return_value = mock_success

        # First call fails with 503, second call succeeds
        mock_urlopen.side_effect = [error_503, mock_success_cm]

        text, model = ai_review.call_llm_api(
            "dummy_key", "prompt here", preferred_model="gemini-3.8-flash", max_retries=1
        )
        self.assertEqual(text, "Success after retry!")
        self.assertEqual(model, "gemini-3.8-flash")
        mock_sleep.assert_called_once()

    @patch("scripts.ai_pr_review.get_available_models", return_value=("v1beta", ["gemini-3.8-flash", "gemini-3.5-flash"]))
    @patch("urllib.request.urlopen")
    def test_call_llm_api_falls_back_on_429(self, mock_urlopen, mock_models):
        import urllib.error
        import io

        error_429 = urllib.error.HTTPError(
            url="http://example.com",
            code=429,
            msg="Too Many Requests",
            hdrs={},
            fp=io.BytesIO(b'{"error": {"message": "Resource exhausted: quota exceeded"}}')
        )

        mock_success = MagicMock()
        mock_success.read.return_value = json.dumps({
            "candidates": [
                {
                    "content": {
                        "parts": [{"text": "Success on second model!"}]
                    }
                }
            ]
        }).encode("utf-8")

        mock_success_cm = MagicMock()
        mock_success_cm.__enter__.return_value = mock_success

        # First model fails with 429, second model succeeds
        mock_urlopen.side_effect = [error_429, mock_success_cm]

        text, model = ai_review.call_llm_api(
            "dummy_key", "prompt here", preferred_model="gemini-3.8-flash"
        )
        self.assertEqual(text, "Success on second model!")
        self.assertEqual(model, "gemini-3.5-flash")

    @patch("subprocess.run")
    def test_find_existing_comment_found_primary_marker(self, mock_run):
        comments_json = json.dumps([
            [{"id": 111, "body": "regular comment"}, {"id": 222, "body": "<!-- ai-code-review -->\nLGTM"}]
        ])
        mock_run.return_value = MagicMock(stdout=comments_json)

        comment_id = ai_review.find_existing_comment("12", "tibagni/LogViewer")
        self.assertEqual(comment_id, 222)

    @patch("subprocess.run")
    def test_find_existing_comment_found_legacy_marker(self, mock_run):
        comments_json = json.dumps([
            [{"id": 111, "body": "regular comment"}, {"id": 333, "body": "<!-- gemini-code-review -->\nLGTM"}]
        ])
        mock_run.return_value = MagicMock(stdout=comments_json)

        comment_id = ai_review.find_existing_comment("12", "tibagni/LogViewer")
        self.assertEqual(comment_id, 333)

    @patch("subprocess.run")
    def test_find_existing_comment_not_found(self, mock_run):
        comments_json = json.dumps([
            [{"id": 111, "body": "regular comment"}]
        ])
        mock_run.return_value = MagicMock(stdout=comments_json)

        comment_id = ai_review.find_existing_comment("12", "tibagni/LogViewer")
        self.assertIsNone(comment_id)

    @patch("subprocess.run")
    @patch("scripts.ai_pr_review.find_existing_comment")
    def test_post_or_update_review_comment_post(self, mock_find, mock_run):
        mock_find.return_value = None
        mock_run.return_value = MagicMock()

        ai_review.post_or_update_review_comment("12", "tibagni/LogViewer", "New review")
        mock_run.assert_called_once()
        cmd = mock_run.call_args[0][0]
        self.assertIn("POST", cmd)
        self.assertIn("repos/tibagni/LogViewer/issues/12/comments", cmd)
        self.assertIn("--input", cmd)

    @patch("subprocess.run")
    @patch("scripts.ai_pr_review.find_existing_comment")
    def test_post_or_update_review_comment_patch(self, mock_find, mock_run):
        mock_find.return_value = 555
        mock_run.return_value = MagicMock()

        ai_review.post_or_update_review_comment("12", "tibagni/LogViewer", "Updated review")
        mock_run.assert_called_once()
        cmd = mock_run.call_args[0][0]
        self.assertIn("PATCH", cmd)
        self.assertIn("repos/tibagni/LogViewer/issues/comments/555", cmd)
        self.assertIn("--input", cmd)

    @patch("subprocess.run")
    def test_post_inline_review_comments(self, mock_run):
        mock_run.return_value = MagicMock()
        comments = [{"path": "src/Foo.kt", "line": 42, "body": "Fix this"}]
        success = ai_review.post_inline_review_comments(
            "12", "tibagni/LogViewer", "commit_sha_123", comments
        )
        self.assertTrue(success)
        mock_run.assert_called_once()
        cmd = mock_run.call_args[0][0]
        self.assertIn("repos/tibagni/LogViewer/pulls/12/reviews", cmd)

    @patch("subprocess.run")
    def test_post_pull_request_review_approve(self, mock_run):
        mock_run.return_value = MagicMock()
        success = ai_review.post_pull_request_review(
            pr_number="12",
            repo="tibagni/LogViewer",
            commit_sha="commit_sha_123",
            body="Approved by AI",
            event="APPROVE"
        )
        self.assertTrue(success)
        mock_run.assert_called_once()
        call_kwargs = mock_run.call_args[1]
        self.assertIsInstance(call_kwargs.get("input"), str)
        payload = json.loads(call_kwargs["input"])
        self.assertEqual(payload["event"], "APPROVE")
        self.assertEqual(payload["body"], "Approved by AI")

    @patch("subprocess.run")
    def test_post_pull_request_review_handles_error(self, mock_run):
        mock_run.side_effect = subprocess.CalledProcessError(1, ["gh"], stderr="Forbidden")
        success = ai_review.post_pull_request_review(
            pr_number="12",
            repo="tibagni/LogViewer",
            commit_sha="commit_sha_123",
            body="Approved by AI",
            event="APPROVE"
        )
        self.assertFalse(success)

    @patch.dict(os.environ, {}, clear=True)
    def test_main_no_api_key(self):
        exit_code = ai_review.main(["--env-file", "/non/existent/.env"])
        self.assertEqual(exit_code, 0)

    @patch("scripts.ai_pr_review.call_llm_api", return_value=("Review output", "gemini-flash-latest"))
    @patch("scripts.ai_pr_review.get_pr_diff", return_value="+ some code diff")
    @patch("scripts.ai_pr_review.get_pr_metadata", return_value={"title": "T", "body": "B", "commits": [], "filtered_comments": []})
    @patch("scripts.ai_pr_review.post_or_update_review_comment")
    def test_main_dry_run_does_not_post(self, mock_post, mock_meta, mock_diff, mock_call):
        with patch.dict(os.environ, {"LLM_API_KEY": "test_key"}):
            exit_code = ai_review.main(["--dry-run", "-v", "--pr", "101", "--env-file", "/non/existent/.env"])
            self.assertEqual(exit_code, 0)
            mock_post.assert_not_called()

    @patch("scripts.ai_pr_review.post_inline_review_comments")
    @patch("scripts.ai_pr_review.post_or_update_review_comment")
    @patch("scripts.ai_pr_review.get_head_sha", return_value="sha123")
    @patch("scripts.ai_pr_review.get_repo_name", return_value="tibagni/LogViewer")
    @patch("scripts.ai_pr_review.get_pr_metadata", return_value={"title": "T", "body": "B", "commits": [], "filtered_comments": []})
    @patch("scripts.ai_pr_review.get_pr_diff", return_value="--- a/src/F.kt\n+++ b/src/F.kt\n@@ -1,1 +1,2 @@\n+val x = 1\n")
    @patch("scripts.ai_pr_review.call_llm_api", return_value=("LGTM\n```inline-comments\n[{\"path\": \"src/F.kt\", \"line\": 1, \"body\": \"Nice\"}]\n```", "gemini-flash-latest"))
    def test_main_posts_both_top_level_and_inline_comments(
        self, mock_call, mock_diff, mock_meta, mock_repo, mock_sha, mock_post_top, mock_post_inline
    ):
        with patch.dict(os.environ, {"LLM_API_KEY": "test_key"}):
            exit_code = ai_review.main(["--pr", "101", "--env-file", "/non/existent/.env"])
            self.assertEqual(exit_code, 0)
            mock_post_top.assert_called_once()
            mock_post_inline.assert_called_once()

    @patch("scripts.ai_pr_review.post_or_update_review_comment")
    @patch("scripts.ai_pr_review.get_head_sha", return_value="sha123")
    @patch("scripts.ai_pr_review.get_repo_name", return_value="tibagni/LogViewer")
    @patch("scripts.ai_pr_review.get_pr_metadata", return_value={"title": "T", "body": "B", "commits": [], "filtered_comments": []})
    @patch("scripts.ai_pr_review.get_pr_diff", return_value="--- a/src/F.kt\n+++ b/src/F.kt\n@@ -1,1 +1,2 @@\n+val x = 1\n")
    @patch("scripts.ai_pr_review.call_llm_api", return_value=("LGTM", "gemini-flash-lite-latest"))
    def test_main_formats_model_attribution_in_review_footer(
        self, mock_call, mock_diff, mock_meta, mock_repo, mock_sha, mock_post_top
    ):
        with patch.dict(os.environ, {"LLM_API_KEY": "test_key"}):
            exit_code = ai_review.main(["--pr", "101", "--env-file", "/non/existent/.env"])
            self.assertEqual(exit_code, 0)
            mock_post_top.assert_called_once()
            comment_posted = mock_post_top.call_args[0][2]
            self.assertIn("*Reviewed by AI (`gemini-flash-lite-latest`)", comment_posted)

    @patch("scripts.ai_pr_review.post_or_update_review_comment")
    @patch("scripts.ai_pr_review.get_git_diff", return_value="--- a/src/F.kt\n+++ b/src/F.kt\n@@ -1,1 +1,2 @@\n+val x = 1\n")
    @patch("scripts.ai_pr_review.get_local_commit_log", return_value=["c1"])
    @patch("scripts.ai_pr_review.call_llm_api", return_value=("LGTM", "gemini-flash-latest"))
    @patch("scripts.ai_pr_review.print_dry_run_results")
    def test_main_local_run_without_pr_falls_back_to_dry_run(
        self, mock_print_dry_run, mock_call, mock_log, mock_diff, mock_post
    ):
        with patch.dict(os.environ, {"LLM_API_KEY": "test_key"}, clear=True):
            exit_code = ai_review.main(["--env-file", "/non/existent/.env"])
            self.assertEqual(exit_code, 0)
            mock_print_dry_run.assert_called_once()
            mock_post.assert_not_called()

    def test_parse_auto_approve_authors(self):
        self.assertEqual(
            ai_review.parse_auto_approve_authors("tibagni, alice, bob"),
            {"tibagni", "alice", "bob"}
        )
        self.assertEqual(
            ai_review.parse_auto_approve_authors("TiBagni   Alice\n Bob"),
            {"tibagni", "alice", "bob"}
        )
        self.assertEqual(ai_review.parse_auto_approve_authors(""), set())
        self.assertEqual(ai_review.parse_auto_approve_authors(None), set())

    def test_is_verdict_lgtm(self):
        lgtm_text_1 = "### Overall Verdict\n\n✅ LGTM (Ready to merge)\n\n### Summary"
        self.assertTrue(ai_review.is_verdict_lgtm(lgtm_text_1))

        lgtm_text_2 = "### 1. Overall Verdict\n`✅ LGTM`\n\n### 2. Summary"
        self.assertTrue(ai_review.is_verdict_lgtm(lgtm_text_2))

        suggestions_text = "### Overall Verdict\n⚠️ LGTM with Suggestions (Non-blocking improvements; ready to merge)\n\n### Summary"
        self.assertTrue(ai_review.is_verdict_lgtm(suggestions_text))

        needs_work_text = "### Overall Verdict\n❌ Needs Work (Critical issues: threading bugs)\n\n### Summary"
        self.assertFalse(ai_review.is_verdict_lgtm(needs_work_text))

        random_text = "Some generic review notes without explicit verdict."
        self.assertFalse(ai_review.is_verdict_lgtm(random_text))

    @patch("scripts.ai_pr_review.post_pull_request_review")
    @patch("scripts.ai_pr_review.post_or_update_review_comment")
    @patch("scripts.ai_pr_review.get_head_sha", return_value="sha123")
    @patch("scripts.ai_pr_review.get_repo_name", return_value="tibagni/LogViewer")
    @patch("scripts.ai_pr_review.get_pr_metadata", return_value={"title": "T", "body": "B", "commits": [], "filtered_comments": [], "author": {"login": "tibagni"}})
    @patch("scripts.ai_pr_review.get_pr_diff", return_value="--- a/src/F.kt\n+++ b/src/F.kt\n@@ -1,1 +1,2 @@\n+val x = 1\n")
    @patch("scripts.ai_pr_review.call_llm_api", return_value=("### Overall Verdict\n✅ LGTM\n\n### Summary\nGood work", "gemini-3.6-flash"))
    def test_main_submits_approval_when_author_matches_and_lgtm(
        self, mock_call, mock_diff, mock_meta, mock_repo, mock_sha, mock_post_comment, mock_post_review
    ):
        with patch.dict(os.environ, {"LLM_API_KEY": "test_key", "AUTO_APPROVE_AUTHORS": "tibagni", "PR_AUTHOR": ""}):
            exit_code = ai_review.main(["--pr", "101", "--env-file", "/non/existent/.env"])
            self.assertEqual(exit_code, 0)
            mock_post_comment.assert_called_once()
            mock_post_review.assert_called_once()
            call_kwargs = mock_post_review.call_args[1]
            self.assertEqual(call_kwargs.get("event"), "APPROVE")
            self.assertEqual(call_kwargs.get("pr_number"), "101")

    @patch("scripts.ai_pr_review.post_pull_request_review")
    @patch("scripts.ai_pr_review.post_or_update_review_comment")
    @patch("scripts.ai_pr_review.get_head_sha", return_value="sha123")
    @patch("scripts.ai_pr_review.get_repo_name", return_value="tibagni/LogViewer")
    @patch("scripts.ai_pr_review.get_pr_metadata", return_value={"title": "T", "body": "B", "commits": [], "filtered_comments": [], "author": {"login": "tibagni"}})
    @patch("scripts.ai_pr_review.get_pr_diff", return_value="--- a/src/F.kt\n+++ b/src/F.kt\n@@ -1,1 +1,2 @@\n+val x = 1\n")
    @patch("scripts.ai_pr_review.call_llm_api", return_value=("### Overall Verdict\n❌ Needs Work\n\n### Key Findings\nBad code", "gemini-3.6-flash"))
    def test_main_does_not_approve_when_verdict_needs_work(
        self, mock_call, mock_diff, mock_meta, mock_repo, mock_sha, mock_post_comment, mock_post_review
    ):
        with patch.dict(os.environ, {"LLM_API_KEY": "test_key", "AUTO_APPROVE_AUTHORS": "tibagni", "PR_AUTHOR": ""}):
            exit_code = ai_review.main(["--pr", "101", "--env-file", "/non/existent/.env"])
            self.assertEqual(exit_code, 0)
            mock_post_comment.assert_called_once()
            mock_post_review.assert_not_called()

    @patch("scripts.ai_pr_review.post_pull_request_review")
    @patch("scripts.ai_pr_review.post_or_update_review_comment")
    @patch("scripts.ai_pr_review.get_head_sha", return_value="sha123")
    @patch("scripts.ai_pr_review.get_repo_name", return_value="tibagni/LogViewer")
    @patch("scripts.ai_pr_review.get_pr_metadata", return_value={"title": "T", "body": "B", "commits": [], "filtered_comments": [], "author": {"login": "other_dev"}})
    @patch("scripts.ai_pr_review.get_pr_diff", return_value="--- a/src/F.kt\n+++ b/src/F.kt\n@@ -1,1 +1,2 @@\n+val x = 1\n")
    @patch("scripts.ai_pr_review.call_llm_api", return_value=("### Overall Verdict\n✅ LGTM\n\n### Summary\nGood work", "gemini-3.6-flash"))
    def test_main_does_not_approve_when_author_not_in_allowed_list(
        self, mock_call, mock_diff, mock_meta, mock_repo, mock_sha, mock_post_comment, mock_post_review
    ):
        with patch.dict(os.environ, {"LLM_API_KEY": "test_key", "AUTO_APPROVE_AUTHORS": "tibagni", "PR_AUTHOR": ""}):
            exit_code = ai_review.main(["--pr", "101", "--env-file", "/non/existent/.env"])
            self.assertEqual(exit_code, 0)
            mock_post_comment.assert_called_once()
            mock_post_review.assert_not_called()

    @patch("scripts.ai_pr_review.post_pull_request_review")
    @patch("scripts.ai_pr_review.post_or_update_review_comment")
    @patch("scripts.ai_pr_review.get_head_sha", return_value="sha123")
    @patch("scripts.ai_pr_review.get_repo_name", return_value="tibagni/LogViewer")
    @patch("scripts.ai_pr_review.get_pr_metadata", return_value={"title": "T", "body": "B", "commits": [], "filtered_comments": [], "author": {"login": "tibagni"}})
    @patch("scripts.ai_pr_review.get_pr_diff", return_value="--- a/src/F.kt\n+++ b/src/F.kt\n@@ -1,1 +1,2 @@\n+val x = 1\n")
    @patch("scripts.ai_pr_review.call_llm_api", return_value=("### Overall Verdict\n✅ LGTM\n\n### Summary\nGood work", "gemini-3.6-flash"))
    def test_main_dry_run_skips_all_network_posts(
        self, mock_call, mock_diff, mock_meta, mock_repo, mock_sha, mock_post_comment, mock_post_review
    ):
        with patch.dict(os.environ, {"LLM_API_KEY": "test_key", "AUTO_APPROVE_AUTHORS": "tibagni", "PR_AUTHOR": ""}):
            exit_code = ai_review.main(["--dry-run", "--pr", "101", "--env-file", "/non/existent/.env"])
            self.assertEqual(exit_code, 0)
            mock_post_comment.assert_not_called()
            mock_post_review.assert_not_called()

    def test_get_review_timestamp_crossover_midnight(self):
        from datetime import datetime, timezone
        # Oct 18 01:51:02 UTC is Oct 17 20:51:02 CDT (different dates)
        dt = datetime(2026, 10, 18, 1, 51, 2, tzinfo=timezone.utc)
        ts = ai_review.get_review_timestamp(dt, tz_name="America/Chicago", commit_sha="c1973c435a109cb3d27afd6e4068f432006d54ea")
        self.assertEqual(
            ts,
            "2026-10-17 20:51:02 CDT (2026-10-18 01:51:02 UTC) • commit `c1973c4`"
        )

    def test_get_review_timestamp_same_date(self):
        from datetime import datetime, timezone
        # Oct 17 20:00:00 UTC is Oct 17 15:00:00 CDT (same date)
        dt = datetime(2026, 10, 17, 20, 0, 0, tzinfo=timezone.utc)
        ts = ai_review.get_review_timestamp(dt, tz_name="America/Chicago", commit_sha="abcdef123456")
        self.assertEqual(
            ts,
            "2026-10-17 15:00:00 CDT (20:00:00 UTC) • commit `abcdef1`"
        )

    def test_get_review_timestamp_utc_only(self):
        from datetime import datetime, timezone
        dt = datetime(2026, 10, 18, 1, 51, 2, tzinfo=timezone.utc)
        ts = ai_review.get_review_timestamp(dt, tz_name="UTC", commit_sha="abcdef123456")
        self.assertEqual(
            ts,
            "2026-10-18 01:51:02 UTC • commit `abcdef1`"
        )

    def test_get_review_timestamp_invalid_tz_fallback(self):
        from datetime import datetime, timezone
        dt = datetime(2026, 10, 18, 1, 51, 2, tzinfo=timezone.utc)
        ts = ai_review.get_review_timestamp(dt, tz_name="Invalid/Timezone_Name", commit_sha="abcdef123456")
        self.assertEqual(
            ts,
            "2026-10-18 01:51:02 UTC • commit `abcdef1`"
        )

    def test_get_review_timestamp_without_commit(self):
        from datetime import datetime, timezone
        dt = datetime(2026, 10, 17, 20, 0, 0, tzinfo=timezone.utc)
        ts = ai_review.get_review_timestamp(dt, tz_name="America/Chicago")
        self.assertEqual(
            ts,
            "2026-10-17 15:00:00 CDT (20:00:00 UTC)"
        )

    def test_get_review_timestamp_env_var(self):
        from datetime import datetime, timezone
        dt = datetime(2026, 10, 18, 1, 51, 2, tzinfo=timezone.utc)
        with patch.dict(os.environ, {"REVIEW_TIMEZONE": "UTC"}):
            ts = ai_review.get_review_timestamp(dt)
            self.assertEqual(ts, "2026-10-18 01:51:02 UTC")

    def test_parse_args_timezone_flag(self):
        args = ai_review.parse_args(["--timezone", "America/New_York"])
        self.assertEqual(args.timezone, "America/New_York")

    def test_parse_args_no_two_pass_flag(self):
        args = ai_review.parse_args(["--no-two-pass"])
        self.assertTrue(args.no_two_pass)

    def test_preferred_models_include_gemini_4(self):
        self.assertIn("gemini-4-flash", ai_review.PREFERRED_MODELS_ORDER)
        self.assertIn("gemini-4-argon", ai_review.PREFERRED_MODELS_ORDER)
        self.assertIn("gemini-4-pro", ai_review.PREFERRED_MODELS_ORDER)

    def test_parse_context_requests(self):
        text = """Here is my initial review.
```context-requests
src/main/java/com/tibagni/logviewer/MainView.kt
- src/main/java/com/tibagni/logviewer/ServiceLocator.kt
`src/main/java/com/tibagni/logviewer/MainView.kt`
# This is a comment line
```
Overall Verdict: Needs Work
"""
        reqs = ai_review.parse_context_requests(text)
        self.assertEqual(
            reqs,
            [
                "src/main/java/com/tibagni/logviewer/MainView.kt",
                "src/main/java/com/tibagni/logviewer/ServiceLocator.kt"
            ]
        )

    def test_parse_context_requests_empty_or_missing(self):
        self.assertEqual(ai_review.parse_context_requests("Review without requests block"), [])
        self.assertEqual(ai_review.parse_context_requests("```context-requests\n\n```"), [])

    def test_strip_context_requests(self):
        text = """Header

```context-requests
src/FileA.kt
```

Body content."""
        cleaned = ai_review.strip_context_requests(text)
        self.assertNotIn("context-requests", cleaned)
        self.assertNotIn("src/FileA.kt", cleaned)
        self.assertIn("Header", cleaned)
        self.assertIn("Body content.", cleaned)

    def test_fetch_file_context(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            file_a = os.path.join(tmpdir, "FileA.kt")
            file_bin = os.path.join(tmpdir, "image.png")
            with open(file_a, "w") as f:
                f.write("class FileA {\n  fun test() = true\n}\n")
            with open(file_bin, "wb") as f:
                f.write(b"\x89PNG\r\n\x1a\n")

            # Normal read
            res = ai_review.fetch_file_context(tmpdir, ["FileA.kt", "nonexistent.kt", "image.png"])
            self.assertIn("FileA.kt", res)
            self.assertIn("class FileA", res["FileA.kt"])
            self.assertNotIn("nonexistent.kt", res)
            self.assertNotIn("image.png", res)

            # Path traversal protection
            traversal_res = ai_review.fetch_file_context(tmpdir, ["../../etc/passwd"])
            self.assertEqual(traversal_res, {})

            # Truncation check
            long_file = os.path.join(tmpdir, "Long.kt")
            with open(long_file, "w") as f:
                for i in range(50):
                    f.write(f"val line{i} = {i}\n")
            truncated_res = ai_review.fetch_file_context(tmpdir, ["Long.kt"], max_lines=10)
            self.assertIn("truncated", truncated_res["Long.kt"])

    def test_build_second_pass_prompt(self):
        prompt = ai_review.build_second_pass_prompt(
            guidelines="Review guidelines",
            diff="+ fun test()",
            first_pass_review="Provisional review: Needs Work",
            file_contexts={"src/FileA.kt": "class FileA"},
            title="My PR",
            description="PR description"
        )
        self.assertIn("Review guidelines", prompt)
        self.assertIn("+ fun test()", prompt)
        self.assertIn("Provisional review: Needs Work", prompt)
        self.assertIn("### File: `src/FileA.kt`", prompt)
        self.assertIn("class FileA", prompt)
        self.assertIn("Second-Pass Verification Instructions", prompt)
        self.assertIn("**Title**: My PR", prompt)

    @patch("scripts.ai_pr_review.call_llm_api")
    @patch("scripts.ai_pr_review.get_pr_diff")
    @patch("scripts.ai_pr_review.get_pr_metadata")
    @patch("scripts.ai_pr_review.fetch_file_context")
    def test_main_two_pass_flow_success(
        self,
        mock_fetch_file_context,
        mock_get_pr_meta,
        mock_get_pr_diff,
        mock_call_llm_api
    ):
        mock_get_pr_diff.return_value = "--- a/F.kt\n+++ b/F.kt\n@@ -1,1 +1,2 @@\n+val x = 1\n"
        mock_get_pr_meta.return_value = {"title": "T", "body": "B"}
        mock_fetch_file_context.return_value = {"src/Extra.kt": "class Extra"}

        pass1_output = """### Overall Verdict
⚠️ LGTM with Suggestions

```context-requests
src/Extra.kt
```
"""
        pass2_output = """### Overall Verdict
✅ LGTM

Summary: Clean after context check.
"""
        mock_call_llm_api.side_effect = [
            (pass1_output, "gemini-4-flash"),
            (pass2_output, "gemini-4-flash")
        ]

        with patch.dict(os.environ, {"LLM_API_KEY": "test-key"}):
            with patch("sys.stdout"):
                code = ai_review.main(["--dry-run", "--pr", "101"])
                self.assertEqual(code, 0)

        self.assertEqual(mock_call_llm_api.call_count, 2)
        mock_fetch_file_context.assert_called_once()

    @patch("scripts.ai_pr_review.call_llm_api")
    @patch("scripts.ai_pr_review.get_pr_diff")
    @patch("scripts.ai_pr_review.get_pr_metadata")
    @patch("scripts.ai_pr_review.fetch_file_context")
    def test_main_two_pass_fallback_on_second_pass_error(
        self,
        mock_fetch_file_context,
        mock_get_pr_meta,
        mock_get_pr_diff,
        mock_call_llm_api
    ):
        mock_get_pr_diff.return_value = "--- a/F.kt\n+++ b/F.kt\n@@ -1,1 +1,2 @@\n+val x = 1\n"
        mock_get_pr_meta.return_value = {"title": "T", "body": "B"}
        mock_fetch_file_context.return_value = {"src/Extra.kt": "class Extra"}

        pass1_output = """### Overall Verdict
✅ LGTM

```context-requests
src/Extra.kt
```
"""
        # Pass 1 succeeds, Pass 2 raises exception
        mock_call_llm_api.side_effect = [
            (pass1_output, "gemini-4-flash"),
            RuntimeError("Pass 2 API timeout")
        ]

        with patch.dict(os.environ, {"LLM_API_KEY": "test-key"}):
            with patch("sys.stdout"):
                code = ai_review.main(["--dry-run", "--pr", "101"])
                self.assertEqual(code, 0)

        # Should attempt 2 calls and gracefully finish
        self.assertEqual(mock_call_llm_api.call_count, 2)

    @patch("scripts.ai_pr_review.call_llm_api")
    @patch("scripts.ai_pr_review.get_pr_diff")
    @patch("scripts.ai_pr_review.get_pr_metadata")
    @patch("scripts.ai_pr_review.fetch_file_context")
    def test_main_two_pass_disabled_flag(
        self,
        mock_fetch_file_context,
        mock_get_pr_meta,
        mock_get_pr_diff,
        mock_call_llm_api
    ):
        mock_get_pr_diff.return_value = "--- a/F.kt\n+++ b/F.kt\n@@ -1,1 +1,2 @@\n+val x = 1\n"
        mock_get_pr_meta.return_value = {"title": "T", "body": "B"}

        pass1_output = """### Overall Verdict
✅ LGTM

```context-requests
src/Extra.kt
```
"""
        mock_call_llm_api.return_value = (pass1_output, "gemini-flash-latest")

        with patch.dict(os.environ, {"LLM_API_KEY": "test-key"}):
            with patch("sys.stdout"):
                code = ai_review.main(["--dry-run", "--pr", "101", "--no-two-pass"])
                self.assertEqual(code, 0)

        # Only 1 call because two-pass was explicitly disabled
        self.assertEqual(mock_call_llm_api.call_count, 1)
        mock_fetch_file_context.assert_not_called()

    @patch("scripts.ai_pr_review.get_pr_diff", return_value="+ pr diff")
    @patch("scripts.ai_pr_review.get_pr_metadata", return_value={
        "title": "PR Title",
        "body": "PR Body",
        "commits": [{"oid": "abcdef12345", "messageHeadline": "Commit msg"}],
        "filtered_comments": [{"user": "alice", "body": "comment"}],
        "author": {"login": "octocat"}
    })
    def test_extract_change_context_pr(self, mock_meta, mock_diff):
        ctx = ai_review.extract_change_context(
            pr_number="123",
            base_ref="master",
            head_sha="sha999",
            explicit_author=None
        )
        self.assertEqual(ctx.diff, "+ pr diff")
        self.assertEqual(ctx.title, "PR Title")
        self.assertEqual(ctx.description, "PR Body")
        self.assertEqual(ctx.commits, ["`abcdef1`: Commit msg"])
        self.assertEqual(ctx.comments, [{"user": "alice", "body": "comment"}])
        self.assertEqual(ctx.pr_number, "123")
        self.assertEqual(ctx.head_sha, "sha999")
        self.assertEqual(ctx.pr_author, "octocat")

    @patch("scripts.ai_pr_review.get_git_diff", return_value="+ local diff")
    @patch("scripts.ai_pr_review.get_local_commit_log", return_value=["c1", "c2"])
    def test_extract_change_context_local(self, mock_log, mock_diff):
        ctx = ai_review.extract_change_context(
            pr_number=None,
            base_ref="master",
            head_sha=None,
            explicit_author="custom_author"
        )
        self.assertEqual(ctx.diff, "+ local diff")
        self.assertIsNone(ctx.title)
        self.assertIsNone(ctx.description)
        self.assertEqual(ctx.commits, ["c1", "c2"])
        self.assertEqual(ctx.comments, [])
        self.assertIsNone(ctx.pr_number)
        self.assertEqual(ctx.pr_author, "custom_author")

    @patch("scripts.ai_pr_review.post_or_update_review_comment")
    def test_handle_empty_diff(self, mock_post):
        # Non-empty diff should return False
        non_empty = ai_review.ChangeContext(diff="+ something")
        self.assertFalse(ai_review.handle_empty_diff(non_empty, "repo", False, None))
        mock_post.assert_not_called()

        # Empty diff with PR and repo should return True and post comment
        empty_pr = ai_review.ChangeContext(diff="   ", pr_number="123", head_sha="sha1")
        self.assertTrue(ai_review.handle_empty_diff(empty_pr, "repo", False, None))
        mock_post.assert_called_once()

        # Dry run should return True but not post
        mock_post.reset_mock()
        self.assertTrue(ai_review.handle_empty_diff(empty_pr, "repo", True, None))
        mock_post.assert_not_called()

    @patch("scripts.ai_pr_review.fetch_file_context", return_value={"src/File.kt": "class File"})
    def test_resolve_context_files(self, mock_fetch):
        # When review has context-requests
        review_with_req = "LGTM\n```context-requests\nsrc/File.kt\n```"
        res = ai_review.resolve_context_files(review_with_req, "/repo")
        self.assertEqual(res, {"src/File.kt": "class File"})
        mock_fetch.assert_called_once_with("/repo", ["src/File.kt"])

        # When review has no context-requests
        res_empty = ai_review.resolve_context_files("LGTM without requests", "/repo")
        self.assertEqual(res_empty, {})

    def test_print_dry_run_results(self):
        import io
        from contextlib import redirect_stdout

        f = io.StringIO()
        with redirect_stdout(f):
            ai_review.print_dry_run_results(
                formatted_review="### Review Content",
                valid_inline=[{"path": "a.kt", "line": 10, "body": "fix"}],
                discarded_inline=[{"path": "b.kt", "line": 20, "body": "ignore"}],
                can_auto_approve=True,
                pr_author="author1",
                auto_approve_authors={"author1"}
            )
        output = f.getvalue()
        self.assertIn("DRY RUN: Generated AI Code Review", output)
        self.assertIn("### Review Content", output)
        self.assertIn("a.kt:10 -> fix", output)
        self.assertIn("b.kt:20 -> ignore", output)
        self.assertIn("Auto-Approval Check: Author 'author1' matches AUTO_APPROVE_AUTHORS", output)

    def test_build_repo_symbol_index(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            pkg_dir = os.path.join(tmpdir, "src", "main", "java", "com", "tibagni", "logviewer")
            os.makedirs(pkg_dir, exist_ok=True)

            live_log_path = os.path.join(pkg_dir, "LiveLogPresenter.kt")
            with open(live_log_path, "w", encoding="utf-8") as f:
                f.write(
                    "package com.tibagni.logviewer\n\n"
                    "interface LiveLogPresenterView {\n"
                    "    fun onLogLinesAdded()\n"
                    "}\n\n"
                    "interface LiveLogPresenter {\n"
                    "    fun start()\n"
                    "}\n\n"
                    "class LiveLogPresenterImpl : LiveLogPresenter {\n"
                    "    override fun start() {}\n"
                    "}\n"
                )

            locator_path = os.path.join(pkg_dir, "ServiceLocator.kt")
            with open(locator_path, "w", encoding="utf-8") as f:
                f.write(
                    "package com.tibagni.logviewer\n"
                    "object ServiceLocator {\n"
                    "    enum class LogLevel { DEBUG, ERROR }\n"
                    "}\n"
                )

            java_path = os.path.join(pkg_dir, "LegacyHelper.java")
            with open(java_path, "w", encoding="utf-8") as f:
                f.write(
                    "package com.tibagni.logviewer;\n"
                    "public final class LegacyHelper {}\n"
                )

            index = ai_review.build_repo_symbol_index(tmpdir)
            expected_live = "src/main/java/com/tibagni/logviewer/LiveLogPresenter.kt"
            expected_locator = "src/main/java/com/tibagni/logviewer/ServiceLocator.kt"
            expected_java = "src/main/java/com/tibagni/logviewer/LegacyHelper.java"

            self.assertEqual(index.get("LiveLogPresenterView"), expected_live)
            self.assertEqual(index.get("LiveLogPresenter"), expected_live)
            self.assertEqual(index.get("LiveLogPresenterImpl"), expected_live)
            self.assertEqual(index.get("ServiceLocator"), expected_locator)
            self.assertEqual(index.get("LogLevel"), expected_locator)
            self.assertEqual(index.get("LegacyHelper"), expected_java)

        self.assertEqual(ai_review.build_repo_symbol_index("/non/existent/repo"), {})

    def test_parse_diff_file_statuses(self):
        diff = (
            "diff --git a/src/Mod.kt b/src/Mod.kt\n"
            "--- a/src/Mod.kt\n"
            "+++ b/src/Mod.kt\n"
            "@@ -1,3 +1,4 @@\n"
            " val a = 1\n"
            "-val b = 2\n"
            "+val b = 20\n"
            "+val c = 30\n"
            " val d = 4\n"
            "diff --git a/src/New.kt b/src/New.kt\n"
            "--- /dev/null\n"
            "+++ b/src/New.kt\n"
            "@@ -0,0 +1,3 @@\n"
            "+package com.tibagni.logviewer\n"
            "+class New {\n"
            "+}\n"
            "diff --git a/src/Old.kt b/src/Old.kt\n"
            "--- a/src/Old.kt\n"
            "+++ /dev/null\n"
            "@@ -1,2 +0,0 @@\n"
            "-class Old\n"
            "-val unused = 0\n"
        )
        entries = ai_review.parse_diff_file_statuses(diff)
        self.assertEqual(len(entries), 3)

        self.assertEqual(entries[0]["path"], "src/Mod.kt")
        self.assertEqual(entries[0]["status"], "modified")
        self.assertEqual(entries[0]["added_or_context_lines"], 4)

        self.assertEqual(entries[1]["path"], "src/New.kt")
        self.assertEqual(entries[1]["status"], "added")
        self.assertEqual(entries[1]["added_or_context_lines"], 3)

        self.assertEqual(entries[2]["path"], "src/Old.kt")
        self.assertEqual(entries[2]["status"], "deleted")
        self.assertEqual(entries[2]["added_or_context_lines"], 0)

    def test_discover_referenced_repo_files(self):
        symbol_index = {
            "FooPresenter": "src/main/java/com/tibagni/logviewer/FooPresenter.kt",
            "FooPresenterImpl": "src/main/java/com/tibagni/logviewer/FooPresenterImpl.kt",
            "BugReportPresenter": (
                "src/main/java/com/tibagni/logviewer/bugreport/BugReportPresenter.kt"
            ),
            "BugReportPresenterView": (
                "src/main/java/com/tibagni/logviewer/bugreport/BugReportView.kt"
            ),
            "BugReportRepository": (
                "src/main/java/com/tibagni/logviewer/bugreport/BugReportRepository.kt"
            ),
            "StringUtils": "src/main/java/com/tibagni/logviewer/util/StringUtils.kt",
            "AlreadyModifiedView": "src/main/java/com/tibagni/logviewer/AlreadyModifiedView.kt",
        }

        # 1. Test-to-subject resolution (FooPresenterTest.kt -> FooPresenter.kt)
        test_diff = (
            "--- a/src/test/java/com/tibagni/logviewer/FooPresenterTest.kt\n"
            "+++ b/src/test/java/com/tibagni/logviewer/FooPresenterTest.kt\n"
            "@@ -1,2 +1,3 @@\n"
            "+@Test fun testFoo() {}\n"
        )
        test_entries = ai_review.parse_diff_file_statuses(test_diff)
        resolved_test = ai_review.discover_referenced_repo_files(
            test_diff, test_entries, symbol_index
        )
        self.assertIn("src/main/java/com/tibagni/logviewer/FooPresenter.kt", resolved_test)
        self.assertIn("src/main/java/com/tibagni/logviewer/FooPresenterImpl.kt", resolved_test)

        # 2. Same-package MVP peer resolution + explicit import + exclusion of modified files
        mvp_diff = (
            "--- a/src/main/java/com/tibagni/logviewer/bugreport/BugReportPresenter.kt\n"
            "+++ b/src/main/java/com/tibagni/logviewer/bugreport/BugReportPresenter.kt\n"
            "@@ -1,3 +1,6 @@\n"
            "+import com.tibagni.logviewer.util.StringUtils\n"
            " class BugReportPresenter(private val view: BugReportPresenterView) {\n"
            "+    fun load(repo: BugReportRepository, other: AlreadyModifiedView) {\n"
            "+        StringUtils.isEmpty(\"\")\n"
            "+    }\n"
            " }\n"
            "--- a/src/main/java/com/tibagni/logviewer/AlreadyModifiedView.kt\n"
            "+++ b/src/main/java/com/tibagni/logviewer/AlreadyModifiedView.kt\n"
            "@@ -1,1 +1,2 @@\n"
            "+class AlreadyModifiedView\n"
        )
        mvp_entries = ai_review.parse_diff_file_statuses(mvp_diff)
        resolved_mvp = ai_review.discover_referenced_repo_files(
            mvp_diff, mvp_entries, symbol_index
        )
        self.assertIn(
            "src/main/java/com/tibagni/logviewer/bugreport/BugReportView.kt", resolved_mvp
        )
        self.assertIn(
            "src/main/java/com/tibagni/logviewer/bugreport/BugReportRepository.kt", resolved_mvp
        )
        self.assertIn("src/main/java/com/tibagni/logviewer/util/StringUtils.kt", resolved_mvp)
        self.assertNotIn(
            "src/main/java/com/tibagni/logviewer/bugreport/BugReportPresenter.kt", resolved_mvp
        )
        self.assertNotIn(
            "src/main/java/com/tibagni/logviewer/AlreadyModifiedView.kt", resolved_mvp
        )

    def test_evaluate_deterministic_context_non_code_diff(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            diff = (
                "--- a/README.md\n"
                "+++ b/README.md\n"
                "@@ -1,1 +1,2 @@\n"
                "+Updated documentation\n"
                "--- a/src/main/resources/messages.properties\n"
                "+++ b/src/main/resources/messages.properties\n"
                "@@ -1,1 +1,2 @@\n"
                "+key=value\n"
            )
            res = ai_review.evaluate_deterministic_context(diff, tmpdir, two_pass_enabled=True)
            self.assertFalse(res.allow_context_requests)
            self.assertEqual(res.preloaded_files, {})
            self.assertIn("self-contained", res.reason)

    def test_evaluate_deterministic_context_small_focused_code_diff(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            pkg_dir = os.path.join(tmpdir, "src", "main", "java", "com", "tibagni", "logviewer")
            os.makedirs(pkg_dir, exist_ok=True)

            presenter_rel = "src/main/java/com/tibagni/logviewer/FooPresenter.kt"
            view_rel = "src/main/java/com/tibagni/logviewer/FooView.kt"
            with open(os.path.join(tmpdir, presenter_rel), "w", encoding="utf-8") as f:
                f.write(
                    "package com.tibagni.logviewer\n"
                    "class FooPresenter(private val view: FooPresenterView) {\n"
                    "    fun refresh() { view.showLoading() }\n"
                    "}\n"
                )
            with open(os.path.join(tmpdir, view_rel), "w", encoding="utf-8") as f:
                f.write(
                    "package com.tibagni.logviewer\n"
                    "interface FooPresenterView {\n"
                    "    fun showLoading()\n"
                    "}\n"
                )

            diff = (
                f"--- a/{presenter_rel}\n"
                f"+++ b/{presenter_rel}\n"
                "@@ -1,3 +1,3 @@\n"
                " class FooPresenter(private val view: FooPresenterView) {\n"
                "-    fun refresh() {}\n"
                "+    fun refresh() { view.showLoading() }\n"
                " }\n"
            )
            res = ai_review.evaluate_deterministic_context(diff, tmpdir, two_pass_enabled=True)
            self.assertFalse(res.allow_context_requests)
            self.assertIn(presenter_rel, res.preloaded_files)
            self.assertIn(view_rel, res.preloaded_files)

    def test_evaluate_deterministic_context_newly_added_file_not_redundantly_preloaded(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            pkg_dir = os.path.join(tmpdir, "src", "main", "java", "com", "tibagni", "logviewer")
            os.makedirs(pkg_dir, exist_ok=True)

            new_rel = "src/main/java/com/tibagni/logviewer/NewFeature.kt"
            dep_rel = "src/main/java/com/tibagni/logviewer/ExistingDep.kt"
            with open(os.path.join(tmpdir, new_rel), "w", encoding="utf-8") as f:
                f.write(
                    "package com.tibagni.logviewer\n"
                    "class NewFeature(private val dep: ExistingDep)\n"
                )
            with open(os.path.join(tmpdir, dep_rel), "w", encoding="utf-8") as f:
                f.write("package com.tibagni.logviewer\nclass ExistingDep\n")

            diff = (
                "--- /dev/null\n"
                f"+++ b/{new_rel}\n"
                "@@ -0,0 +1,2 @@\n"
                "+package com.tibagni.logviewer\n"
                "+class NewFeature(private val dep: ExistingDep)\n"
            )
            res = ai_review.evaluate_deterministic_context(diff, tmpdir, two_pass_enabled=True)
            self.assertFalse(res.allow_context_requests)
            self.assertNotIn(new_rel, res.preloaded_files)
            self.assertIn(dep_rel, res.preloaded_files)

    def test_evaluate_deterministic_context_large_or_overflow_keeps_pass2_enabled(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            pkg_dir = os.path.join(tmpdir, "src", "main", "java", "com", "tibagni", "logviewer")
            os.makedirs(pkg_dir, exist_ok=True)

            # Create 6 small files and 1 truncated (>600 lines) file
            for idx in range(6):
                rel = f"src/main/java/com/tibagni/logviewer/Dep{idx}.kt"
                with open(os.path.join(tmpdir, rel), "w", encoding="utf-8") as f:
                    f.write(f"package com.tibagni.logviewer\nclass Dep{idx}\n")

            huge_rel = "src/main/java/com/tibagni/logviewer/HugeFile.kt"
            with open(os.path.join(tmpdir, huge_rel), "w", encoding="utf-8") as f:
                for line_no in range(650):
                    f.write(f"val x{line_no} = {line_no}\n")

            # Case 1: Candidate count exceeds max_prefetch_files (5)
            mod_rel = "src/main/java/com/tibagni/logviewer/Dep0.kt"
            overflow_diff = (
                f"--- a/{mod_rel}\n"
                f"+++ b/{mod_rel}\n"
                "@@ -1,2 +1,3 @@\n"
                "+// uses Dep1 Dep2 Dep3 Dep4 Dep5\n"
            )
            res_overflow = ai_review.evaluate_deterministic_context(
                overflow_diff, tmpdir, two_pass_enabled=True, max_prefetch_files=5
            )
            self.assertTrue(res_overflow.allow_context_requests)
            self.assertEqual(len(res_overflow.preloaded_files), 5)

            # Case 2: Modified file exceeds MAX_CONTEXT_FILE_LINES
            huge_diff = (
                f"--- a/{huge_rel}\n"
                f"+++ b/{huge_rel}\n"
                "@@ -1,1 +1,2 @@\n"
                "+val extra = 1\n"
            )
            res_huge = ai_review.evaluate_deterministic_context(
                huge_diff, tmpdir, two_pass_enabled=True
            )
            self.assertTrue(res_huge.allow_context_requests)

            # Case 3: Diff exceeds SMALL_DIFF_MAX_LINES (> 400 lines)
            long_diff_lines = [f"--- a/{mod_rel}", f"+++ b/{mod_rel}", "@@ -1,1 +1,405 @@"]
            long_diff_lines.extend(f"+val line{i} = {i}" for i in range(405))
            res_long_diff = ai_review.evaluate_deterministic_context(
                "\n".join(long_diff_lines), tmpdir, two_pass_enabled=True
            )
            self.assertTrue(res_long_diff.allow_context_requests)

            # Case 4: Diff touches > SMALL_DIFF_MAX_CODE_FILES (4 code files)
            multi_file_chunks = []
            for idx in range(4):
                rel = f"src/main/java/com/tibagni/logviewer/Dep{idx}.kt"
                multi_file_chunks.append(
                    f"--- a/{rel}\n+++ b/{rel}\n@@ -1,1 +1,2 @@\n+// edit\n"
                )
            res_multi_code = ai_review.evaluate_deterministic_context(
                "".join(multi_file_chunks), tmpdir, two_pass_enabled=True
            )
            self.assertTrue(res_multi_code.allow_context_requests)

    def test_build_prompt_preloaded_context_and_ci_assumption(self):
        prompt_with_pass2 = ai_review.build_prompt(
            guidelines="Rules",
            diff="+ val x = 1",
            allow_context_requests=True,
            preloaded_context={"src/Foo.kt": "class Foo"}
        )
        self.assertIn("## Pre-loaded Repository File Context:", prompt_with_pass2)
        self.assertIn("### File: `src/Foo.kt`", prompt_with_pass2)
        self.assertIn("class Foo", prompt_with_pass2)
        self.assertIn("CI Build & Compilation Assumption", prompt_with_pass2)
        self.assertIn("Assume the PR Compiles and Passes CI Tests", prompt_with_pass2)
        self.assertIn(
            "Do not request files that are already provided in the "
            "Pre-loaded Repository File Context section.",
            prompt_with_pass2
        )

        prompt_single_pass = ai_review.build_prompt(
            guidelines="Rules",
            diff="+ val x = 1",
            allow_context_requests=False,
            preloaded_context={"src/Foo.kt": "class Foo"}
        )
        self.assertIn("## Pre-loaded Repository File Context:", prompt_single_pass)
        self.assertIn("CI Build & Compilation Assumption", prompt_single_pass)
        self.assertNotIn("Optional Context Request (Two-Pass Review)", prompt_single_pass)

        pass2_prompt = ai_review.build_second_pass_prompt(
            guidelines="Rules",
            diff="+ val x = 1",
            first_pass_review="Draft",
            file_contexts={"src/Foo.kt": "class Foo"}
        )
        self.assertIn("Assume the PR Compiles and Passes CI Tests", pass2_prompt)

    @patch("scripts.ai_pr_review.call_llm_api")
    @patch("scripts.ai_pr_review.fetch_file_context")
    def test_resolve_context_files_and_run_second_pass_deduplication(
        self, mock_fetch, mock_call_llm
    ):
        review_req_preloaded = "Draft\n```context-requests\nsrc/Already.kt\n```"

        # 1. All requested files were already preloaded -> returns {} without fetching
        dedup_res = ai_review.resolve_context_files(
            review_req_preloaded,
            "/repo",
            already_loaded={"src/Already.kt"}
        )
        self.assertEqual(dedup_res, {})
        mock_fetch.assert_not_called()

        # 2. run_second_pass_if_needed skips Pass 2 API call and preserves verified_files
        change = ai_review.ChangeContext(diff="+ val x = 1")
        final_text, model_used, verified = ai_review.run_second_pass_if_needed(
            api_key="key",
            guidelines="Rules",
            change=change,
            first_pass_review=review_req_preloaded,
            model_used="gemini-4-flash",
            preloaded_context={"src/Already.kt": "class Already"}
        )
        self.assertEqual(final_text, review_req_preloaded)
        self.assertEqual(model_used, "gemini-4-flash")
        self.assertEqual(verified, ["src/Already.kt"])
        mock_call_llm.assert_not_called()

        # 3. Mix of already-loaded and new file only fetches new file and merges verified_files
        review_req_mixed = "Draft\n```context-requests\nsrc/Already.kt\nsrc/NewExtra.kt\n```"
        mock_fetch.return_value = {"src/NewExtra.kt": "class NewExtra"}
        mock_call_llm.return_value = ("### Overall Verdict\n✅ LGTM", "gemini-4-flash")

        final_text2, _, verified2 = ai_review.run_second_pass_if_needed(
            api_key="key",
            guidelines="Rules",
            change=change,
            first_pass_review=review_req_mixed,
            model_used="gemini-4-flash",
            preloaded_context={"src/Already.kt": "class Already"}
        )
        mock_fetch.assert_called_once()
        self.assertEqual(mock_fetch.call_args[0][1], ["src/NewExtra.kt"])
        mock_call_llm.assert_called_once()
        self.assertEqual(final_text2, "### Overall Verdict\n✅ LGTM")
        self.assertEqual(verified2, ["src/Already.kt", "src/NewExtra.kt"])

    @patch("scripts.ai_pr_review.post_or_update_review_comment")
    @patch("scripts.ai_pr_review.get_head_sha", return_value="sha123")
    @patch("scripts.ai_pr_review.get_repo_name", return_value="tibagni/LogViewer")
    @patch(
        "scripts.ai_pr_review.get_pr_metadata",
        return_value={"title": "T", "body": "B", "commits": [], "filtered_comments": []}
    )
    @patch(
        "scripts.ai_pr_review.get_pr_diff",
        return_value="--- a/src/A.kt\n+++ b/src/A.kt\n@@ -1,1 +1,2 @@\n+val x = 1\n"
    )
    @patch(
        "scripts.ai_pr_review.evaluate_deterministic_context",
        return_value=ai_review.DeterministicContextResult(
            preloaded_files={"src/A.kt": "class A", "src/B.kt": "class B"},
            allow_context_requests=False,
            reason="small diff and 100% of modified/referenced context files preloaded in Pass 1"
        )
    )
    @patch(
        "scripts.ai_pr_review.call_llm_api",
        return_value=("### Overall Verdict\n✅ LGTM", "gemini-4-flash")
    )
    def test_main_single_pass_preloaded_footer_attribution(
        self,
        mock_call,
        mock_eval_ctx,
        mock_diff,
        mock_meta,
        mock_repo,
        mock_sha,
        mock_post_top
    ):
        with patch.dict(os.environ, {"LLM_API_KEY": "test_key"}):
            exit_code = ai_review.main(["--pr", "101", "--env-file", "/non/existent/.env"])
            self.assertEqual(exit_code, 0)
            mock_call.assert_called_once()
            mock_post_top.assert_called_once()
            comment_posted = mock_post_top.call_args[0][2]
            self.assertIn(
                "(`gemini-4-flash`, context-verified: `src/A.kt`, `src/B.kt`)",
                comment_posted
            )


if __name__ == "__main__":
    unittest.main()

