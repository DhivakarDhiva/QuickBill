# Authorship, Attribution, and Source Integrity

## Overview

This document details the authorship attribution and source integrity mechanisms implemented across the **QuickBill POS & QuickKitchen KDS** project.

The system is designed to provide clear author attribution, automated tamper detection, and verifiable cryptographic records of project-owned source files.

> **Honest Integrity Notice:**
> These mechanisms provide attribution and tamper detection. They cannot technically prevent a person with write access to the repository from deleting or modifying source files.
> What they provide is verifiable proof of authorship, tamper detection through cryptographic SHA-256 digests, and transparent change history tracked via Git.

---

## 1. Author Attribution

- **Original Author**: Dhivakar
- **Role**: Android Developer
- **Year**: 2026
- **Project**: QuickBill POS & QuickKitchen KDS (Single APK Dual-Mode Architecture)

Detailed contribution records are documented in [AUTHORS.md](file:///c:/Users/Admin/Documents/QuickBill/AUTHORS.md), and formal copyright notices and third-party acknowledgments are provided in [NOTICE.md](file:///c:/Users/Admin/Documents/QuickBill/NOTICE.md).

---

## 2. Standardized Source Headers

Every project-owned source file (`.kt`, `.kts`, `.xml`, and configuration scripts) includes a standardized header containing:

- Project identification (`QuickBill + QuickKitchen`)
- Author name and role (`Author: Dhivakar`, `Role: Android Developer`)
- Copyright notice (`Copyright (c) 2026 Dhivakar`)
- Explicit attribution statement
- Machine-verifiable marker: `QuickBill-QuickKitchen-Author: Dhivakar`

### Header Placement Safeguards:
- **Kotlin (`.kt`, `.kts`)**: Positioned at the very beginning of the file, cleanly preceding `package` declarations and import directives.
- **XML Resource & Manifest Files (`.xml`)**: Positioned immediately following the mandatory XML prolog (`<?xml version="1.0" encoding="utf-8"?>`) to ensure 100% compliance with Android AAPT2 and XML specifications.
- **Special & Binary Files**: Binary assets (images, webp, compiled apk, jar) and third-party wrapper files are excluded from comment modification to prevent data corruption.

---

## 3. Cryptographic SHA-256 Source Manifest

The repository maintains an authoritative file catalog in `SOURCE_MANIFEST.json`.

Each entry records:
```json
{
  "path": "app/src/main/java/com/quickbill/pos/MainActivity.kt",
  "sha256": "4f53...",
  "size": 28930
}
```

### Manifest Generation
To re-generate or update the source manifest after intentional updates:
```bash
python tools/generate_source_manifest.py
```
The generator script recursively scans all project-owned source files while strictly ignoring transient artifacts, `.git`, `.gradle`, build outputs, and third-party binaries.

---

## 4. Integrity and Authorship Verification Scripts

Two automated verification tools are provided under `tools/`:

### A. Authorship Verification (`tools/verify_authorship.py`)
Scans all project source files to confirm the presence of the unique marker `QuickBill-QuickKitchen-Author: Dhivakar`.
```bash
python tools/verify_authorship.py
```
- Returns exit code `0` when all project source files contain verified attribution.
- Returns exit code `1` if any attribution is removed or missing.

### B. Source Integrity Verification (`tools/verify_source_integrity.py`)
Recalculates the SHA-256 hash and file size for every entry in `SOURCE_MANIFEST.json`.
```bash
python tools/verify_source_integrity.py
```
- Detects modified source files.
- Detects deleted source files.
- Reports clear status per file (`✓` for intact files, warnings for altered or missing files).
- Returns exit code `0` on 100% integrity match; exits with non-zero code if any tampering or deletion is detected.

---

## 5. Git History Verification

In addition to SHA-256 file digests, Git commit history provides an immutable, chronological ledger of all commits authored by Dhivakar:

```bash
# Verify author commit log
git log --author="Dhivakar" --oneline

# Inspect cryptographic commit hashes
git log -n 5 --stat
```

---

## 6. Third-Party Software and Licenses

This project adheres to ethical open-source standards. Third-party dependencies (such as Android Jetpack, Kotlin Coroutines, Koin, Java-WebSocket, ZXing, and JUnit) remain subject to their respective Apache 2.0, MIT, and EPL licenses. Factual attribution is claimed solely for the original implementation and project-specific components created by Dhivakar.
