#!/usr/bin/env python3
"""在 Plain.kt 的 cypSubstrate 后面插入 metabolism() 方法 — 不用 old_string 整块, 用 partial match"""
from pathlib import Path
p = Path("app/src/main/java/com/dosecare/app/ui/Plain.kt")
text = p.read_text(encoding="utf-8")

marker = '"这个药在肝脏里被某些酶拆解。吃的剂量越多、酶被抑制 → 浓度升高, 容易过量"'
if marker not in text:
    print("FAIL: marker not found")
else:
    add = "\n\n    /** 主要代谢途径 (含 CYP 比例 / 非 CYP 标注) */\n    fun metabolism(pathwayType: com.dosecare.app.domain.catalog.PathwayType?): String = when (pathwayType) {\n        com.dosecare.app.domain.catalog.PathwayType.CYP450 ->\n            \"这个药主要被肝脏 CYP 酶系统代谢, 容易跟其他 CYP 抑制剂/诱导剂产生相互作用\"\n        com.dosecare.app.domain.catalog.PathwayType.UGT_GLUCURONIDATION, com.dosecare.app.domain.catalog.PathwayType.GLUCURONIDATION ->\n            \"经葡萄糖苷酸化代谢 (Phase II), 通常不跟 CYP 抑制剂/诱导剂相互作用, 相互作用少\"\n        com.dosecare.app.domain.catalog.PathwayType.RENAL_EXCRETION ->\n            \"原型经肾排出, 不经肝代谢, 几乎无 CYP 介导的药物相互作用, 肾衰时需减量\"\n        com.dosecare.app.domain.catalog.PathwayType.HYDROLYSIS, com.dosecare.app.domain.catalog.PathwayType.ESTERASE ->\n            \"经血浆/组织酯酶水解, 不依赖肝肾功能, 几乎无药物相互作用\"\n        com.dosecare.app.domain.catalog.PathwayType.DEIODINATION ->\n            \"经脱碘酶代谢 (甲状腺素特殊路径), 相互作用少\"\n        com.dosecare.app.domain.catalog.PathwayType.MAO ->\n            \"经单胺氧化酶代谢, 注意 tyramine 反应 (奶酪效应)\"\n        com.dosecare.app.domain.catalog.PathwayType.DPP4 ->\n            \"DPP-IV 酶降解 (GLP-1 类似物), 注射给药\"\n        com.dosecare.app.domain.catalog.PathwayType.BETA_OXIDATION ->\n            \"经脂肪酸 β-氧化 + UGT 葡萄糖苷酸化, 多途径代谢\"\n        else ->\n            \"其他途径代谢, 详见 catalog\"\n    }"
    text = text.replace(marker, marker + add, 1)
    p.write_text(text, encoding="utf-8")
    print("OK: metabolism() added after cypSubstrate")
