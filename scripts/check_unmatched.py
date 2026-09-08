"""检查 v0.4 124 药中所有 Chinese 字符, 哪些没在字典里"""
import sys
import json
import importlib.util
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
spec = importlib.util.spec_from_file_location("pd", Path(__file__).parent / "pinyin_dict.py")
pd = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pd)

ROOT = Path(r"C:\Users\yuwen\Desktop\精品神药")
data = json.load(open(ROOT / "data" / "drugs" / "v0.4.json", encoding="utf-8"))

chars_in_catalog = set()
for d in data["drugs"]:
    for field in ["genericNameZh"]:
        v = d.get(field, "")
        chars_in_catalog.update(c for c in v if '\u4e00' <= c <= '\u9fff')
    for b in d.get("brandNames", []):
        chars_in_catalog.update(c for c in b if '\u4e00' <= c <= '\u9fff')

# 找出没在字典里的
unmatched = sorted(c for c in chars_in_catalog if c not in pd.PINYIN_INITIAL)
print(f"Total chars in catalog: {len(chars_in_catalog)}")
print(f"Unmatched: {len(unmatched)}")
for c in unmatched:
    print(f"  {c!r} (U+{ord(c):04X})")
