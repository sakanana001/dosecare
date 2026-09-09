"""Extract DrugCategory + IndicationGroup enum values from Kotlin source."""
import re

src = open('app/src/main/java/com/dosecare/app/domain/catalog/DrugCatalog.kt', 'r', encoding='utf-8').read()
m = re.search(r'enum class DrugCategory.*?^\}', src, re.DOTALL | re.MULTILINE)
if m:
    cats = re.findall(r'(\w+)\("([^"]+)"\)', m.group(0))
    print(f'# DrugCategory: {len(cats)} entries')
    for en, zh in cats:
        print(f'DRUG_CAT|{en}|{zh}')

src = open('app/src/main/java/com/dosecare/app/domain/catalog/IndicationGroup.kt', 'r', encoding='utf-8').read()
m = re.search(r'enum class IndicationGroup.*?^\}', src, re.DOTALL | re.MULTILINE)
if m:
    inds = re.findall(r'(\w+)\("([^"]+)"\)', m.group(0))
    print(f'# IndicationGroup: {len(inds)} entries')
    for en, zh in inds:
        print(f'INDICATION|{en}|{zh}')

# Also OverdoseSeverity + CypEnzyme + PathwayType for completeness
src = open('app/src/main/java/com/dosecare/app/domain/catalog/DrugCatalog.kt', 'r', encoding='utf-8').read()
m = re.search(r'enum class OverdoseSeverity.*?^\}', src, re.DOTALL | re.MULTILINE)
if m:
    sevs = re.findall(r'(\w+)\("([^"]+)"\)', m.group(0))
    print(f'# OverdoseSeverity: {len(sevs)} entries')
    for en, zh in sevs:
        print(f'SEVERITY|{en}|{zh}')

src = open('app/src/main/java/com/dosecare/app/domain/catalog/CypProfile.kt', 'r', encoding='utf-8').read()
m = re.search(r'enum class CypEnzyme.*?^\}', src, re.DOTALL | re.MULTILINE)
if m:
    cyps = re.findall(r'(\w+)\("([^"]+)"\)', m.group(0))
    print(f'# CypEnzyme: {len(cyps)} entries')
    for en, zh in cyps:
        print(f'CYP|{en}|{zh}')
m = re.search(r'enum class PathwayType.*?^\}', src, re.DOTALL | re.MULTILINE)
if m:
    paths = re.findall(r'(\w+)\("([^"]+)"\)', m.group(0))
    print(f'# PathwayType: {len(paths)} entries')
    for en, zh in paths:
        print(f'PATHWAY|{en}|{zh}')
