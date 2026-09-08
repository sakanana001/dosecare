"""A6: Clean up '急性过量经验有限' marker from symptoms that have other real data"""
import json
from pathlib import Path

V06 = Path(r"C:\Users\yuwen\Desktop\精品神药\app\src\main\assets\drugs\v0.6.json")
data = json.loads(V06.read_text(encoding="utf-8"))
drugs = data["drugs"]

cleaned = 0
for d in drugs:
    od = d.get("overdose", {})
    s = od.get("symptoms", "")
    # Remove "急性过量经验有限" prefix/phrase when there's other real content
    if "急性过量经验有限" in s and len(s) > 20:
        # Real content exists, remove the placeholder phrase
        s2 = s.replace("急性过量经验有限; ", "").replace("急性过量经验有限", "").strip("; ")
        if s2 != s:
            od["symptoms"] = s2
            d["overdose"] = od
            cleaned += 1

print(f"Cleaned {cleaned} drugs")
V06.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"Saved {V06}")
