#!/usr/bin/env python3
"""
QuickBill + QuickKitchen - Source Integrity Manifest Generator
Author: Dhivakar
Year: 2026

Recursively inspects project-owned source files, computes SHA-256 hashes
and file sizes, and generates SOURCE_MANIFEST.json.
"""

import hashlib
import json
import os
import sys

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

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

IGNORED_EXTENSIONS = {
    ".apk",
    ".png",
    ".jpg",
    ".jpeg",
    ".webp",
    ".jar",
    ".ico",
    ".class",
    ".bin",
}

IGNORED_FILES = {
    "SOURCE_MANIFEST.json",
    "gradlew",
    "gradlew.bat",
    "local.properties",
}

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


def compute_sha256(filepath):
    hasher = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            hasher.update(chunk)
    return hasher.hexdigest()


def is_project_owned_file(rel_path):
    parts = rel_path.replace("\\", "/").split("/")
    filename = parts[-1]

    # Check ignored directories
    for part in parts[:-1]:
        if part in IGNORED_DIRS or part.startswith("."):
            return False

    # Check ignored file names
    if filename in IGNORED_FILES or filename.startswith("."):
        return False

    # Check ignored extensions
    _, ext = os.path.splitext(filename)
    if ext.lower() in IGNORED_EXTENSIONS:
        return False

    # Include project source and documentation files
    allowed_exts = {
        ".kt",
        ".kts",
        ".xml",
        ".pro",
        ".properties",
        ".md",
        ".txt",
        ".py",
        ".yml",
        ".yaml",
    }

    return ext.lower() in allowed_exts


def generate_manifest():
    print(f"Scanning project files in: {PROJECT_ROOT}")
    manifest_entries = []

    for root, dirs, files in os.walk(PROJECT_ROOT):
        # Prune ignored directories in-place
        dirs[:] = [d for d in dirs if d not in IGNORED_DIRS and not d.startswith(".")]

        for file in files:
            full_path = os.path.join(root, file)
            rel_path = os.path.relpath(full_path, PROJECT_ROOT).replace("\\", "/")

            if is_project_owned_file(rel_path):
                sha256 = compute_sha256(full_path)
                size = os.path.getsize(full_path)
                manifest_entries.append({
                    "path": rel_path,
                    "sha256": sha256,
                    "size": size
                })

    # Sort entries by path for deterministic manifest output
    manifest_entries.sort(key=lambda x: x["path"])

    manifest_path = os.path.join(PROJECT_ROOT, "SOURCE_MANIFEST.json")
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest_entries, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(f"Successfully generated SOURCE_MANIFEST.json with {len(manifest_entries)} source files.")
    return manifest_entries


if __name__ == "__main__":
    generate_manifest()
