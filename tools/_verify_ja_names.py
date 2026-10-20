#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
验证候选日文药名: 日文维基百科是否真的存在以该名字为标题的条目。

这比"搜索取第一条命中"可靠得多 —— 搜索会把「非ベンゾジアゼピン系」这种
分类条目当成药名, 而"标题完全等于该药名"是有意义的硬证据。

用法:
  python _verify_ja_names.py                 # 验证 _ja_drug_titles.json 里的候选
  python _verify_ja_names.py --only-pending  # 只验证还没定稿的
"""
import io
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HERE = os.path.join(ROOT, "tools")
API = "https://ja.wikipedia.org/w/api.php"
UA = "DoseCareCatalogTool/1.0 (local dev tool for i18n; contact: local)"


def exists(titles):
    """返回 {标题: (是否存在, 首段文本)} —— 标题会被 redirects 跟随。"""
    params = {
        "action": "query",
        "titles": "|".join(titles),
        "redirects": "1",
        "prop": "extracts",
        "exintro": "1",
        "explaintext": "1",
        "format": "json",
        "formatversion": "2",
    }
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
    for attempt in range(5):
        try:
            with urllib.request.urlopen(req, timeout=40) as resp:
                data = json.loads(resp.read().decode("utf-8"))
            break
        except Exception:
            if attempt < 4:
                time.sleep(5 * (attempt + 1))
                continue
            raise
    q = data.get("query", {})
    redirected = {r["from"]: r["to"] for r in (q.get("redirects") or [])}
    out = {}
    for p in (q.get("pages") or []):
        title = p.get("title", "")
        out[title] = (not p.get("missing", False), p.get("extract", "") or "")
    # 映射回原始标题
    res = {}
    for orig, tgt in redirected.items():
        res[orig] = out.get(tgt, (False, ""))
    for t, v in out.items():
        res.setdefault(t, v)
    return res


def norm(s):
    return re.sub(r"[^a-z0-9]", "", (s or "").lower())


# 复合名 / 盐型 / 剂型: 用哪些英文词判定"这条日文条目讲的确实是这个药"
TOKENS = {
    "memantine_donepezil": ["memantine", "donepezil"],
    "xanomeline": ["xanomeline", "trospium"],
    "thyroid_armour": ["thyroid"],
    "conjugated_estrogens": ["estrogen", "estradiol"],
    "morphine_6_glucuronide": ["morphine", "glucuronide"],
    "norketamine": ["norketamine", "ketamine"],
    "dihydroetorphine": ["etorphine"],
    "levalbuterol": ["levalbuterol", "albuterol", "salbutamol"],
    "risedronate": ["risedronate"],
    "eslicarbazepine": ["eslicarbazepine", "oxcarbazepine"],
    "levodopa_carbidopa": ["levodopa", "carbidopa"],
    "bupropion_xl": ["bupropion"],
    "venlafaxine_xr": ["venlafaxine"],
    "paroxetine_cr": ["paroxetine"],
    "fluoxetine_weekly": ["fluoxetine"],
    "valproic_acid": ["valproate", "valproic"],
    "lithium": ["lithium"],
    "methylprednisolone": ["methylprednisolone"],
    "levothyroxine": ["levothyroxine"],
    "desmopressin": ["desmopressin"],
    "gabapentin": ["gabapentin"],
}


def tokens_for(r):
    if r["id"] in TOKENS:
        return TOKENS[r["id"]]
    en = r["en"]
    # "Levodopa/Carbidopa" / "Memantine + Donepezil" / "Bupropion XL" / "Zoledronic acid"
    first = re.split(r"[+/]|XL|XR|CR|Weekly", en)[0].strip()
    return [first] if first else [en]


def main():
    path = os.path.join(HERE, "_ja_drug_titles.json")
    with io.open(path, "r", encoding="utf-8") as f:
        rows = json.load(f)

    only_pending = "--only-pending" in sys.argv
    targets = [r for r in rows if r.get("ja") and (not only_pending or r.get("ja_source") in ("ja-search", "derived", "split", None))]
    print("待验证: %d 条" % len(targets))

    ok, bad = [], []
    for i in range(0, len(targets), 20):
        chunk = targets[i:i + 20]
        names = [r["ja"] for r in chunk]
        res = exists(names)
        for r in chunk:
            found, extract = res.get(r["ja"], (False, ""))
            en_ok = any(norm(t) and norm(t) in norm(extract) for t in tokens_for(r))
            if found and en_ok:
                ok.append(r)
            else:
                bad.append((r, found, en_ok, extract[:120]))
        time.sleep(1.0)

    print("\n日文维基条目存在且首段提到对应英文药名: %d" % len(ok))
    print("未通过: %d" % len(bad))
    print("\n-- 未通过的 (需要人工定名) --")
    for r, found, en_ok, ex in bad:
        print("  %-28s | %-32s | 候选: %-22s | 存在=%-5s 英文名匹配=%-5s [%s]"
              % (r["id"], r["en"], r["ja"], found, en_ok, r.get("ja_source")))
        if not en_ok:
            print("        首段: %s" % ex.replace("\n", " ")[:110])

    out = os.path.join(HERE, "_ja_name_verification.json")
    with io.open(out, "w", encoding="utf-8") as f:
        json.dump({
            "verified": [r["id"] for r in ok],
            "unverified": [{"id": r["id"], "en": r["en"], "zh": r["zh"],
                            "candidate": r["ja"], "source": r.get("ja_source")} for r, _, _, _ in bad],
        }, f, ensure_ascii=False, indent=2)
    print("\nwrote %s" % out)


if __name__ == "__main__":
    main()
