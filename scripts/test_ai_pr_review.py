#!/usr/bin/env python3
"""
Unit tests for scripts/ai_pr_review.py
"""

import json
import os
import tempfile
import unittest
from unittest.mock import MagicMock, patch

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
                {"name": "models/gemini-2.0-flash", "supportedGenerationMethods": ["generateContent"]},
                {"name": "models/gemini-pro-latest", "supportedGenerationMethods": ["generateContent"]}
            ]
        }).encode("utf-8")
        mock_urlopen.return_value.__enter__.return_value = mock_response

        version, models = ai_review.get_available_models("dummy_key")
        self.assertEqual(version, "v1beta")
        self.assertIn("gemini-2.0-flash", models)

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

        result = ai_review.call_llm_api("dummy_key", "prompt here", preferred_model="gemini-3.8-flash")
        self.assertEqual(result, "Review feedback: LGTM!")

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


if __name__ == "__main__":
    unittest.main()
