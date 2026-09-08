#!/usr/bin/env python3
"""Drug + DrugSchema 加 pharmacology: String? 字段"""
from pathlib import Path

# 改 Drug.kt
p1 = Path("app/src/main/java/com/dosecare/app/domain/catalog/DrugCatalog.kt")
t = p1.read_text(encoding="utf-8")
old = "    val monitoring: MonitoringRequirements = MonitoringRequirements(),\n\n    val references: List<String> = emptyList()  // PMID / 说明书 / 共识指南"
new = """    val monitoring: MonitoringRequirements = MonitoringRequirements(),

    /**
     * 药理作用 (一两句话简短描述): 主要作用机制 + 治疗作用
     * 例: "选择性 5-HT 再摄取抑制剂 (SSRI), 增强 5-HT 神经传递, 抗抑郁/抗焦虑"
     */
    val pharmacology: String? = null,

    val references: List<String> = emptyList()  // PMID / 说明书 / 共识指南"""
if old not in t:
    print("FAIL: DrugCatalog.kt old_string not found")
else:
    t = t.replace(old, new, 1)
    p1.write_text(t, encoding="utf-8")
    print("OK: DrugCatalog.kt pharmacology 字段已加")

# 改 DrugCatalogLoader.kt
p2 = Path("app/src/main/java/com/dosecare/app/domain/catalog/DrugCatalogLoader.kt")
t = p2.read_text(encoding="utf-8")
old = "    val monitoring: MonitoringSchema,\n\n    val references: List<String> = emptyList(),"
new = """    val monitoring: MonitoringSchema,

    val pharmacology: String? = null,

    val references: List<String> = emptyList(),"""
if old not in t:
    print("FAIL: DrugCatalogLoader.kt old_string not found")
else:
    t = t.replace(old, new, 1)

# 找到 toInternal Drug(...) 构造, 在 references = references 前面加 pharmacology
old2 = """            criticalInteractions = criticalInteractions.mapNotNull { it.toInternal() },
            monitoring = monitoring.toInternal(),
            references = references
        )"""
new2 = """            criticalInteractions = criticalInteractions.mapNotNull { it.toInternal() },
            monitoring = monitoring.toInternal(),
            pharmacology = pharmacology,
            references = references
        )"""
if old2 in t:
    t = t.replace(old2, new2, 1)
    p2.write_text(t, encoding="utf-8")
    print("OK: DrugCatalogLoader.kt pharmacology 字段 + toInternal 透传")
else:
    print("FAIL: DrugCatalogLoader.kt toInternal old_string not found")
