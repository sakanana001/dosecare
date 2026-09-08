# 06 · UI 设计

## 设计原则

### 1. 极简优先（精神疾病特殊需求）

精神分裂急性期、双相躁狂期、严重抑郁期，**用户的认知带宽是低的**。
- 主屏不堆图表
- 主屏只显示两件事：**下次服药时间** + **当前是否在治疗窗**
- 大按钮、清晰颜色、高对比度
- 默认字号偏大

### 2. "操作即记录"

服药是最高频操作，必须**一次点击完成**。
- 主屏中部一个巨大的"刚吃了"按钮 → 弹底部 sheet → 默认值是上次 → 一键确认
- 不要求每次都填剂量

### 3. 警示明确但不焦虑

精神科患者中**健康焦虑**是共病。
- 严重警示（红）要醒目
- 中等警示（黄）默认折叠，不主动轰炸
- 永远不"吓唬"用户

### 4. 一加风格的对比页

你给的参考图那种并排对比非常适合"换药决策"的场景。

## 主屏（HomeScreen）

```
┌────────────────────────────────────┐
│  王小明                           ⚙  │  ← 顶栏：昵称 + 设置
├────────────────────────────────────┤
│                                    │
│        ⏰  下一剂              8 小时 23 分
│      利培酮 2mg                    │  ← 大字号、清晰
│                                    │
│   ┌──────────────────────────────┐ │
│   │                              │ │
│   │  ✓ 我刚刚吃了               │ │  ← 巨大主按钮
│   │                              │ │
│   └──────────────────────────────┘ │
│                                    │
│   血药浓度：在治疗窗内 ✓            │  ← 单行状态
│   最近一次血检 3 天前：利培酮 28ng/mL │  ← 上下文
│                                    │
│   ─────────────────────            │
│   ⚠  1 条中等警示需要查看           │  ← 折叠的警示
│                                    │
└────────────────────────────────────┘
```

**首屏的核心信息密度：1 + 1 + 1 = 三件事。**

## 导航结构

```
Bottom Nav (4 项):
├── 主屏 (Home)        ← 默认
├── 用药 (Meds)        ← 所有药列表 + 添加
├── 警示 (Alerts)      ← 冲突 / 漏服 / 治疗窗
└── 我的 (Profile)     ← 设置 / 患者画像 / 备份

抽屉 (侧滑 / Profile 内):
├── PK 曲线（详细血药浓度视图）
├── 对比 (Compare)        ← 新功能入口
├── 化验记录 (Labs)
├── 依从性日历 (Adherence)
├── 症状打卡 (Mood/Symptoms)  ← v0.5
├── 备份 (Backup)
└── 关于 (About)
```

## 屏幕列表

| 屏幕 | 关键组件 |
|------|---------|
| HomeScreen | NextDoseCard, QuickIntakeButton, StatusBanner |
| MedsListScreen | DrugCard (含 PK 缩略曲线), AddDrugFAB |
| AddDrugScreen | DrugSearchPicker, DosePicker, ScheduleBuilder |
| DrugDetailScreen | Full PK 曲线, 治疗窗, 历史记录, 警示 |
| AlertsScreen | 严重度分组, 折叠/展开 |
| CompareScreen | 双列分组对比（参考一加） |
| ProfileScreen | 体重/年龄/吸烟/CYP 基因等 |
| LabsScreen | 时间线, 录入弹窗 |
| AdherenceScreen | 热力图日历, 漏服统计 |
| BackupScreen | 导出/导入, 密码设置 |

## 关键屏幕设计

### CompareScreen（多药对比）

参考你给的一加手机参数对比图，药物版设计：

