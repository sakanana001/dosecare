"""检查 unmatched chars 的实际字符 (用 unicodedata)"""
import sys
import json
import importlib.util
from pathlib import Path
import unicodedata

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

unmatched = sorted(c for c in chars_in_catalog if c not in pd.PINYIN_INITIAL)
print("Unmatched chars (with name + U+xxxx):")
for c in unmatched:
    try:
        name = unicodedata.name(c)
    except ValueError:
        name = "?"
    # 输出 Python 源中的转义形式
    print(f"  U+{ord(c):04X} = {c!r} (name: {name})")
