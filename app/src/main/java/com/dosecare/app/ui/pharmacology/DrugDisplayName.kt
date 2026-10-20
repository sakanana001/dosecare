package com.dosecare.app.ui.pharmacology

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.dosecare.app.model.pharmacology.Drug
import com.dosecare.app.model.pharmacology.displayNameFor
import com.dosecare.app.model.pharmacology.secondaryNameFor

/**
 * 当前界面语言标签 (跟随 in-app 语言切换器 / 系统 per-app locale)。
 *
 * 底座的 `SearchableDrugPicker` 一直用 `LocalConfiguration.current.locales[0].language`
 * 判断是否中文界面, 这里沿用同一来源, 保证各处药名展示口径一致。
 */
@Composable
fun currentUiLanguage(): String =
    LocalConfiguration.current.locales[0].language

/** 药的主显示名 (按当前界面语言)。 */
@Composable
fun Drug.displayName(): String = displayNameFor(currentUiLanguage())

/** 药的副显示名 (按当前界面语言)。 */
@Composable
fun Drug.secondaryDisplayName(): String = secondaryNameFor(currentUiLanguage())