```kotlin
@Composable
fun DrugCompareScreen(
    drugA: DrugSummary,
    drugB: DrugSummary,
    catalog: DrugCatalog
) {
    val a = catalog.getById(drugA.id)
    val b = catalog.getById(drugB.id)
    
    val sections = remember(a, b) { buildCompareSections(a, b) }
    
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 顶部两列标题
        item { 
            CompareHeader(
                left = CompareColumn(
                    name = a.genericNameZh,
                    subtitle = a.genericName,
                    icon = a.chemicalStructure
                ),
                right = CompareColumn(...)
            )
        }
        
        // 免责声明
        item { 
            CompareDisclaimer()
        }
        
        // 分组循环
        sections.forEach { section ->
            item { SectionHeader(section.title) }
            items(section.fields) { field ->
                CompareFieldRow(
                    label = field.label,
                    valueLeft = field.getValue(a),
                    valueRight = field.getValue(b),
                    highlight = field.shouldHighlight(a, b),
                    colorLeft = field.color(a),
                    colorRight = field.color(b)
                )
            }
        }
    }
}

@Composable
fun CompareFieldRow(
    label: String,
    valueLeft: String,
    valueRight: String,
    highlight: Boolean,
    colorLeft: Color,
    colorRight: Color
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (highlight) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(valueLeft, Modifier.weight(1f), color = colorLeft, style = bodyMedium)
        Spacer(Modifier.width(16.dp))
        Text(label, Modifier.weight(1f), style = bodyMedium, color = onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(valueRight, Modifier.weight(1f), color = colorRight, style = bodyMedium)
    }
}
```

**分组设计：**

| 组 | 字段 | 差异高亮规则 |
|----|------|-------------|
| 药理机制 | D2、5-HT2A、H1、M1、α1 亲和力 | Ki 差异 > 10 倍 |
| 药代动力学 | F、Tmax、t½、CL、Vd、蛋白结合 | t½ 差异 > 2 倍 |
| 治疗窗 | 低 / 高 | 数字差 |
| 代谢路径 | 主代谢酶、抑制/诱导、活性代谢物 | 主酶不同 / 有致命诱导物 |
| 关键不良反应 | EPS、代谢综合征、QTc、镇静、粒缺、性功能 | 等级差 |
| 抗胆碱能 | 评分 | 评分差 ≥ 1 |
| 相互作用 | 当前与用户其他药冲突数 | 数量差 |
| 监测 | 监测项目和频率 | 文字差 |
| 食物/吸烟 | 食物效应、吸烟影响 | 一有 / 一无 |

### DrugDetailScreen（单药详情）

```
┌────────────────────────────────────┐
│  ← 氯氮平 (Clozapine)         ⋮   │
├────────────────────────────────────┤
│  [化学结构 SVG]                      │
│  通用名：氯氮平                       │
│  分类：非典型抗精神病药·二苯并二氮䓬类 │
│  ATC: N05AH02                       │
├────────────────────────────────────┤
│  [PK 曲线：24h 滚动]                │
│  ┌──────────────────────────────┐  │
│  │     [ 治疗窗带 ]              │  │
│  │  ╱╲     ╱╲                   │  │
│  │ ╱  ╲   ╱  ╲                  │  │
│  │     ╲_╱    ╲_                │  │
│  └──────────────────────────────┘  │
│  350 - 600 ng/mL  ← 治疗窗         │
│  当前：420 ng/mL ✓ 在窗内            │
├────────────────────────────────────┤
│  [今日服药]                          │
│  08:00 ✓ 100mg                      │
│  14:00 ✓ 100mg                      │
│  20:00 ⏳ 14:32 后                  │
├────────────────────────────────────┤
│  [代谢路径图]                        │
│  CYP1A2 (70%) ━→ 去甲氯氮平 (活性)  │
│  CYP3A4 (20%) ━→ 羟化代谢           │
│  CYP2C19 (10%) ━→ 羟化代谢          │
│  ⚠ 氟伏沙明强烈抑制 CYP1A2          │
├────────────────────────────────────┤
│  [最近警示]                          │
│  ⚠ 1 条 HIGH：氟西汀 (冲突)         │
├────────────────────────────────────┤
│  [监测项目]                          │
│  ● WBC 每周 (下次: 3 天后)          │
│  ● 血糖 每月                        │
│  ● 体重 每月                        │
└────────────────────────────────────┘
```

