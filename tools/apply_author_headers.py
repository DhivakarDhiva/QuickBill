#!/usr/bin/env python3
"""
QuickBill + QuickKitchen - Author Header Application Script
Author: Dhivakar
Year: 2026

Safely applies standardized attribution headers to project-owned source files
without breaking XML declarations, package statements, or syntax.
"""

import os
import sys

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

KOTLIN_HEADER = """/*
 * QuickBill + QuickKitchen
 *
 * Author: Dhivakar
 * Role: Android Developer
 *
 * Copyright (c) 2026 Dhivakar
 *
 * This file is part of the QuickBill + QuickKitchen project.
 * The original implementation and modifications in this file were
 * created by Dhivakar for the project/assignment.
 *
 * QuickBill-QuickKitchen-Author: Dhivakar
 *
 * Do not remove or alter this attribution notice.
 */
"""

XML_HEADER = """<!--
  QuickBill + QuickKitchen

  Author: Dhivakar
  Role: Android Developer

  Copyright (c) 2026 Dhivakar

  This file is part of the QuickBill + QuickKitchen project.
  The original implementation and modifications in this file were
  created by Dhivakar for the project/assignment.

  QuickBill-QuickKitchen-Author: Dhivakar

  Do not remove or alter this attribution notice.
-->
"""

PROGUARD_HEADER = """# QuickBill + QuickKitchen
#
# Author: Dhivakar
# Role: Android Developer
#
# Copyright (c) 2026 Dhivakar
#
# This file is part of the QuickBill + QuickKitchen project.
# The original implementation and modifications in this file were
# created by Dhivakar for the project/assignment.
#
# QuickBill-QuickKitchen-Author: Dhivakar
#
# Do not remove or alter this attribution notice.
"""

MARKER = "QuickBill-QuickKitchen-Author: Dhivakar"


def apply_header_to_kotlin(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    if MARKER in content:
        return False, "Already attributed"

    # Kotlin / Kts: place header at the very top
    # If the file had a leading comment like // Top-level build file..., prepend our header cleanly
    new_content = KOTLIN_HEADER + "\n" + content
    with open(filepath, "w", encoding="utf-8", newline="\n") as f:
        f.write(new_content)
    return True, "Header added"


def apply_header_to_xml(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    if MARKER in content:
        return False, "Already attributed"

    # XML standard requires <?xml ...?> to be at character 0 if present.
    # Put header immediately after XML prologue if present.
    if content.startswith("<?xml"):
        prologue_end = content.find("?>")
        if prologue_end != -1:
            end_idx = prologue_end + 2
            # Check for trailing newline
            if end_idx < len(content) and content[end_idx] == "\r":
                end_idx += 1
            if end_idx < len(content) and content[end_idx] == "\n":
                end_idx += 1
            prologue = content[:end_idx]
            rest = content[end_idx:]
            new_content = prologue + "\n" + XML_HEADER + "\n" + rest
        else:
            new_content = XML_HEADER + "\n" + content
    else:
        new_content = XML_HEADER + "\n" + content

    with open(filepath, "w", encoding="utf-8", newline="\n") as f:
        f.write(new_content)
    return True, "Header added"


def apply_header_to_proguard(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    if MARKER in content:
        return False, "Already attributed"

    new_content = PROGUARD_HEADER + "\n\n" + content
    with open(filepath, "w", encoding="utf-8", newline="\n") as f:
        f.write(new_content)
    return True, "Header added"


def main():
    root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    print(f"Applying author headers in: {root_dir}")

    total_processed = 0
    total_added = 0

    # 1. Kotlin source files (.kt)
    for root, dirs, files in os.walk(os.path.join(root_dir, "app", "src")):
        for file in files:
            if file.endswith(".kt"):
                path = os.path.join(root, file)
                rel = os.path.relpath(path, root_dir).replace("\\", "/")
                added, msg = apply_header_to_kotlin(path)
                total_processed += 1
                if added:
                    total_added += 1
                    print(f"✓ Added (.kt): {rel}")

    # 2. Gradle KTS files
    kts_candidates = [
        os.path.join(root_dir, "build.gradle.kts"),
        os.path.join(root_dir, "app", "build.gradle.kts"),
        os.path.join(root_dir, "settings.gradle.kts"),
    ]
    for path in kts_candidates:
        if os.path.isfile(path):
            rel = os.path.relpath(path, root_dir).replace("\\", "/")
            added, msg = apply_header_to_kotlin(path)
            total_processed += 1
            if added:
                total_added += 1
                print(f"✓ Added (.kts): {rel}")

    # 3. XML files
    xml_dir = os.path.join(root_dir, "app", "src", "main")
    for root, dirs, files in os.walk(xml_dir):
        for file in files:
            if file.endswith(".xml"):
                path = os.path.join(root, file)
                rel = os.path.relpath(path, root_dir).replace("\\", "/")
                added, msg = apply_header_to_xml(path)
                total_processed += 1
                if added:
                    total_added += 1
                    print(f"✓ Added (.xml): {rel}")

    # 4. Proguard rules
    proguard_path = os.path.join(root_dir, "app", "proguard-rules.pro")
    if os.path.isfile(proguard_path):
        rel = os.path.relpath(proguard_path, root_dir).replace("\\", "/")
        added, msg = apply_header_to_proguard(proguard_path)
        total_processed += 1
        if added:
            total_added += 1
            print(f"✓ Added (proguard): {rel}")

    print(f"\nDone. Processed {total_processed} files, added headers to {total_added} files.")


if __name__ == "__main__":
    main()
