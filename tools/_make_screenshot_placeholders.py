"""Generate placeholder screenshot SVGs for the PicoHRT site.

The placeholders are minimal but recognisably phone-shaped, so the
screenshot carousel has something to show until real PNGs are added
under site/public/screenshots/{en,zh-Hans}-{light,dark}/.
"""
import os
import textwrap

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "site", "public", "screenshots")

LABELS = {
    "01-home": "Home",
    "02-plan": "Plan",
    "03-history": "History",
    "04-editor": "Editor",
    "05-manual": "Manual",
    "06-calibration": "Calibration",
    "07-medicines": "Medicines",
    "08-journal": "Journal",
}

BG = {
    "light": ("#ffffff", "#0b0c10", "#eef0ff", "#5b6cff"),
    "dark":  ("#0e0f12", "#e9ecef", "#1f2230", "#7c8cff"),
}

def make_svg(file_stem: str, theme: str) -> str:
    label = LABELS.get(file_stem, file_stem)
    bg, fg, accent_soft, accent = BG[theme]
    return textwrap.dedent(f"""\
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 480" width="240" height="480">
          <defs>
            <linearGradient id="bar" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stop-color="{accent}" stop-opacity="0.0"/>
              <stop offset="20%" stop-color="{accent}" stop-opacity="0.9"/>
              <stop offset="80%" stop-color="{accent}" stop-opacity="0.9"/>
              <stop offset="100%" stop-color="{accent}" stop-opacity="0.0"/>
            </linearGradient>
          </defs>
          <rect width="240" height="480" fill="{bg}"/>
          <rect x="14" y="14" width="212" height="452" rx="20" fill="none" stroke="{accent}" stroke-width="1.2" opacity="0.45"/>
          <rect x="24" y="32" width="80" height="14" rx="6" fill="{accent_soft}"/>
          <rect x="24" y="58" width="160" height="10" rx="5" fill="{fg}" opacity="0.18"/>
          <rect x="24" y="78" width="192" height="120" rx="14" fill="{accent_soft}"/>
          <text x="120" y="142" text-anchor="middle" font-family="-apple-system,Segoe UI,sans-serif" font-size="22" font-weight="600" fill="{fg}" opacity="0.55">{label}</text>
          <rect x="24" y="78" width="192" height="120" rx="14" fill="url(#bar)" opacity="0.55"/>
          <rect x="24" y="214" width="192" height="40" rx="10" fill="{fg}" opacity="0.10"/>
          <rect x="24" y="266" width="192" height="40" rx="10" fill="{fg}" opacity="0.10"/>
          <rect x="24" y="318" width="192" height="40" rx="10" fill="{fg}" opacity="0.10"/>
          <rect x="24" y="394" width="192" height="56" rx="14" fill="{accent}"/>
          <text x="120" y="428" text-anchor="middle" font-family="-apple-system,Segoe UI,sans-serif" font-size="18" font-weight="600" fill="{bg}">PicoHRT</text>
          <text x="120" y="460" text-anchor="middle" font-family="-apple-system,Segoe UI,sans-serif" font-size="9" fill="{fg}" opacity="0.5">screenshot placeholder · {theme}</text>
        </svg>
    """)


def main():
    written = 0
    for theme in ("light", "dark"):
        for stem in LABELS:
            for lang in ("en", "zh-Hans"):
                rel = os.path.join(OUT, f"{lang}-{theme}", f"{stem}.svg")
                os.makedirs(os.path.dirname(rel), exist_ok=True)
                with open(rel, "w", encoding="utf-8") as f:
                    f.write(make_svg(stem, theme))
                written += 1
    print(f"wrote {written} placeholder svgs under {OUT}")


if __name__ == "__main__":
    main()
