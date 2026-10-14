#!/usr/bin/env python3
"""
Unit tests for scripts/gemini_pr_review.py
"""

import json
import os
import tempfile
import unittest
from unittest.mock import MagicMock, patch

from scripts import gemini_pr_review


class TestGeminiPrReview(unittest.TestCase):

    def test_build_prompt(self):
        guidelines = "Follow Kotlin idioms."
        diff = "+fun test() = Unit"
        prompt = gemini_pr_review.build_prompt(guidelines, diff)
        self.assertIn("Follow Kotlin idioms.", prompt)
        self.assertIn("+fun test() = Unit", prompt)

    def test_load_review_guidelines_existing(self):
        content = "# Custom Guidelines"
        with tempfile.NamedTemporaryFile("w+", delete=False) as tmp:
            tmp.write(content)
            tmp_path = tmp.name

        try:
            loaded = gemini_pr_review.load_review_guidelines(tmp_path)
            self.assertEqual(loaded, content)
        finally:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)

    def test_load_review_guidelines_missing(self):
        loaded = gemini_pr_review.load_review_guidelines("/nonexistent/file.md")
        self.assertIn("code review", loaded.lower())

    @patch("scripts.gemini_pr_review.get_available_models", return_value=("v1beta", ["gemini-3.8-flash"]))
    @patch("urllib.request.urlopen")
    def test_call_gemini_api_success(self, mock_urlopen, mock_models):
        mock_response = MagicMock()
        response_body = {
            "candidates": [
                {
                    "content": {
                        "parts": [
                            {"text": "✅ LGTM. Code looks great!"}
                        ]
                    }
                }
            ]
        }
        mock_response.read.return_value = json.dumps(response_body).encode("utf-8")
        mock_response.__enter__.return_value = mock_response
        mock_urlopen.return_value = mock_response

        res = gemini_pr_review.call_gemini_api("dummy_key", "prompt text")
        self.assertEqual(res, "✅ LGTM. Code looks great!")

    @patch("urllib.request.urlopen")
    def test_get_available_models_discovers_available(self, mock_urlopen):
        mock_response = MagicMock()
        response_body = {
            "models": [
                {
                    "name": "models/gemini-2.0-flash",
                    "supportedGenerationMethods": ["generateContent"]
                },
                {
                    "name": "models/gemini-3.8-flash",
                    "supportedGenerationMethods": ["generateContent"]
                },
                {
                    "name": "models/text-embedding-004",
                    "supportedGenerationMethods": ["embedContent"]
                }
            ]
        }
        mock_response.read.return_value = json.dumps(response_body).encode("utf-8")
        mock_response.__enter__.return_value = mock_response
        mock_urlopen.return_value = mock_response

        version, models = gemini_pr_review.get_available_models("dummy_key")
        self.assertEqual(version, "v1beta")
        self.assertEqual(models, ["gemini-2.0-flash", "gemini-3.8-flash"])

    @patch("subprocess.run")
    def test_find_existing_comment_found(self, mock_subproc):
        comments = [
            {"id": 101, "body": "Standard human comment"},
            {"id": 202, "body": f"{gemini_pr_review.COMMENT_MARKER}\nPrevious review"}
        ]
        mock_subproc.return_value = MagicMock(
            stdout=json.dumps(comments), returncode=0
        )

        found_id = gemini_pr_review.find_existing_comment("12", "owner/repo")
        self.assertEqual(found_id, 202)

    @patch("subprocess.run")
    def test_find_existing_comment_none(self, mock_subproc):
        comments = [
            {"id": 101, "body": "Standard human comment"}
        ]
        mock_subproc.return_value = MagicMock(
            stdout=json.dumps(comments), returncode=0
        )

        found_id = gemini_pr_review.find_existing_comment("12", "owner/repo")
        self.assertIsNone(found_id)

    @patch("scripts.gemini_pr_review.find_existing_comment", return_value=None)
    @patch("subprocess.run")
    def test_post_new_review_comment(self, mock_subproc, mock_find):
        gemini_pr_review.post_or_update_review_comment("12", "owner/repo", "Review body")
        mock_subproc.assert_called_once()
        args = mock_subproc.call_args[0][0]
        self.assertIn("POST", args)
        self.assertIn("repos/owner/repo/issues/12/comments", args)

    @patch("scripts.gemini_pr_review.find_existing_comment", return_value=555)
    @patch("subprocess.run")
    def test_update_existing_review_comment(self, mock_subproc, mock_find):
        gemini_pr_review.post_or_update_review_comment("12", "owner/repo", "Updated body")
        mock_subproc.assert_called_once()
        args = mock_subproc.call_args[0][0]
        self.assertIn("PATCH", args)
        self.assertIn("repos/owner/repo/issues/comments/555", args)

    @patch.dict(os.environ, {}, clear=True)
    def test_main_missing_api_key(self):
        exit_code = gemini_pr_review.main()
        self.assertEqual(exit_code, 0)


if __name__ == "__main__":
    unittest.main()
