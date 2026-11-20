#!/usr/bin/env python3
"""
update_screenshots.py - Automated Screenshot Pipeline for LogViewer.

Automates the complete screenshot lifecycle:
  1. Generates fresh screenshots off-screen in a temporary directory via Gradle.
  2. Ensures the repository is on 'master' and updates the README screenshots in docs/images/.
  3. Commits README image changes if updated.
  4. Switches to the 'gh-pages' branch and updates all 17 website documentation screenshots.
  5. Commits website image changes if updated.
  6. Switches back to 'master'.
  7. If --push is provided, pushes both 'master' and 'gh-pages' to 'origin'.

Auto-heals stale XAUTHORITY tokens in Wayland / GNOME Mutter / Herdr sessions.

Usage:
  python3 scripts/update_screenshots.py
  python3 scripts/update_screenshots.py --push
  python3 scripts/update_screenshots.py --dry-run
"""

from __future__ import annotations

import argparse
import glob
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

README_SCREENSHOTS = [
    "main_view.png",
    "live_logs.png",
    "bugreport_app_view.png",
]

ALL_SCREENSHOTS = [
    "main_view.png",
    "live_logs.png",
    "bugreport_app_view.png",
    "search-bar.png",
    "stream-indication.png",
    "visible-logs-menu.png",
    "elapsed-time-menu.png",
    "filter-search.png",
    "filter-editor.png",
    "keymap-settings.png",
    "line-inspector.png",
    "visible-logs-dialog.png",
    "filter-details.png",
    "regex-editor.png",
    "log-context-menu.png",
    "go-to-dialog.png",
    "selecting-streams.png",
]


def find_repo_root() -> Path:
    """Finds the root of the git repository containing this script."""
    current = Path(__file__).resolve().parent.parent
    try:
        proc = subprocess.run(
            ["git", "-C", str(current), "rev-parse", "--show-toplevel"],
            capture_output=True,
            text=True,
            check=False,
        )
        if proc.returncode == 0 and proc.stdout.strip():
            return Path(proc.stdout.strip()).resolve()
    except Exception:
        pass
    return current


def ensure_xauthority() -> None:
    """
    Auto-heals stale XAUTHORITY if running in Wayland / GNOME Mutter where the
    auth cookie rotates on desktop reloads or inside terminal multiplexers / Herdr.
    """
    xauth = os.environ.get("XAUTHORITY")
    if not xauth or not os.path.exists(xauth):
        uid = os.getuid()
        mutter_dir = Path(f"/run/user/{uid}")
        if mutter_dir.exists():
            cookies = sorted(
                mutter_dir.glob(".mutter-Xwaylandauth.*"),
                key=lambda p: p.stat().st_mtime,
                reverse=True,
            )
            if cookies:
                os.environ["XAUTHORITY"] = str(cookies[0])
                print(f"[AUTH] Auto-healed XAUTHORITY to: {cookies[0]}")


def git(cmd: list[str], repo_root: Path, check: bool = True) -> subprocess.CompletedProcess:
    """Executes a git command in the repository root."""
    full_cmd = ["git", "-C", str(repo_root)] + cmd
    return subprocess.run(full_cmd, capture_output=True, text=True, check=check)


def check_working_tree_clean(repo_root: Path) -> None:
    """Ensures tracked files have no uncommitted changes before switching branches."""
    proc = git(["diff-index", "--quiet", "HEAD", "--"], repo_root, check=False)
    if proc.returncode != 0:
        status_proc = git(["status", "--short", "--untracked-files=no"], repo_root, check=False)
        print(
            "[ERROR] Working tree has uncommitted tracked changes:\n"
            + status_proc.stdout
            + "\nPlease commit or stash your changes before running.",
            file=sys.stderr,
        )
        sys.exit(1)


def generate_screenshots_to_temp(repo_root: Path, temp_dir: Path) -> None:
    """Invokes ./gradlew generateScreenshots -PoutputDir=<temp_dir>."""
    ensure_xauthority()
    gradlew = repo_root / "gradlew"
    cmd = [str(gradlew), "generateScreenshots", f"-PoutputDir={temp_dir}"]
    print(f"[STEP 1/4] Generating screenshots into temp folder: {temp_dir}")

    proc = subprocess.run(cmd, cwd=str(repo_root), check=False)
    if proc.returncode != 0:
        print("[ERROR] Gradle screenshot generation failed!", file=sys.stderr)
        sys.exit(1)

    generated = list(temp_dir.glob("*.png"))
    print(f"[STEP 1/4] Successfully generated {len(generated)} screenshots.")
    if len(generated) < len(README_SCREENSHOTS):
        print(
            f"[ERROR] Expected at least {len(README_SCREENSHOTS)} screenshots, found {len(generated)}",
            file=sys.stderr,
        )
        sys.exit(1)


def update_master_readme_images(repo_root: Path, temp_dir: Path, dry_run: bool = False) -> bool:
    """Ensures on master, updates README images, and commits if changed."""
    print("[STEP 2/4] Updating README screenshots on 'master'...")
    git(["checkout", "master"], repo_root)

    dest_dir = repo_root / "docs" / "images"
    dest_dir.mkdir(parents=True, exist_ok=True)

    for name in README_SCREENSHOTS:
        src = temp_dir / name
        if src.exists():
            shutil.copy2(src, dest_dir / name)

    status_proc = git(["status", "--porcelain", "docs/images/"], repo_root)
    if not status_proc.stdout.strip():
        print("  -> README screenshots are already up to date on 'master'.")
        return False

    print("  -> Found updated README screenshots:")
    for line in status_proc.stdout.strip().splitlines():
        print(f"     {line}")

    if dry_run:
        print("  -> [DRY-RUN] Skipping commit on 'master'.")
        git(["checkout", "--", "docs/images/"], repo_root)
        return False

    files_to_add = [f"docs/images/{name}" for name in README_SCREENSHOTS]
    git(["add"] + files_to_add, repo_root)
    git(["commit", "-m", "Update README screenshots with latest UI"], repo_root)
    print("  -> Committed updated README screenshots on 'master'.")
    return True


