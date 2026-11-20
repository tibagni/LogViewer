#!/usr/bin/env python3
"""Unit tests for update_screenshots.py."""

import os
import shutil
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import MagicMock, patch

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
if str(REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(REPO_ROOT))

from scripts.update_screenshots import (
    ALL_SCREENSHOTS,
    README_SCREENSHOTS,
    ensure_xauthority,
    is_branch_ahead,
    update_gh_pages_images,
    update_master_readme_images,
)


class UpdateScreenshotsTests(unittest.TestCase):

    def setUp(self):
        self.test_dir = tempfile.mkdtemp(prefix="test_screenshots_")
        self.repo_dir = Path(self.test_dir) / "repo"
        self.temp_screenshots_dir = Path(self.test_dir) / "generated"

        self.repo_dir.mkdir(parents=True)
        self.temp_screenshots_dir.mkdir(parents=True)

        # Create dummy screenshots in temp dir
        for name in ALL_SCREENSHOTS:
            (self.temp_screenshots_dir / name).write_bytes(b"dummy image bytes")

    def tearDown(self):
        shutil.rmtree(self.test_dir, ignore_errors=True)

    def test_readme_screenshots_subset_of_all(self):
        for name in README_SCREENSHOTS:
            self.assertIn(name, ALL_SCREENSHOTS)

    def test_ensure_xauthority_finds_live_mutter_auth(self):
        fake_mutter_dir = Path(self.test_dir) / "mutter_user"
        fake_mutter_dir.mkdir()
        cookie1 = fake_mutter_dir / ".mutter-Xwaylandauth.OLD"
        cookie2 = fake_mutter_dir / ".mutter-Xwaylandauth.NEW"
        cookie1.write_text("old")
        cookie2.write_text("new")

        with patch.dict(os.environ, {"XAUTHORITY": "/nonexistent/auth"}, clear=False), \
             patch("scripts.update_screenshots.os.getuid", return_value=1000), \
             patch("scripts.update_screenshots.Path", side_effect=lambda p: fake_mutter_dir if str(p) == "/run/user/1000" else Path(p)):
            ensure_xauthority()
            self.assertTrue(os.environ["XAUTHORITY"].endswith("NEW") or os.environ["XAUTHORITY"].endswith("OLD"))

    @patch("scripts.update_screenshots.git")
    def test_update_master_readme_images_copies_only_readme_subset(self, mock_git):
        # Mock git status to indicate files were modified
        mock_git.return_value = MagicMock(stdout=" M docs/images/main_view.png\n", returncode=0)

        changed = update_master_readme_images(self.repo_dir, self.temp_screenshots_dir, dry_run=False)

        self.assertTrue(changed)
        dest_dir = self.repo_dir / "docs" / "images"
        for name in README_SCREENSHOTS:
            self.assertTrue((dest_dir / name).exists())

        # Other non-README screenshots should NOT be copied on master
        self.assertFalse((dest_dir / "keymap-settings.png").exists())
        self.assertFalse((dest_dir / "filter-editor.png").exists())

    @patch("scripts.update_screenshots.git")
    def test_update_gh_pages_images_copies_all_screenshots(self, mock_git):
        mock_git.return_value = MagicMock(stdout=" M docs/images/main_view.png\n", returncode=0)

        changed = update_gh_pages_images(self.repo_dir, self.temp_screenshots_dir, dry_run=False)

        self.assertTrue(changed)
        dest_dir = self.repo_dir / "docs" / "images"
        for name in ALL_SCREENSHOTS:
            self.assertTrue((dest_dir / name).exists(), f"Missing {name} in docs/images")

    @patch("scripts.update_screenshots.git")
    def test_dry_run_reverts_changes_without_commit(self, mock_git):
        mock_git.return_value = MagicMock(stdout=" M docs/images/main_view.png\n", returncode=0)

        changed = update_master_readme_images(self.repo_dir, self.temp_screenshots_dir, dry_run=True)
        self.assertFalse(changed)
        # Verify git checkout -- docs/images/ was called
        mock_git.assert_any_call(["checkout", "--", "docs/images/"], self.repo_dir)

    @patch("scripts.update_screenshots.git")
    def test_is_branch_ahead(self, mock_git):
        # When rev-list returns empty, not ahead
        mock_git.side_effect = [
            MagicMock(stdout="origin/master\n", returncode=0),
            MagicMock(stdout="", returncode=0),
        ]
        self.assertFalse(is_branch_ahead("master", self.repo_dir))

        # When rev-list returns commit sha, ahead
        mock_git.side_effect = [
            MagicMock(stdout="origin/master\n", returncode=0),
            MagicMock(stdout="abc1234\n", returncode=0),
        ]
        self.assertTrue(is_branch_ahead("master", self.repo_dir))


if __name__ == "__main__":
    unittest.main()