### AdherenceScreen（依从性日历）

热力图风格：

```
2026 年 9 月
┌─────────────────────────────┐
│  一  二  三  四  五  六  日  │
│  ▓   ░   ▓   ▓   ░   ▓   ▓  │ ← 9/1-9/7
│  ▓   ▓   ▓   ▓   ▓   ▓   ▓  │ ← 9/8-9/14
│  ░   ▓   ▓   ▓   ▓   ░   ▓  │ ← 9/15-9/21
│  ▓   ▓   ▓   ░   ░   ▓   ▓  │ ← 9/22-9/28
│  ▓   ▓   ?   ?   ?   ?   ?  │ ← 9/29-30
└─────────────────────────────┘
■ 完美执行   ░ 漏服/部分    ? 未来

依从率 7 日: 92%
依从率 30 日: 87%
漏服最多药: 喹硫平 (3 次)
漏服最常时段: 早上 8:00 (4 次)
```

## 主题与色彩

### 浅色主题

```kotlin
val LightColors = lightColorScheme(
    primary = Color(0xFF2C5F8D),       // 沉稳蓝
    onPrimary = Color.White,
    secondary = Color(0xFF7E5CAD),      // 紫（治疗窗带）
    error = Color(0xFFD32F2F),          // CONTRAINDICATED
    onError = Color.White,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFEFF1F4)  // 分组头灰
)
```

### 深色主题（夜间服药提示更友好）

```kotlin
val DarkColors = darkColorScheme(
    primary = Color(0xFF7AB8E8),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)
```

### 严重度色板

```kotlin
val SeverityColors = mapOf(
    Severity.CONTRAINDICATED to Color(0xFFB71C1C),  // 暗红
    Severity.HIGH to Color(0xFFEF6C00),             // 橙
    Severity.MEDIUM to Color(0xFFFBC02D),            // 黄
    Severity.LOW to Color(0xFF9E9E9E),              // 灰
    Severity.INFO to Color(0xFFE0E0E0)               // 浅灰
)
```

### 字号阶梯

- Display: 32sp（主屏下一剂时间）
- Headline: 24sp（屏标题）
- Title: 20sp（分组标题）
- Body: 16sp（默认）
- Label: 14sp（按钮文字、警示标签）
- Caption: 12sp（说明、参考）

## 交互细节

### 服药打卡动效

- 主按钮按下 → 200ms 缩放 → 弹"确认吗" → 滑出打卡完成
- 整个交互 1.5 秒内完成
- 失败有兜底：未点击"刚吃了"前 30 分钟，弹"你今天 8 点的药还没记录"提醒

### 漏服智能建议

检测到漏服（提醒触发后 30 分钟未记录）：

```
┌────────────────────────────────────┐
│  漏服：利培酮 2mg（08:00 应服）      │
│                                    │
│  现在是 08:32。距下次服药还有 5.5h  │
│                                    │
│  推荐：补服（立即 2mg）              │  ← 根据 PK 决策
│  说明：补服后 Cmax 仍在治疗窗内      │
│                                    │
│  [ 补服 2mg ]                       │
│  [ 跳过（不补）]                    │
│  [ 延后到下次一起 ]                  │
│                                    │
└────────────────────────────────────┘
```

v0.5 实现完整的"漏服决策卡"算法。

### 警示通知

```
[严重] 氟西汀 + 氯氮平：可能致死
──────────────────────────────────
强烈建议：不要联用
如已联用：立即联系您的精神科医师
详情 → [忽略] [已咨询医师]
```

## 可访问性

- 所有按钮最小 48dp × 48dp
- 文字对比度 ≥ 4.5:1
- 全部 TalkBack 标签
- 字号可放大到 200%
- 支持深色模式
- 减少动效选项（系统层）

## 国际化

- v0.1 默认中文
- 专有缩写（5-HT、NE、DA、SSRI、SNRI、MAOI、TCA、CYP3A4 等）保留英文
- v1.0+ 加 i18n 框架，支持 en-US（FDA 标签语种）