def update_gh_pages_images(repo_root: Path, temp_dir: Path, dry_run: bool = False) -> bool:
    """Switches to gh-pages, updates website images, and commits if changed."""
    print("[STEP 3/4] Updating documentation screenshots on 'gh-pages'...")
    git(["checkout", "gh-pages"], repo_root)

    dest_dir = repo_root / "docs" / "images"
    dest_dir.mkdir(parents=True, exist_ok=True)

    copied_count = 0
    for src in temp_dir.glob("*.png"):
        shutil.copy2(src, dest_dir / src.name)
        copied_count += 1

    status_proc = git(["status", "--porcelain", "docs/images/"], repo_root)
    if not status_proc.stdout.strip():
        print("  -> Documentation screenshots are already up to date on 'gh-pages'.")
        return False

    print(f"  -> Found updated website screenshots ({copied_count} checked):")
    for line in status_proc.stdout.strip().splitlines():
        print(f"     {line}")

    if dry_run:
        print("  -> [DRY-RUN] Skipping commit on 'gh-pages'.")
        git(["checkout", "--", "docs/images/"], repo_root)
        return False

    git(["add", "docs/images/*.png"], repo_root)
    git(["commit", "-m", "Update documentation screenshots with latest UI"], repo_root)
    print("  -> Committed updated documentation screenshots on 'gh-pages'.")
    return True


def is_branch_ahead(branch: str, repo_root: Path) -> bool:
    """Checks if a local branch is ahead of its tracking remote."""
    proc = git(["rev-parse", "--abbrev-ref", f"{branch}@{{u}}"], repo_root, check=False)
    if proc.returncode != 0:
        return True  # No upstream configured, assume push needed
    upstream = proc.stdout.strip()
    rev_proc = git(["rev-list", f"{upstream}..{branch}"], repo_root, check=False)
    return bool(rev_proc.stdout.strip())


def push_branches(repo_root: Path, master_changed: bool, gh_pages_changed: bool) -> None:
    """Pushes master and gh-pages to origin if there are outgoing commits."""
    print("[PUSH] Pushing updated branches to origin...")

    # Push master if ahead
    if master_changed or is_branch_ahead("master", repo_root):
        print("  -> Pushing 'master' to origin...")
        git(["push", "origin", "master"], repo_root)
        print("  -> 'master' pushed successfully.")
    else:
        print("  -> 'master' is already in sync with origin.")

    # Push gh-pages if ahead
    if gh_pages_changed or is_branch_ahead("gh-pages", repo_root):
        print("  -> Pushing 'gh-pages' to origin...")
        git(["push", "origin", "gh-pages"], repo_root)
        print("  -> 'gh-pages' pushed successfully.")
    else:
        print("  -> 'gh-pages' is already in sync with origin.")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Automated screenshot generation and multi-branch updater for LogViewer"
    )
    parser.add_argument(
        "--push",
        action="store_true",
        help="Push updated master and gh-pages branches to origin",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Generate screenshots and check for changes without committing or pushing",
    )
    parser.add_argument(
        "--repo-dir",
        help="Custom path to LogViewer repository root",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_dir).resolve() if args.repo_dir else find_repo_root()
    print(f"=== LogViewer Screenshot Pipeline ===")
    print(f"Repository: {repo_root}")
    print(f"Mode: {'DRY RUN' if args.dry_run else ('COMMIT & PUSH' if args.push else 'COMMIT LOCAL')}\n")

    check_working_tree_clean(repo_root)

    temp_dir = Path(tempfile.mkdtemp(prefix="logviewer_screenshots_"))
    master_changed = False
    gh_pages_changed = False

    try:
        # Step 1: Generate screenshots into isolated temp folder
        generate_screenshots_to_temp(repo_root, temp_dir)

        # Step 2: Update master README screenshots
        master_changed = update_master_readme_images(repo_root, temp_dir, dry_run=args.dry_run)

        # Step 3: Switch to gh-pages and update website screenshots
        gh_pages_changed = update_gh_pages_images(repo_root, temp_dir, dry_run=args.dry_run)

        # Step 4: Always return to master
        print("[STEP 4/4] Returning to 'master' branch...")
        git(["checkout", "master"], repo_root)

        # Push if requested
        if args.push and not args.dry_run:
            push_branches(repo_root, master_changed, gh_pages_changed)

        print("\n=== Screenshot Pipeline Completed Successfully! ===")
        if not args.push and not args.dry_run:
            if master_changed or gh_pages_changed:
                print("Local commits created. Run with '--push' or push manually when ready:")
                print("  git push origin master")
                print("  git push origin gh-pages")
            else:
                print("All screenshots on 'master' and 'gh-pages' are already up to date.")

    finally:
        # Always return to master in case of unexpected abort, and clean up temp files
        try:
            current_branch = git(["rev-parse", "--abbrev-ref", "HEAD"], repo_root, check=False)
            if current_branch.stdout.strip() != "master":
                git(["checkout", "master"], repo_root, check=False)
        except Exception:
            pass

        if temp_dir.exists():
            shutil.rmtree(temp_dir, ignore_errors=True)


if __name__ == "__main__":
    main()
