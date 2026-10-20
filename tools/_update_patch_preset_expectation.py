# -*- coding: utf-8 -*-
"""更新 MedicationCatalogTest 的贴剂预设期望值。

目录里 PATCH 的预设早已从"只有 5 个纯 mcg/day"扩成"品牌预设 (Climara / Estraderm MX /
Vivelle-Dot / Estrana Tape, 带 productLabel + wearDays) + 5 个纯预设"(见
MedicationCatalog.kt 的 PR3.5 注释), 但这条断言还停留在旧版本, 于是必挂。
PATCH_OFF 仍然应该是空的 —— 那部分期望不变。
"""
import io
import os

PATH = r"C:\Users\yuwen\Desktop\dosecare-workspace\merged\app\src\test\java\com\dosecare\app\model\medication\MedicationCatalogTest.kt"

with io.open(PATH, "r", encoding="utf-8", newline="") as f:
    text = f.read()
nl = "\r\n" if "\r\n" in text else "\n"

OLD = (
    "            listOf(" + nl +
    "                MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(\"50\")," + nl +
    "                MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(\"75\")," + nl +
    "                MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(\"100\")," + nl +
    "                MedicationDoseAssistPreset.PatchTotalMg(\"0.36\")," + nl +
    "                MedicationDoseAssistPreset.PatchTotalMg(\"0.72\")," + nl +
    "            )," + nl
)

BRANDED = [
    ("25", "Climara 25", 7),
    ("50", "Climara 50", 7),
    ("75", "Climara 75", 7),
    ("100", "Climara 100", 7),
    ("25", "Estraderm MX 25", 4),
    ("50", "Estraderm MX 50", 4),
    ("100", "Estraderm MX 100", 4),
    ("25", "Vivelle-Dot 25", 4),
    ("50", "Vivelle-Dot 50", 4),
    ("100", "Vivelle-Dot 100", 4),
    ("50", "Estrana Tape 50", 7),
]

lines = ["            listOf("]
for value, label, days in BRANDED:
    lines.append(
        "                MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay("
        "\"" + value + "\", productLabel = \"" + label + "\", wearDays = " + str(days) + "),"
    )
for value in ("50", "75", "100"):
    lines.append(
        "                MedicationDoseAssistPreset.PatchReleaseRateMcgPerDay(\"" + value + "\"),"
    )
lines.append("                MedicationDoseAssistPreset.PatchTotalMg(\"0.36\"),")
lines.append("                MedicationDoseAssistPreset.PatchTotalMg(\"0.72\"),")
lines.append("            ),")

NEW = nl.join(lines) + nl

assert text.count(OLD) == 1, "旧的贴剂期望没匹配上 (count=%d)" % text.count(OLD)
text = text.replace(OLD, NEW, 1)

with io.open(PATH, "w", encoding="utf-8", newline="") as f:
    f.write(text)
print("updated the patch preset expectation (newline=%r)" % nl)
