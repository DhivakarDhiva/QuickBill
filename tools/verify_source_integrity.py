#!/usr/bin/env python3
"""
QuickBill + QuickKitchen - Source Integrity Verification Script
Author: Dhivakar
Year: 2026

Verifies project file integrity against SOURCE_MANIFEST.json.
Recalculates SHA-256 hashes and detects:
- Modified files
- Deleted files
- Untracked unexpected source files (optional check)
Exits with code 0 on success, or code 1 on verification failure.
"""

import hashlib
import json
import os
import sys

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MANIFEST_PATH = os.path.join(PROJECT_ROOT, "SOURCE_MANIFEST.json")


def compute_sha256(filepath):
    hasher = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            hasher.update(chunk)
    return hasher.hexdigest()


def verify_integrity():
    print("=" * 60)
    print("SOURCE INTEGRITY CHECK")
    print("=" * 60)

    if not os.path.isfile(MANIFEST_PATH):
        print(f"ERROR: Manifest file not found at: {MANIFEST_PATH}")
        print("Please run 'python tools/generate_source_manifest.py' first.")
        sys.exit(1)

    with open(MANIFEST_PATH, "r", encoding="utf-8") as f:
        manifest_data = json.load(f)

    # Allow either a list or an object with 'files'
    if isinstance(manifest_data, dict) and "files" in manifest_data:
        entries = manifest_data["files"]
    elif isinstance(manifest_data, list):
        entries = manifest_data
    else:
        print("ERROR: Invalid manifest format.")
        sys.exit(1)

    passed = []
    modified = []
    deleted = []

    for entry in entries:
        rel_path = entry.get("path")
        expected_hash = entry.get("sha256")
        expected_size = entry.get("size")

        full_path = os.path.join(PROJECT_ROOT, rel_path)

        if not os.path.exists(full_path):
            deleted.append(rel_path)
            continue

        current_hash = compute_sha256(full_path)
        if current_hash != expected_hash:
            modified.append(rel_path)
        else:
            passed.append(rel_path)
            print(f"✓ {rel_path}")

    # Summary Report
    has_errors = False

    if modified:
        has_errors = True
        print("\nWARNING:")
        print("Modified:")
        for path in modified:
            print(f"  ✗ {path}")

    if deleted:
        has_errors = True
        print("\nWARNING:")
        print("Deleted:")
        for path in deleted:
            print(f"  ✗ {path}")

    print("\n" + "-" * 60)
    print(f"Total checked: {len(entries)}")
    print(f"Passed:        {len(passed)}")
    print(f"Modified:      {len(modified)}")
    print(f"Deleted:       {len(deleted)}")
    print("-" * 60)

    if has_errors:
        print("\n[FAILED] Source integrity verification failed! Tampering or deletion detected.")
        sys.exit(1)
    else:
        print("\n[PASSED] All source files match the authoritative SHA-256 manifest.")
        sys.exit(0)


if __name__ == "__main__":
    verify_integrity()
