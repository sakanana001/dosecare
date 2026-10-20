package com.dosecare.app.model.pharmacology

/**
 * 药名的界面语言适配。
 *
 * 目录里每个药最多有三个名字:
 *   - [Drug.genericName]    英文 INN
 *   - [Drug.genericNameZh]  中文通用名
 *   - [Drug.genericNameJa]  日文通用名 (INN 日本語)
 *
 * 主名 [displayNameFor] 按当前界面语言取; 副名 [secondaryNameFor] 取另一种语言,
 * 两者拼起来就是 UI 上的 "主名 · 副名" 双名展示。
 *
 * 纯 Kotlin, 不依赖 Android —— 这样可以在单元测试里直接覆盖, 不必上仪器。
 */

/**
 * 按界面语言取药的主显示名。
 *
 * @param language 界面语言标签 (取自 `Locale.language`, 例如 "zh" / "yue" / "ja" / "en")
 */
fun Drug.displayNameFor(language: String): String = when (language) {
    "zh", "yue" -> genericNameZh
    "ja" -> genericNameJa?.takeIf { it.isNotBlank() } ?: genericName
    else -> genericName
}

/**
 * 按界面语言取药的副显示名 (与主名配对的另一种语言名)。
 *
 * 中文/日文界面下副名用英文 INN (国际通用, 医生和说明书上最常见);
 * 英文界面下副名用中文通用名 (沿用底座既有行为)。
 */
fun Drug.secondaryNameFor(language: String): String = when (language) {
    "zh", "yue", "ja" -> genericName
    else -> genericNameZh
}

/**
 * 药名是否命中搜索词 (中/英/日三种通用名 + 品牌名 + ATC 码 + 中文名拼音首字母)。
 *
 * 这是选药器 (`SearchableDrugPicker`) 真正使用的那份规则 —— UI 层直接调它,
 * 不再自己内联一份, 这样单元测试覆盖到的就是上线的行为。
 */
fun Drug.matchesNameQuery(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    val ql = q.lowercase()
    // 纯 ASCII 串才按拼音首字母解释, 否则 "氟" 之类会被误判
    val isPinyinInitialsQuery = q.all { it in 'a'..'z' || it in 'A'..'Z' }
    return genericNameZh.contains(q, ignoreCase = true) ||
        genericNameJa?.contains(q, ignoreCase = true) == true ||
        genericName.lowercase().contains(ql) ||
        brandNames.any { it.lowercase().contains(ql) } ||
        atc?.lowercase()?.contains(ql) == true ||
        (isPinyinInitialsQuery && toPinyinInitials(genericNameZh).lowercase().contains(ql))
}
