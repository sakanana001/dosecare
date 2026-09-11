"""Insert field_note + field_note_hint string entries into all 4 locale strings.xml files,
right after field_time_of_application so it groups with the other dose-log fields."""
from pathlib import Path

ROOT = Path(r"C:\Users\yuwen\Desktop\featherline\app\src\main\res")

files = {
    ROOT / "values" / "strings.xml": ("Note", "Notes for this application (optional)"),
    ROOT / "values-b+zh+Hans" / "strings.xml": ("备注", "本次用药的备注（可选）"),
    ROOT / "values-b+zh+Hant" / "strings.xml": ("備註", "本次用藥的備註（選填）"),
    ROOT / "values-b+yue+Hant+HK" / "strings.xml": ("備註", "今次用藥嘅備註（可選）"),
}

needle = '<string name="field_time_of_application">'

for path, (label, hint) in files.items():
    text = path.read_text(encoding="utf-8")
    if "field_note" in text:
        print(f"SKIP (already present): {path}")
        continue
    new_block = f'    <string name="field_note">{label}</string>\n    <string name="field_note_hint">{hint}</string>\n'
    if needle not in text:
        print(f"WARN: needle not found, appending at end: {path}")
        text = text + new_block
    else:
        idx = text.find(needle)
        end = text.find("</string>", idx) + len("</string>")
        text = text[:end] + "\n" + new_block + text[end:]
    path.write_text(text, encoding="utf-8")
    print(f"updated: {path}")
