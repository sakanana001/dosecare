#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Generate DoseCare adaptive launcher icon assets from the gradient logo JPG.

Uses numpy for fast per-pixel conversion (vs naive per-pixel loop).
"""
import os
from pathlib import Path
import numpy as np
from PIL import Image

# ===== CONFIG =====
SRC = Path(r"C:\Users\yuwen\Desktop\dosecare_logo_gradient.jpg")
PROJECT_RES = Path(r"C:\Users\yuwen\Desktop\精品神药\app\src\main\res")
OUTPUT_REF = Path(r"C:\Users\yuwen\Desktop\dosecare_icon_full.png")

DENSITIES = [
    ("mipmap-mdpi", 48),
    ("mipmap-hdpi", 72),
    ("mipmap-xhdpi", 96),
    ("mipmap-xxhdpi", 144),
    ("mipmap-xxxhdpi", 192),
]

# Adaptive icon foreground canvas
FG_CANVAS_PX = 432      # xxxhdpi = 108dp
FG_SAFE_AREA_PX = 264   # 66dp safe area (heart-pill fits here)


def make_foreground(src_img: Image.Image) -> Image.Image:
    """
    Take a 2048x2048 JPG with white-ish BG and heart-pill in center,
    return 432x432 RGBA with transparent BG and heart-pill in safe area.
    """
    # Convert to RGBA
    src_rgba = src_img.convert("RGBA")
    arr = np.array(src_rgba).astype(np.int16)  # H x W x 4

    # Find whiteness: min of R/G/B (low = colorful, high = white)
    whiteness = arr[:, :, :3].min(axis=2)  # H x W

    # Compute new alpha:
    #   - whiteness >= 245: alpha 0 (fully transparent BG)
    #   - 240 < whiteness < 245: partial alpha (soft edge anti-alias)
    #   - whiteness <= 240: alpha 255 (heart-pill, keep opaque)
    new_alpha = np.where(
        whiteness >= 245, 0,
        np.where(
            whiteness <= 240, 255,
            # Linear ramp 240..245 -> 255..0
            ((245 - whiteness) * 255 / 5).astype(np.int16)
        )
    ).astype(np.uint8)

    arr[:, :, 3] = new_alpha

    out = Image.fromarray(arr.astype(np.uint8), mode="RGBA")

    # Crop to bounding box of non-transparent content
    bbox = out.getbbox()
    print(f"  Heart-pill bbox: {bbox}")
    if bbox is None:
        raise ValueError("no non-transparent content after threshold")
    cropped = out.crop(bbox)
    print(f"  Cropped size: {cropped.size}")

    # Resize to safe-area size (264x264), preserve alpha
    heart = cropped.resize((FG_SAFE_AREA_PX, FG_SAFE_AREA_PX), Image.LANCZOS)

    # Center on 432x432 transparent canvas
    canvas = Image.new("RGBA", (FG_CANVAS_PX, FG_CANVAS_PX), (0, 0, 0, 0))
    x_off = (FG_CANVAS_PX - FG_SAFE_AREA_PX) // 2
    y_off = (FG_CANVAS_PX - FG_SAFE_AREA_PX) // 2
    canvas.paste(heart, (x_off, y_off), heart)  # use heart as alpha mask
    return canvas


def main():
    if not SRC.exists():
        print(f"ERROR: source not found: {SRC}")
        return

    print(f"Loading {SRC} ...")
    src_rgb = Image.open(SRC).convert("RGB")
    print(f"  Source size: {src_rgb.size}")

    # Save a copy of full-res for reference
    src_rgb.save(OUTPUT_REF, "PNG", optimize=True)
    print(f"  Saved full-res copy: {OUTPUT_REF}")

    # ----- 1) Generate foreground (heart-pill on transparent BG) -----
    print("\nGenerating adaptive icon foreground (transparent BG)...")
    fg = make_foreground(src_rgb)
    fg_path = PROJECT_RES / "drawable" / "ic_launcher_foreground.png"
    fg.save(fg_path, "PNG", optimize=True)
    print(f"  Saved: {fg_path} ({fg.size[0]}x{fg.size[1]})")

    # ----- 2) Generate 5-density full icons (with white BG) -----
    print("\nGenerating 5-density full launcher icons (with white BG)...")
    for dirname, size in DENSITIES:
        out_dir = PROJECT_RES / dirname
        out_dir.mkdir(parents=True, exist_ok=True)
        resized = src_rgb.resize((size, size), Image.LANCZOS)
        out_path = out_dir / "ic_launcher.png"
        resized.save(out_path, "PNG", optimize=True)
        print(f"  {dirname:18s} {size:>3}x{size:<3}  {out_path}")
        out_path_round = out_dir / "ic_launcher_round.png"
        resized.save(out_path_round, "PNG", optimize=True)

    # ----- 3) Create mipmap-anydpi-v26/ic_launcher.xml and ic_launcher_round.xml -----
    print("\nCreating adaptive icon XML files...")
    anydpi_dir = PROJECT_RES / "mipmap-anydpi-v26"
    anydpi_dir.mkdir(parents=True, exist_ok=True)

    adaptive_icon_xml = '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
'''
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        out = anydpi_dir / name
        out.write_text(adaptive_icon_xml, encoding="utf-8")
        print(f"  Saved: {out}")

    # ----- 4) Create drawable/ic_launcher_background.xml -----
    bg_path = PROJECT_RES / "drawable" / "ic_launcher_background.xml"
    bg_path.write_text('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#E3EEF7"/>
</shape>
''', encoding="utf-8")
    print(f"  Saved: {bg_path}")

    # ----- 5) Delete the old vector drawable -----
    old = PROJECT_RES / "drawable" / "ic_launcher.xml"
    if old.exists():
        old.unlink()
        print(f"\nRemoved old vector: {old}")

    print("\n[OK] All icon assets generated.")


if __name__ == "__main__":
    main()
