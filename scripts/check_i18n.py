#!/usr/bin/env python3
"""
I18n Check & Extraction Tool for LogViewer.

Usage:
  # Check only added/modified lines in git diff against a base branch:
  python3 scripts/check_i18n.py --diff origin/master

  # Scan files or directories for existing hardcoded UI strings:
  python3 scripts/check_i18n.py --scan src/main

  # Validate that all constants in I18n.java exist in strings.properties:
  python3 scripts/check_i18n.py --validate-bundle
"""

import argparse
import os
import re
import subprocess
import sys

# Patterns that indicate UI text being passed as a literal string
UI_PATTERNS = [
    # .setText("...")
    re.compile(r'\.setText\s*\(\s*"([^"]+)"\s*\)'),
    # .setTitle("...")
    re.compile(r'\.setTitle\s*\(\s*"([^"]+)"\s*\)'),
    # dialogTitle = "..." (Kotlin property)
    re.compile(r'dialogTitle\s*=\s*"([^"]+)"'),
    # new JLabel("..."), new JButton("..."), new JMenuItem("..."), etc.
    re.compile(r'new\s+(?:JLabel|JButton|JMenuItem|JCheckBox|JRadioButton|JMenu|JTabbedPane)\s*\(\s*"([^"]+)"'),
    # new TitledBorder("...") or new TitledBorder(..., "...")
    re.compile(r'new\s+TitledBorder\s*\([^)]*?"([^"]+)"'),
    # JOptionPane.show*(..., "...")
    re.compile(r'JOptionPane\.(?:showMessageDialog|showConfirmDialog|showInputDialog|showOptionDialog)\s*\([^)]*?"([^"]+)"'),
    # ProgressDialog.showProgressDialog(..., "...")
    re.compile(r'ProgressDialog\.showProgressDialog\s*\([^)]*?"([^"]+)"'),
]

# Text values that are exempt from i18n
EXEMPT_VALUES = {"...", "", " ", "\n", "\t", "x", "X", "+", "-"}


def is_exempt(text: str, line: str) -> bool:
    if text.strip() in EXEMPT_VALUES:
        return True
    if "// i18n:ignore" in line or "// no-i18n" in line:
        return True
    # Ignore comment lines
    trimmed = line.strip()
    if trimmed.startswith("//") or trimmed.startswith("*") or trimmed.startswith("/*"):
        return True
    return False


def find_hardcoded_in_line(line: str):
    matches = []
    for pattern in UI_PATTERNS:
        for match in pattern.finditer(line):
            text = match.group(1)
            if not is_exempt(text, line):
                matches.append(text)
    return matches


def scan_file(file_path: str):
    violations = []
    with open(file_path, "r", encoding="utf-8", errors="replace") as f:
        for line_num, line in enumerate(f, 1):
            matches = find_hardcoded_in_line(line)
            for match in matches:
                violations.append((file_path, line_num, match, line.strip()))
    return violations


def scan_path(target_path: str):
    violations = []
    if os.path.isfile(target_path):
        violations.extend(scan_file(target_path))
    else:
        for root, _, files in os.walk(target_path):
            if "src/test" in root or "/test/" in root:
                continue
            for file in files:
                if file.endswith((".java", ".kt")):
                    violations.extend(scan_file(os.path.join(root, file)))
    return violations


def check_git_diff(base_ref: str):
    cmd = ["git", "diff", "-U0", f"{base_ref}...HEAD"]
    try:
        diff_output = subprocess.check_output(cmd, stderr=subprocess.STDOUT).decode("utf-8")
    except subprocess.CalledProcessError as e:
        # Fallback to direct diff if merge-base syntax fails
        cmd = ["git", "diff", "-U0", base_ref]
        diff_output = subprocess.check_output(cmd).decode("utf-8")

    current_file = None
    violations = []
    current_line_num = 0

    for line in diff_output.splitlines():
        if line.startswith("+++ b/"):
            current_file = line[6:]
        elif line.startswith("@@ "):
            # Format: @@ -old,count +new,count @@
            match = re.search(r'\+(\d+)', line)
            if match:
                current_line_num = int(match.group(1)) - 1
        elif line.startswith("+") and not line.startswith("+++"):
            current_line_num += 1
            if current_file and (current_file.endswith(".java") or current_file.endswith(".kt")):
                if "src/test" in current_file:
                    continue
                content = line[1:]  # strip leading '+'
                matches = find_hardcoded_in_line(content)
                for match in matches:
                    violations.append((current_file, current_line_num, match, content.strip()))

    return violations


def validate_bundle():
    i18n_path = "src/main/java/com/tibagni/logviewer/i18n/I18n.java"
    props_path = "src/main/resources/properties/strings.properties"

    if not os.path.exists(i18n_path) or not os.path.exists(props_path):
        print(f"Error: Missing {i18n_path} or {props_path}")
        return False

    i18n_keys = {}
    key_pattern = re.compile(r'public\s+static\s+final\s+String\s+(\w+)\s*=\s*"([^"]+)";')
    with open(i18n_path, "r", encoding="utf-8") as f:
        for line_num, line in enumerate(f, 1):
            m = key_pattern.search(line)
            if m:
                const_name, key_val = m.groups()
                i18n_keys[key_val] = (const_name, line_num)

    prop_keys = set()
    with open(props_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k = line.split("=", 1)[0].strip()
                prop_keys.add(k)

    missing = set(i18n_keys.keys()) - prop_keys
    unused = prop_keys - set(i18n_keys.keys())

    has_err = False
    if missing:
        has_err = True
        print("❌ Keys defined in I18n.java but MISSING in strings.properties:")
        for k in sorted(missing):
            const_name, line_num = i18n_keys[k]
            print(f"  - {const_name} = \"{k}\" (I18n.java:{line_num})")

    if unused:
        print("⚠️  Keys in strings.properties without constants in I18n.java:")
        for k in sorted(unused):
            print(f"  - {k}")

    if not has_err:
        print(f"✅ All {len(i18n_keys)} I18n keys are present in strings.properties.")

    return not has_err


def main():
    parser = argparse.ArgumentParser(description="LogViewer I18n Checker and Scanner")
    parser.add_argument("--diff", metavar="BASE_REF", help="Check only added/modified lines in git diff against BASE_REF")
    parser.add_argument("--scan", metavar="PATH", nargs="?", const="src/main", help="Scan a path for hardcoded UI strings")
    parser.add_argument("--validate-bundle", action="store_true", help="Validate I18n.java keys against strings.properties")
    args = parser.parse_args()

    if args.validate_bundle:
        ok = validate_bundle()
        sys.exit(0 if ok else 1)

    if args.diff:
        violations = check_git_diff(args.diff)
        if violations:
            print(f"❌ Found {len(violations)} hardcoded UI string(s) in git diff against {args.diff}:")
            for fpath, lnum, text, code in violations:
                print(f"  {fpath}:{lnum}: \"{text}\"")
                print(f"    Line: {code}")
                print(f"    Fix: Use I18n.get(...) or add // i18n:ignore if not user-facing\n")
            sys.exit(1)
        else:
            print(f"✅ No hardcoded UI strings introduced in git diff against {args.diff}.")
            sys.exit(0)

    if args.scan:
        violations = scan_path(args.scan)
        if violations:
            print(f"Found {len(violations)} hardcoded UI strings in {args.scan}:")
            for fpath, lnum, text, code in violations:
                print(f"  {fpath}:{lnum}: \"{text}\" -> {code}")
        else:
            print(f"No hardcoded UI strings found in {args.scan}.")
        sys.exit(0)

    parser.print_help()


if __name__ == "__main__":
    main()
