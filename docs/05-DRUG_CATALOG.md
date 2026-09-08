# 05 · 药物目录（DrugCatalog）

## 范围

### 核心范围（必须全）

#### 精神科（~120-150 个）
- **抗精神病药**（口服 + LAI）：氯氮平、奥氮平、利培酮、阿立哌唑、喹硫平、齐拉西酮、氨磺必利、帕利哌酮、氟哌啶醇、奋乃静、氯丙嗪、舒必利、硫必利、五氟利多、癸酸氟哌啶醇、棕榈酸帕利哌酮
- **心境稳定剂**：碳酸锂、丙戊酸钠、卡马西平、拉莫三嗪、奥卡西平
- **抗抑郁药**：
  - SSRI：氟西汀、舍曲林、帕罗西汀、氟伏沙明、西酞普兰、艾司西酞普兰
  - SNRI：文拉法辛、度洛西汀、米那普仑
  - 三环类：阿米替林、丙咪嗪、氯米帕明、多塞平
  - MAOI：苯乙肼、反苯环丙胺、吗氯贝胺
  - 其他：米氮平、安非他酮、曲唑酮、沃替西汀、维拉佐酮
- **抗焦虑/苯二氮䓬类**：地西泮、氯硝西泮、阿普唑仑、劳拉西泮、奥沙西泮、咪达唑仑、艾司唑仑
- **Z 类助眠**：唑吡坦、佐匹克隆、右佐匹克隆
- **ADHD**：哌甲酯（速释+缓释）、阿托莫西汀、托莫西汀、安非他命
- **抗胆碱能**：苯海索（安坦）
- **抗震颤麻痹**：左旋多巴/卡比多巴、普拉克索、罗匹尼罗

#### 内分泌（~60-80 个）
- **降糖**：
  - 双胍：二甲双胍
  - 磺脲：格列本脲、格列吡嗪、格列齐特、格列美脲
  - 格列奈：瑞格列奈、那格列奈
  - α-糖苷酶抑制剂：阿卡波糖、伏格列波糖
  - DPP-4：西格列汀、沙格列汀、维格列汀、利格列汀
  - SGLT2：恩格列净、达格列净、卡格列净
  - GLP-1：艾塞那肽、利拉鲁肽、度拉糖肽、司美格鲁肽
  - 胰岛素：速效/短效/中效/长效/预混（10+ 种）
- **甲状腺**：左甲状腺素、丙硫氧嘧啶、甲巯咪唑
- **肾上腺皮质激素**：泼尼松、泼尼松龙、地塞米松、甲泼尼龙
- **钙/骨代谢**：碳酸钙、骨化三醇、阿法骨化醇、双膦酸盐

#### 心血管（精神科共病必需，~40-50 个）
- **降压**：ACEI（卡托普利、依那普利）、ARB（缬沙坦、厄贝沙坦）、CCB（氨氯地平、硝苯地平）、β 阻滞剂（美托洛尔、比索洛尔、阿替洛尔）、利尿剂（氢氯噻嗪、呋塞米、螺内酯）
- **调脂**：他汀类（阿托伐他汀、瑞舒伐他汀、辛伐他汀、普伐他汀）
- **抗血小板**：阿司匹林、氯吡格雷、替格瑞洛
- **抗凝**：华法林、利伐沙班、达比加群

#### 其他常用共病
- **抗癫痫**（精神科常用于情感稳定）：苯妥英、托吡酯、左乙拉西坦
- **止痛**（精神科的疼痛共病）：对乙酰氨基酚、布洛芬、萘普生、曲马多、羟考酮
- **抗组胺**（OTC 重灾区）：苯海拉明、氯苯那敏、西替利嗪、氯雷他定
- **草本补充剂**：圣约翰草、卡瓦胡椒、褪黑素、银杏

**总计：~250-300 个药。** v0.5 上线 200+ 即可发版，剩余迭代补齐。

## 数据来源

### 一手数据（学术可靠）

| 来源 | 内容 | 许可 | 备注 |
|------|------|------|------|
| **DrugBank** | PK、靶点、CYP 谱 | 学术免费 / 商用付费 | v0.1-v0.5 用学术版可商用时切 |
| **OpenFDA** | 美国 FDA 标签 SPL | 公开 | 结构化好 |
| **Flockhart Table** | CYP 相互作用 | 公开 | https://drug-interactions.medicine.iu.edu |
| **AGNP Consensus** | 精神科 TDM 治疗窗 | 公开 PDF | 黄金标准 |
| **DailyMed** | FDA 标签 | 公开 | 文本格式 |
| **国内药品说明书** | 中国上市药 | 各厂商版权 | 仅供 PK / 用法，不全文复制 |
| **ClinicalTables / RxNorm** | 命名归一 | 公开 | 解决别名问题 |
| **Wikipedia CYP 表** | CYP 关系 | CC-BY-SA | 二手 |
| **药品监督局数据** | 中国 CFDA 上市 | 公开 | 决定哪些药在中国可用 |

