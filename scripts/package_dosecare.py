#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Package DoseCare Android project for GitHub upload.

Excludes:
  - build artifacts (.gradle/, build/, *.apk, *.aab)
  - logs (*.log, gradle-v0X*.log, etc.)
  - IDE files (.idea/, *.iml)
  - secrets (local.properties, *.jks)
  - temp/backup files (*.bak, *.tmp, .DS_Store)
  - dev-only test screenshots (app/src/main/assets/test_*.png)
  - agent workspace (.minimax/)

Includes:
  - All source code (app/src/main/java/, app/src/test/, prototype/)
  - All docs (docs/, README.md, CHANGELOG.md, CONTRIBUTING.md, SECURITY.md, LICENSE)
  - All build configs (build.gradle.kts, settings.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradle/wrapper/)
  - Data generation scripts (scripts/*.py — non-log only)
  - Built-in drug catalog JSON (app/src/main/assets/drugs/v0.6.json)
  - AndroidManifest.xml + res/
"""
import os
import sys
import zipfile
import fnmatch
from pathlib import Path
from datetime import datetime

# ===== CONFIG =====
PROJECT_ROOT = Path(r"C:\Users\yuwen\Desktop\精品神药")
OUTPUT_ZIP = Path(r"C:\Users\yuwen\Desktop\DoseCare-v0.8b-github.zip")

# Patterns to exclude (matched against relative path or any path component)
EXCLUDE_DIR_PATTERNS = [
    ".gradle",
    ".git",  # git history should not be in the source zip
    "build",
    ".kotlin",
    ".idea",
    "captures",
    ".cxx",
    ".externalNativeBuild",
    ".navigation",
    ".vscode",
    "__pycache__",
    ".minimax",
    "node_modules",
    "dist",  # build artifact (re-zip output goes here, but we don't want it inside the zip)
    "docs/ui-dumps",  # debug XML only, not useful for repo readers
]

EXCLUDE_FILE_PATTERNS = [
    # Build artifacts
    "*.apk", "*.ap_", "*.aab", "*.aar", "*.dex", "*.class", "*.hprof",
    # Backups / temp
    "*.bak", "*.bak.*", "*.tmp", "*.swp", "*.swo", "*~", "*_orig.*",
    # IDE
    "*.iml", ".DS_Store", "Thumbs.db", "desktop.ini",
    # Secrets
    "local.properties", "*.jks", "*.keystore", "*.env",
    # Logs (catch-all)
    "*.log",
    # Specific dev-time log files
    "gradle-*.log", "junit-*.log", "lorazepam-*.log", "compile-*.log",
    "run-*.log", "rule-*.log", "help.txt", "progress.html",
    # Dev-time build/test scripts that should not be in source
    "compile-*.bat", "compile-*.sh",
    "lorazepam-*.ps1", "run-*.ps1", "run_tests.ps1",
    # KSP/build cache
    "*.classpath", "*.project", "*.settings", ".factorypath",
    # Python
    "*.pyc", "*.pyo",
    # Old v0.6 test screenshots (UI has been refactored 5-tab → 3-tab)
    "test_*.png", "test_screenshot.png",
    # Agent workspace marker
    ".DS_Store",
]

EXCLUDE_EXTRA_FILES = set([
    "gradle-v0*.log",  # explicitly catch v05, v06, v07, v08 series
])


def should_exclude(rel_path: Path) -> bool:
    """Check if a relative path should be excluded."""
    parts = rel_path.parts
    # Check dir components
    for part in parts[:-1]:
        for pat in EXCLUDE_DIR_PATTERNS:
            if fnmatch.fnmatch(part, pat):
                return True
    # Check file name
    name = rel_path.name
    for pat in EXCLUDE_FILE_PATTERNS:
        if fnmatch.fnmatch(name, pat):
            return True
    # Check extra files (full relative path match)
    rel_str = str(rel_path).replace("\\", "/")
    for pat in EXCLUDE_EXTRA_FILES:
        if fnmatch.fnmatch(rel_str, pat):
            return True
    return False


def main():
    if not PROJECT_ROOT.exists():
        print(f"ERROR: project not found: {PROJECT_ROOT}", file=sys.stderr)
        sys.exit(1)

    # Sanity: verify it's the DoseCare project
    if not (PROJECT_ROOT / "build.gradle.kts").exists():
        print(f"ERROR: {PROJECT_ROOT} is not a Gradle project (no build.gradle.kts)", file=sys.stderr)
        sys.exit(1)

    # Collect files
    files_to_zip = []
    total_size = 0
    excluded_count = 0
    excluded_size = 0

    for root, dirs, files in os.walk(PROJECT_ROOT):
        # Prune excluded dirs in-place (so os.walk skips them)
        dirs[:] = [d for d in dirs if d not in EXCLUDE_DIR_PATTERNS]

        for fname in files:
            full_path = Path(root) / fname
            try:
                rel_path = full_path.relative_to(PROJECT_ROOT)
            except ValueError:
                continue

            if should_exclude(rel_path):
                excluded_count += 1
                try:
                    excluded_size += full_path.stat().st_size
                except OSError:
                    pass
                continue

            try:
                size = full_path.stat().st_size
            except OSError:
                continue
            files_to_zip.append((full_path, rel_path, size))
            total_size += size

    # Sort: by relative path for deterministic zip
    files_to_zip.sort(key=lambda x: str(x[1]).replace("\\", "/").lower())

    print(f"Project root: {PROJECT_ROOT}")
    print(f"Output zip:   {OUTPUT_ZIP}")
    print(f"Files to zip: {len(files_to_zip)}")
    print(f"Total size:   {total_size / 1024 / 1024:.2f} MB")
    print(f"Excluded:     {excluded_count} files ({excluded_size / 1024 / 1024:.2f} MB)")
    print()

    # Show top-level structure that will be in zip
    top_level = sorted({str(rel.parts[0]) for _, rel, _ in files_to_zip})
    print("Top-level entries in zip:")
    for entry in top_level:
        cnt = sum(1 for _, rel, _ in files_to_zip if str(rel.parts[0]) == entry)
        size = sum(s for _, rel, s in files_to_zip if str(rel.parts[0]) == entry)
        print(f"  {entry:<30} {cnt:>5} files  {size / 1024 / 1024:>7.2f} MB")
    print()

    # Write zip
    OUTPUT_ZIP.parent.mkdir(parents=True, exist_ok=True)
    if OUTPUT_ZIP.exists():
        OUTPUT_ZIP.unlink()
        print(f"Removed existing {OUTPUT_ZIP.name}")

    with zipfile.ZipFile(OUTPUT_ZIP, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
        for full_path, rel_path, size in files_to_zip:
            # Use forward slashes for cross-platform compatibility (GitHub expects this)
            arcname = str(rel_path).replace("\\", "/")
            zf.write(full_path, arcname)

    final_size = OUTPUT_ZIP.stat().st_size
    print(f"\n[OK] Created: {OUTPUT_ZIP}")
    print(f"  Zip size: {final_size / 1024 / 1024:.2f} MB")
    print(f"  Compression: {(1 - final_size / total_size) * 100:.1f}%")


if __name__ == "__main__":
    main()
