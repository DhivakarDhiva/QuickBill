#!/usr/bin/env python3
"""
QuickBill + QuickKitchen - Authorship Verification Script
Author: Dhivakar
Year: 2026

Checks that applicable project-owned source files contain the required
attribution marker: 'QuickBill-QuickKitchen-Author: Dhivakar'.
Exits with code 0 on success, or code 1 if any required attribution is missing.
"""

import os
import sys

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

MARKER = "QuickBill-QuickKitchen-Author: Dhivakar"

IGNORED_DIRS = {
    ".git",
    ".gradle",
    ".idea",
    ".kotlin",
    "build",
    "gradle",
    ".system_generated",
    ".tempmediaStorage",
    ".user_uploaded",
    "scratch",
}

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


def should_check_file(rel_path):
    parts = rel_path.replace("\\", "/").split("/")
    filename = parts[-1]

    # Check ignored directories
    for part in parts[:-1]:
        if part in IGNORED_DIRS or part.startswith("."):
            return False

    # Check applicable source extensions
    _, ext = os.path.splitext(filename)
    applicable_exts = {".kt", ".kts", ".xml", ".pro"}
    return ext.lower() in applicable_exts


def verify_authorship():
    print("=" * 60)
    print("AUTHORSHIP ATTRIBUTION VERIFICATION")
    print(f"Required Marker: '{MARKER}'")
    print("=" * 60)

    present_files = []
    missing_files = []

    for root, dirs, files in os.walk(PROJECT_ROOT):
        dirs[:] = [d for d in dirs if d not in IGNORED_DIRS and not d.startswith(".")]

        for file in files:
            full_path = os.path.join(root, file)
            rel_path = os.path.relpath(full_path, PROJECT_ROOT).replace("\\", "/")

            if should_check_file(rel_path):
                try:
                    with open(full_path, "r", encoding="utf-8", errors="ignore") as f:
                        content = f.read()

                    if MARKER in content:
                        present_files.append(rel_path)
                        print(f"✓ Attribution present: {rel_path}")
                    else:
                        missing_files.append(rel_path)
                        print(f"✗ Attribution missing: {rel_path}")
                except Exception as e:
                    missing_files.append(rel_path)
                    print(f"✗ Attribution missing (read error): {rel_path} ({e})")

    print("\n" + "-" * 60)
    print(f"Total files checked: {len(present_files) + len(missing_files)}")
    print(f"Attribution present: {len(present_files)}")
    print(f"Attribution missing: {len(missing_files)}")
    print("-" * 60)

    if missing_files:
        print("\n[FAILED] One or more project source files are missing authorship attribution:")
        for path in missing_files:
            print(f"  - {path}")
        sys.exit(1)
    else:
        print("\n[PASSED] All applicable project source files contain verified author attribution.")
        sys.exit(0)


if __name__ == "__main__":
    verify_authorship()