### 数据采集流程

```
原始数据源
    ↓
[Python ETL]   解析 SPL/PDF/HTML → 统一 JSON schema
    ↓
[人工 review]  临床药师/医学编辑核对 PK 字段和警示
    ↓
[JSON]        v1.json
    ↓
[APK 打包]    assets/drugs/v1.json
    ↓
[运行时]      DrugCatalog.load() 加载到内存
```

### 关键数据冲突解决

多个来源对同一字段有矛盾时：

1. **优先 AGNP / FDA 标签**（监管认可）
2. **次优 Clinical Pharmacology / Lexicomp**（专业数据库）
3. **最末 原始文献**（PubMed）

争议字段在 JSON 中保留 `notes: ["CONFLICTING: source A says X, source B says Y"]` 提示用户。

## 数据维护

### 增量更新（v1.0+）

通过 `assets/drugs/v1.json` → `v1.1.json` 增量发布：
- 新药 → 直接追加
- 已有药更新 → 覆盖（带 updatedAt）
- 旧药下架 → 标记 `deprecated: true`，仍保留供历史查询

应用启动时检查版本：

```kotlin
fun maybeUpdateCatalog(context: Context) {
    val currentVersion = Prefs.getCatalogVersion()
    val newVersion = AssetUtil.readVersion("drugs/v1.json")
    if (newVersion > currentVersion) {
        // 提示用户
        // 用户确认后加载
    }
}
```

> v0.1-v0.5 不做联网更新，目录变化跟随 APP 版本发布。
> v1.0+ 加 `CatalogUpdateWorker` 增量下载（用户可选）。

## 字段标准化

### 命名

- `id` = 英文小写下划线（`clozapine`）
- `genericName` = INN
- `genericNameZh` = 国家药典中文名
- 同一药多别名进 `brandNames` 数组

### 单位

| 字段 | 单位 |
|------|------|
| `f` | 0-1（无量纲） |
| `kaPerHour` | h⁻¹ |
| `tHalfHours` | h |
| `vdLPerKg` | L/kg |
| `clLPerHour` | L/h |
| `therapeuticWindow.*` | 见各药约定 |
| `proteinBindingPct` | 0-100 |

### Cyp 强度

```kotlin
enum class EffectStrength(val aucFoldLow: Double, val aucFoldHigh: Double) {
    WEAK(1.25, 2.0),
    MODERATE(2.0, 5.0),
    STRONG(5.0, Double.MAX_VALUE)
}
```

## CYP 谱编码规范

```json
"cypProfile": {
  "substrates": [
    { "cyp": "CYP1A2", "fraction": 0.7 },
    { "cyp": "CYP3A4", "fraction": 0.2 }
  ],
  "inhibitors": [
    { "cyp": "CYP2D6", "strength": "STRONG" }
  ],
  "inducers": []
}
```

`fraction` 之和应 ≤ 1.0（剩余留给非 CYP 路径：UGT、SULT、直接肾排等）。

## 警示消息

每条 Interaction 都有：

- `message`（用户友好的中文）
- `clinicalNote`（医师友好的解释）
- `mechanism`（药理机制）
- `references`（PubMed ID 或 URL）

例：

```kotlin
Interaction(
    type = InteractionType.CYP_INHIBITION,
    severity = Severity.CONTRAINDICATED,
    title = "氟伏沙明 + 氯氮平：致命风险",
    message = "氟伏沙明强烈抑制氯氮平的代谢酶 CYP1A2，"
            + "可能导致氯氮平血药浓度升高 5-10 倍，"
            + "引发粒细胞缺乏、癫痫发作、严重低血压等。",
    mechanism = "CYP1A2 强抑制",
    clinicalNote = "如必须联用，氯氮平剂量需减至 1/5-1/10，"
                 + "并加强血药浓度监测。",
    references = listOf(
        "PMID: 10210627", 
        "FDA Fluvoxamine Prescribing Information"
    )
)
```

## 质量保证

### 自动化测试

- 加载所有药物后跑 `DrugCatalogValidator`：
  - 所有必填字段存在
  - `f` 在 [0, 1]
  - `tHalf > 0`, `ke > 0`
  - `cypProfile.substrates` 分数和 ≤ 1.01
  - 每个交互的 `references` 不为空
  - 没有重复 `id`

### 临床 review

v0.5 后强烈建议找一位**精神科临床药师**作为顾问：

- 至少 review 一次完整 DrugCatalog
- 给出 critical interactions 清单的审核
- 帮确定严重度分级
- 帮忙发现遗漏的"经典禁忌"

这是**保护你（开发者）** 也是**保护用户**的步骤。

## 已知风险

- **数据错误可能误导患者**——必须有清晰免责声明
- **新研究更新快**——CYP 相互作用有"经典错误"被推翻的案例（如某些中成药）
- **个体差异巨大**——CYP 基因多态性（PM / IM / EM / UM）能改变一切
- **食物、草本补充剂很难穷尽**——鼓励用户主动录入"我的补充剂"
