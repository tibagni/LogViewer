#!/usr/bin/env python3
"""
Unit tests for scripts/ai_pr_review.py
"""

import json
import os
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

    @patch.dict(os.environ, {}, clear=True)
    def test_main_no_api_key(self):
        exit_code = ai_review.main(["--env-file", "/non/existent/.env"])
        self.assertEqual(exit_code, 0)

    @patch("scripts.ai_pr_review.call_llm_api", return_value="Review output")
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
    @patch("scripts.ai_pr_review.call_llm_api", return_value="LGTM\n```inline-comments\n[{\"path\": \"src/F.kt\", \"line\": 1, \"body\": \"Nice\"}]\n```")
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


if __name__ == "__main__":
    unittest.main()
