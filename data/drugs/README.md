# 药物目录（DrugCatalog）数据

本目录存 DrugCatalog 各版本的 JSON 文件。每个版本对应一次目录发布。

## v0.1（当前）

5 个精神科最核心的药，作为 MVP 启动数据：

| 药 | 类别 | 治疗窗 | 关键监测 |
|----|------|--------|----------|
| 氯氮平 clozapine | 非典型抗精神病 | 350-600 ng/mL | WBC/ANC（前 18 月每周，之后每月）|
| 奥氮平 olanzapine | 非典型抗精神病 | 20-80 ng/mL | 体重/血糖/血脂 |
| 利培酮 risperidone | 非典型抗精神病 | 20-60 ng/mL (含 9-OH) | EPS、催乳素 |
| 丙戊酸钠 valproic_acid | 心境稳定剂 | 50-100 μg/mL | 血药浓度、肝功、血小板 |
| 碳酸锂 lithium | 心境稳定剂 | 0.6-1.2 mmol/L | 血锂、肾功、甲状腺 |

数据来源（详见 JSON 头部 source 字段）：
- OpenFDA DailyMed
- AGNP 2017 共识（Hiemke C. et al., Pharmacopsychiatry 2018）
- Flockhart CYP Table（Indiana University）
- 国内药品说明书
- 各药关键 PMID 引用

## 文件命名

- `v0.1.json` — 0.1.0 目录
- `v0.2.json` — 0.2.0 目录
- `v1.0.json` — 正式版

每次更新应：
1. 保留旧文件（向后兼容）
2. 在新文件中改 `schemaVersion`
3. 在 CHANGELOG.md 列出所有变更

## 集成方式（Android）

把 v0.1.json 复制到 `app/src/main/assets/drugs/v0.1.json`，启动时加载：

```kotlin
val raw = context.assets.open("drugs/v0.1.json")
    .bufferedReader(Charsets.UTF_8).use { it.readText() }
val drugs = DrugCatalogLoader.fromJson(raw)
val catalog = DrugCatalogService(drugs)
```

集成方式（单元测试）：放在 `src/test/resources/drugs/v0.1.json`，加载用 classloader。

## 字段约定

详见 [docs/02-DATA_MODEL.md](../../docs/02-DATA_MODEL.md) 的 A 章节。

- 所有时间单位：小时（h⁻¹ 或 h）
- 所有浓度单位：见各药 `therapeuticWindow.unit`
- 所有剂量单位：mg
- 半衰期：h
- 分布容积：L/kg（v0.1 默认按 70kg 计算 Vd）
- 蛋白结合：0-100 (%)

## 已知限制（v0.1）

- 只支持一房室口服模型（`ONE_COMPARTMENT_ORAL`）
- 默认按 70kg 计算 Vd，不做个体化体重调整
- CYP 字段只有 6 种 CYP 酶（CYP1A2/2D6/3A4/2C9/2C19/2E1），UGT/UGT1A4 走 note
- 严重度分级：`VERY_LOW / LOW / MEDIUM / HIGH / VERY_HIGH`（注意：与 Interaction.severity 的 `INFO/LOW/MEDIUM/HIGH/CONTRAINDICATED` 是不同维度，不要混用）

## 后续计划

- v0.2（+2 周）：扩展到 20 个药
- v0.3（+6 周）：扩展到 100 药
- v0.5（+10 周）：扩展到 200+ 药
- v1.0：临床药师 review 完成版
