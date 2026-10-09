# Authorship & Source Integrity Implementation Report

**Project**: QuickBill POS & QuickKitchen KDS  
**Author**: Dhivakar (Android Developer)  
**Year**: 2026  
**Date**: October 9, 2026  

---

## 1. Number of Source Files Inspected
- **Applicable Source Files Inspected for Attribution**: 104 files
  - Kotlin Source Files (`.kt`): 92 files (85 `app/src/main/java` + 7 `app/src/test/java`)
  - Gradle Kotlin Scripts (`.kts`): 3 files (`build.gradle.kts`, `app/build.gradle.kts`, `settings.gradle.kts`)
  - XML Application & Resource Files (`.xml`): 8 files (`AndroidManifest.xml`, vector drawables, mipmap declarations, `strings.xml`, `themes.xml`, `file_paths.xml`)
  - Proguard / R8 Optimization Rules (`.pro`): 1 file (`app/proguard-rules.pro`)

## 2. Number of Files Containing Attribution
- **Attributed Files**: 104 / 104 applicable source files (100% coverage)
- **Attribution Marker**: `QuickBill-QuickKitchen-Author: Dhivakar`
- All applicable files have been verified by `tools/verify_authorship.py`.

## 3. Files Intentionally Excluded and Why
- **`gradle/libs.versions.toml`**: TOML format does not support C-style block comments or XML comments. Forcing comments could break TOML version catalog parsers.
- **`SOURCE_MANIFEST.json`**: JSON specification (RFC 8259) does not support comments. Additionally, the manifest must not contain its own hash to prevent circular hashing dependencies.
- **Binary Assets**:
  - `apk/app-release.apk`
  - Screenshots in `screenshots/*.png`
  - Mipmap launcher icons in `app/src/main/res/mipmap-*/` (`.webp`)
  - Gradle wrapper binary `gradle/wrapper/gradle-wrapper.jar`
  *Reason*: Binary files cannot receive textual comments without file corruption. Note: unused asset `kitchen_splash_bg.jpg` and dead component `QuickKitchenSplashScreen.kt` were removed.
- **Third-Party Wrapper Scripts (`gradlew`, `gradlew.bat`)**: Third-party Gradle wrapper distribution files owned by Gradle Inc.
- **Build Outputs & IDE Caches**:
  - `.git/` (VCS metadata)
  - `.gradle/` (Gradle cache)
  - `.idea/` (Android Studio workspace cache)
  - `.kotlin/` (Kotlin incremental compilation cache)
  - `build/`, `app/build/` (Transient build outputs)
  - `local.properties` (Machine-specific SDK paths)

## 4. Manifest Location
- **Location**: [`SOURCE_MANIFEST.json`](file:///c:/Users/Admin/Documents/QuickBill/SOURCE_MANIFEST.json) (Repository root)
- **Format**: JSON array of file entries with `path`, `sha256`, and `size`.
- **Generation Script**: [`tools/generate_source_manifest.py`](file:///c:/Users/Admin/Documents/QuickBill/tools/generate_source_manifest.py)

## 5. Verification Script Locations
- **Authorship Verification**: [`tools/verify_authorship.py`](file:///c:/Users/Admin/Documents/QuickBill/tools/verify_authorship.py)
  - Scans applicable source files for `QuickBill-QuickKitchen-Author: Dhivakar`.
  - Exits with status `0` on success, `1` on missing attribution.
- **Source Integrity Verification**: [`tools/verify_source_integrity.py`](file:///c:/Users/Admin/Documents/QuickBill/tools/verify_source_integrity.py)
  - Validates all tracked files against `SOURCE_MANIFEST.json`.
  - Detects modified, deleted, and untracked files.
  - Exits with status `0` on success, `1` on mismatch.

## 6. Signature File Location
- **Location**: [`AUTHOR_SIGNATURE.txt`](file:///c:/Users/Admin/Documents/QuickBill/AUTHOR_SIGNATURE.txt) (Repository root)
- Notes: Accurately references the SHA-256 manifest and verification tools without claiming "undeletability".

## 7. CI Configuration
- **Location**: [`.github/workflows/verify-authorship.yml`](file:///c:/Users/Admin/Documents/QuickBill/.github/workflows/verify-authorship.yml)
- **Triggers**: Pushes and Pull Requests to `main` and `master`.
- **Actions**:
  1. Executes `python tools/verify_authorship.py` to prevent attribution removal.
  2. Executes `python tools/verify_source_integrity.py` to catch unauthorized file modifications or deletions.

## 8. Any Files That Could Not Safely Receive an Author Header
- `gradle/libs.versions.toml` (TOML format)
- `SOURCE_MANIFEST.json` (JSON standard)
- Binary images (`.webp`, `.png`, `.jpg`) and compiled artifacts (`.apk`, `.jar`)
- All XML files received comments safely placed **after** the `<?xml ...?>` prolog to preserve AAPT2 XML validation rules.

## 9. Any Existing Copyright / License Notices That Must Not Be Changed
- The repository's [README.md](file:///c:/Users/Admin/Documents/QuickBill/README.md) MIT license mention was preserved.
- [NOTICE.md](file:///c:/Users/Admin/Documents/QuickBill/NOTICE.md) formally documents and attributes third-party licenses for Android Jetpack, Kotlin Coroutines, Koin, Java-WebSocket, ZXing, and JUnit.

## 10. Any Conflicts With Existing Licenses
- None. Dhivakar's attribution notice strictly applies to original implementation and project-specific contributions created for QuickBill POS and QuickKitchen KDS. No proprietary claims are made over third-party open-source components.
