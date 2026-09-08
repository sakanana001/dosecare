# prototype/ — 领域逻辑核心库（纯 Kotlin）

> ⚠️ **2026-09-06 更新**：所有 Kotlin 文件已合并到 `app/src/main/java/com/dosecare/app/domain/`（package `com.dosecare.app.domain.*`）。v1.5 KMP 抽离时再从这里拆出。prototype/ 留作架构参考。

这里放的是**与 Android 完全解耦的纯 Kotlin 代码**，包含 PK 引擎、规则引擎、药物目录 schema、患者画像、Repository 接口。

**为什么不是 Android Studio 项目**：写完整 Gradle 工程会引入大量 boilerplate（gradle wrapper、AGP、Room 注解处理等），这些你应该根据实际需要配置。我把**最关键、最难写**的领域逻辑抽出来，方便你：

1. 快速阅读 PK 引擎和规则引擎的实现思路
2. 在 KMP 抽离时（v1.5）直接搬走 `prototype/src/main/kotlin/*` 到 KMP shared module
3. 用单元测试独立验证（不需要 Robolectric）

## 目录

```
src/main/kotlin/
├── pk/                          # PK 引擎
│   ├── PkModel.kt               # PK 模型定义（口服一房室/IV/治疗窗）
│   └── PkEngine.kt              # 多次给药、稳态、叠加计算
│
├── catalog/                     # 药物目录
│   ├── DrugCatalog.kt           # Drug/CypProfile/AdverseEffects 等 schema
│   ├── CypProfile.kt            # CYP 谱定义
│   └── seed/
│       └── Clozapine.kt         # 氯氮平示例（完整的"教科书"数据）
│
├── rules/                       # 规则引擎
│   ├── Interactions.kt          # 6 类 Interaction 数据类 + 严重度
│   └── RuleEngine.kt            # 6 类规则的实现
│
├── patient/                     # 患者画像
│   └── PatientProfile.kt        # 影响 PK 和警示的个体因素
│
└── app/repository/              # Repository 接口（KMP-ready）
    └── Repository.kt            # 接口定义，留好 future 联网扩展
```

## 快速验证（在自己的 Gradle 项目里）

最小测试代码：

```kotlin
import catalog.DrugCatalogService
import catalog.seed.ClozapineSeed
import catalog.EffectStrength
import catalog.CypContribution
import catalog.CypEnzyme
import catalog.CypInhibition
import catalog.CypProfile
import catalog.Drug
import catalog.DrugCategory
import catalog.DrugForm
import catalog.MonitoringRequirements
import catalog.AdverseEffects
import catalog.DoseAdjustments
import pk.PkEngine
import pk.PkModel
import pk.DoseEvent
import patient.PatientProfile
import patient.BiologicalSex
import rules.RuleEngine
import rules.UserDrugForRule

fun main() {
    // 1. 加载药物目录
    val catalog = DrugCatalogService(listOf(ClozapineSeed.drug))

    // 2. 创建 PK 引擎
    val pkEngine = PkEngine()

    // 3. 模拟氯氮平 100mg bid 用药 5 天
    val doseEvents = (0 until 10).map { i ->
        DoseEvent(doseMg = 100.0, tHours = i * 12.0)
    }
    val curve = pkEngine.multiDoseCurve(
        model = ClozapineSeed.drug.pkModel,
        doseEvents = doseEvents,
        tStart = 96.0,    // 第 5 天起
        tEnd = 120.0,     // 第 5 天末
        stepHours = 0.25
    )

    println("氯氮平 100mg bid 第 5 天：")
    println("  Cmax = ${curve.cMax} ng/mL")
    println("  Cmin = ${curve.cMin} ng/mL")
    println("  Cavg = ${curve.cAvg} ng/mL")
    println("  AUC  = ${curve.auc} ng·h/mL")
    println("  治疗窗 = 350-600 ng/mL，状态：${curve.windowStatus}")

    // 4. 估算稳态
    val ss = pkEngine.steadyState(ClozapineSeed.drug.pkModel, doseMg = 100.0, tauHours = 12.0)
    println("稳态：")
    println("  Cmax = ${ss.cMax} ng/mL")
    println("  Cmin = ${ss.cMin} ng/mL")
    println("  累积比 R = ${ss.accumulationRatio}")

    // 5. 跑规则引擎
    val patient = PatientProfile(
        displayName = "示例患者",
        biologicalSex = BiologicalSex.MALE,
        smoker = false
    )
    val ruleEngine = RuleEngine(catalog)
    val interactions = ruleEngine.evaluate(
        userDrugs = listOf(
            UserDrugForRule("clozapine", 100.0, 2.0)
        ),
        patient = patient
    )
    println("警示：")
    interactions.forEach { i ->
        println("  [${i.severity.displayName}] ${i.title}")
    }
}
```

## 单元测试示例

`PkEngineTest.kt`：

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import pk.PkEngine
import pk.PkModel
import pk.DoseEvent

class PkEngineTest {

    @Test
    fun `one-compartment oral matches analytical formula`() {
        val model = PkModel.OneCompartmentWithAbsorption(
            f = 0.6, kaPerHour = 1.5, kePerHour = 0.05, vdLiters = 400.0
        )
        val engine = PkEngine()
        val curve = engine.multiDoseCurve(
            model = model,
            doseEvents = listOf(DoseEvent(100.0, 0.0)),
            tStart = 0.0, tEnd = 24.0, stepHours = 0.1
        )

        // 解析解验证：在 12h 时刻
        val t12 = 12.0
        val analytical = model.concentrationAt(100.0, t12)
        val numerical = curve.c.first { it > 0.0 && (curve.t[curve.c.indexOf(it)] - t12).let { d -> d < 0.1 } }

        assertEquals(analytical, numerical, 1e-3)
    }
}
```

`RuleEngineTest.kt`（关键 case）：

```kotlin
class RuleEngineTest {

    @Test
    fun `fluvoxamine + clozapine is contraindicated`() {
        // 需要 mock 出 fluvoxamine 的 drug 数据
        // 验证 AUC 折叠 ≥ 5，severity = CONTRAINDICATED
    }
}
```

## 接下来要做的事

1. **创建 Android Studio 工程**，用 `File → New → New Project → Empty Activity` 起步
2. **把 `prototype/src/main/kotlin/*` 全部 import 进 `app/src/main/java/`**（或挪到 `domain/` 模块）
3. **加 Room + SQLCipher** 依赖（继承 PicoHRT 的依赖管理）
4. **写 DrugCatalog 加载器**：从 `assets/drugs/v1.json` 读 JSON，解析成 `List<Drug>`，注入 `DrugCatalogService`
5. **写 ViewModel + Compose UI**（参考 docs/06-UI_DESIGN.md）
6. **写第一组 rule test**：从真实病例回放 5-10 个，确认规则正确
