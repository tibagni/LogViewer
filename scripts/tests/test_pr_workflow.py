#!/usr/bin/env python3
"""
Unit tests for scripts/pr-workflow.sh
"""

import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
SCRIPT_PATH = REPO_ROOT / "scripts" / "pr-workflow.sh"


class TestPrWorkflowScript(unittest.TestCase):

    def setUp(self):
        self.test_dir = tempfile.mkdtemp()
        self.mock_bin = os.path.join(self.test_dir, "mock_bin")
        os.makedirs(self.mock_bin, exist_ok=True)

        # Create a mock 'gh' CLI in mock_bin
        mock_gh = os.path.join(self.mock_bin, "gh")
        with open(mock_gh, "w") as f:
            f.write("#!/bin/bash\n")
            f.write("if [ \"$1\" = \"pr\" ] && [ \"$2\" = \"list\" ]; then\n")
            f.write("  echo ''\n")
            f.write("elif [ \"$1\" = \"pr\" ] && [ \"$2\" = \"create\" ]; then\n")
            f.write("  echo 'https://github.com/tibagni/LogViewer/pull/999'\n")
            f.write("fi\n")
            f.write("exit 0\n")
        os.chmod(mock_gh, 0o755)

        # Base env for running tests with mock_bin prioritized
        self.env = os.environ.copy()
        self.env["PATH"] = f"{self.mock_bin}:{self.env.get('PATH', '')}"

    def tearDown(self):
        shutil.rmtree(self.test_dir, ignore_errors=True)

    def _init_repo(self, repo_path: str) -> None:
        """Initializes a local git repo with an initial commit on master."""
        subprocess.run(["git", "init", "-b", "master", repo_path], check=True, capture_output=True)
        subprocess.run(["git", "-C", repo_path, "config", "user.name", "Test User"], check=True)
        subprocess.run(["git", "-C", repo_path, "config", "user.email", "test@example.com"], check=True)
        dummy_file = os.path.join(repo_path, "README.md")
        with open(dummy_file, "w") as f:
            f.write("# Test\n")
        subprocess.run(["git", "-C", repo_path, "add", "."], check=True)
        subprocess.run(["git", "-C", repo_path, "commit", "-m", "Initial commit"], check=True)

    def test_help_flag(self):
        res = subprocess.run([str(SCRIPT_PATH), "--help"], capture_output=True, text=True, stdin=subprocess.DEVNULL, env=self.env)
        self.assertEqual(res.returncode, 0)
        self.assertIn("Usage:", res.stdout)
        self.assertIn("--quick", res.stdout)
        self.assertIn("--draft", res.stdout)
        self.assertIn("--watch", res.stdout)

    def test_unknown_option(self):
        res = subprocess.run([str(SCRIPT_PATH), "--unknown"], capture_output=True, text=True, stdin=subprocess.DEVNULL, env=self.env)
        self.assertEqual(res.returncode, 1)
        self.assertIn("Unknown option", res.stdout)

    def test_not_in_git_repo(self):
        empty_dir = os.path.join(self.test_dir, "empty")
        os.makedirs(empty_dir, exist_ok=True)
        res = subprocess.run([str(SCRIPT_PATH), "feat/test"], cwd=empty_dir, capture_output=True, text=True, stdin=subprocess.DEVNULL, env=self.env)
        self.assertEqual(res.returncode, 1)
        self.assertIn("Not inside a git repository", res.stdout)

    def test_on_master_without_branch_non_interactive(self):
        repo_path = os.path.join(self.test_dir, "repo1")
        self._init_repo(repo_path)
        res = subprocess.run([str(SCRIPT_PATH)], cwd=repo_path, capture_output=True, text=True, stdin=subprocess.DEVNULL, env=self.env)
        self.assertEqual(res.returncode, 1)
        self.assertIn("You are on master", res.stdout)

    def test_no_commits_ahead_of_master(self):
        repo_path = os.path.join(self.test_dir, "repo2")
        self._init_repo(repo_path)
        # Create branch feat/no-commits from master without any new commits
        subprocess.run(["git", "-C", repo_path, "checkout", "-b", "feat/no-commits"], check=True)
        res = subprocess.run([str(SCRIPT_PATH)], cwd=repo_path, capture_output=True, text=True, stdin=subprocess.DEVNULL, env=self.env)
        self.assertEqual(res.returncode, 0)
        self.assertIn("No commits ahead of origin/master", res.stdout)

    def test_existing_branch_prompt_abort(self):
        repo_path = os.path.join(self.test_dir, "repo3")
        self._init_repo(repo_path)
        # Create an existing branch feat/existing
        subprocess.run(["git", "-C", repo_path, "branch", "feat/existing"], check=True)
        # Make a new commit on master
        with open(os.path.join(repo_path, "change.txt"), "w") as f:
            f.write("New change\n")
        subprocess.run(["git", "-C", repo_path, "add", "."], check=True)
        subprocess.run(["git", "-C", repo_path, "commit", "-m", "Commit on master"], check=True)

        # Run script targeting feat/existing, selecting option 2 (abort)
        proc = subprocess.Popen(
            [str(SCRIPT_PATH), "feat/existing"],
            cwd=repo_path,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            env=self.env
        )
        stdout, stderr = proc.communicate(input="2\n")
        self.assertEqual(proc.returncode, 1)
        self.assertIn("Branch 'feat/existing' already exists locally", stdout)
        self.assertIn("Aborted", stdout)

    def test_existing_branch_prompt_reset(self):
        repo_path = os.path.join(self.test_dir, "repo4")
        self._init_repo(repo_path)
        # Create an existing branch feat/existing pointing to initial commit
        subprocess.run(["git", "-C", repo_path, "branch", "feat/existing"], check=True)

        # Set up dummy remote origin
        bare_remote = os.path.join(self.test_dir, "remote4.git")
        subprocess.run(["git", "init", "--bare", bare_remote], check=True)
        subprocess.run(["git", "-C", repo_path, "remote", "add", "origin", bare_remote], check=True)
        subprocess.run(["git", "-C", repo_path, "push", "origin", "master"], check=True)

        # Make a new commit on master
        with open(os.path.join(repo_path, "change.txt"), "w") as f:
            f.write("New change\n")
        subprocess.run(["git", "-C", repo_path, "add", "."], check=True)
        subprocess.run(["git", "-C", repo_path, "commit", "-m", "Commit on master"], check=True)

        # Run script targeting feat/existing, selecting option 1 (reset)
        proc = subprocess.Popen(
            [str(SCRIPT_PATH), "feat/existing"],
            cwd=repo_path,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            env=self.env
        )
        stdout, stderr = proc.communicate(input="1\n")
        self.assertEqual(proc.returncode, 0)
        self.assertIn("Reset and switched to 'feat/existing' at current HEAD", stdout)
        self.assertIn("Pull request created successfully", stdout)

        # Verify feat/existing is current branch
        current_branch = subprocess.run(
            ["git", "-C", repo_path, "branch", "--show-current"],
            capture_output=True, text=True, check=True
        ).stdout.strip()
        self.assertEqual(current_branch, "feat/existing")

    def test_default_mode_stays_on_branch(self):
        repo_path = os.path.join(self.test_dir, "repo5")
        self._init_repo(repo_path)
        bare_remote = os.path.join(self.test_dir, "remote5.git")
        subprocess.run(["git", "init", "--bare", bare_remote], check=True)
        subprocess.run(["git", "-C", repo_path, "remote", "add", "origin", bare_remote], check=True)
        subprocess.run(["git", "-C", repo_path, "push", "origin", "master"], check=True)

        # Make a commit on master
        with open(os.path.join(repo_path, "change.txt"), "w") as f:
            f.write("New change\n")
        subprocess.run(["git", "-C", repo_path, "add", "."], check=True)
        subprocess.run(["git", "-C", repo_path, "commit", "-m", "Commit on master"], check=True)

        res = subprocess.run(
            [str(SCRIPT_PATH), "feat/stay-here"],
            cwd=repo_path,
            capture_output=True,
            text=True,
            stdin=subprocess.DEVNULL,
            env=self.env
        )
        self.assertEqual(res.returncode, 0)
        self.assertIn("You are currently on branch 'feat/stay-here'", res.stdout)
        current_branch = subprocess.run(
            ["git", "-C", repo_path, "branch", "--show-current"],
            capture_output=True, text=True, check=True
        ).stdout.strip()
        self.assertEqual(current_branch, "feat/stay-here")

    def test_quick_mode_cleans_up_to_master(self):
        repo_path = os.path.join(self.test_dir, "repo6")
        self._init_repo(repo_path)
        bare_remote = os.path.join(self.test_dir, "remote6.git")
        subprocess.run(["git", "init", "--bare", bare_remote], check=True)
        subprocess.run(["git", "-C", repo_path, "remote", "add", "origin", bare_remote], check=True)
        subprocess.run(["git", "-C", repo_path, "push", "origin", "master"], check=True)

        # Make a commit on master
        with open(os.path.join(repo_path, "change.txt"), "w") as f:
            f.write("New change\n")
        subprocess.run(["git", "-C", repo_path, "add", "."], check=True)
        subprocess.run(["git", "-C", repo_path, "commit", "-m", "Commit on master"], check=True)

        res = subprocess.run(
            [str(SCRIPT_PATH), "--quick", "feat/quick-fix"],
            cwd=repo_path,
            capture_output=True,
            text=True,
            stdin=subprocess.DEVNULL,
            env=self.env
        )
        self.assertEqual(res.returncode, 0)
        self.assertIn("Quick mode enabled: cleaning up local branch", res.stdout)
        self.assertIn("Switched back to 'master' and deleted local branch 'feat/quick-fix'", res.stdout)
        current_branch = subprocess.run(
            ["git", "-C", repo_path, "branch", "--show-current"],
            capture_output=True, text=True, check=True
        ).stdout.strip()
        self.assertEqual(current_branch, "master")


if __name__ == "__main__":
    unittest.main()
