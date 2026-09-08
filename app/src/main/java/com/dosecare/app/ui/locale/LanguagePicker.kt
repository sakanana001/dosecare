package com.dosecare.app.ui.locale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dosecare.app.R

/**
 * v0.9a 多语言: 设置页里的「语言」入口 Card + 弹窗选择器
 *
 * 用法 (v0.9a 接入 SettingsScreen):
 *   LanguageSettingsCard()                  // 在 SettingsScreen Column 里
 *
 * 选完语言:
 *  - LocaleController.setLanguage() 写 SharedPreferences + 触发 AppCompatDelegate.setApplicationLocales
 *  - AppCompat 1.7+ 内部自动 recreate Activity (Android 13+ 直接走系统, 旧版用 AsyncTask 模拟)
 *  - 新 MainActivity.onCreate 调 LocaleController.load() 读回新状态, UI 自动用新语言渲染
 */
@Composable
fun LanguageSettingsCard(modifier: Modifier = Modifier) {
    val localeState by LocaleController.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = currentLanguageDisplayName(localeState.language),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDialog) {
        LanguagePickerDialog(
            current = localeState.language,
            onSelect = { lang ->
                showDialog = false
                if (lang != localeState.language) {
                    LocaleController.setLanguage(context, lang)
                    // setLanguage 内部会调 activity.recreate() 强制刷新
                }
            },
            onDismiss = { showDialog = false }
        )
    }
}

/** 弹窗: 4 个 RadioButton 选项 (跟随系统 / 简体中文 / English / 日本語) */
@Composable
private fun LanguagePickerDialog(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column {
                AppLanguage.entries.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(lang) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = lang == current,
                            onClick = { onSelect(lang) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = languageDisplayName(lang),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

/** 显示当前语言的用户友好名字 (给 Card 副标题用) */
@Composable
private fun currentLanguageDisplayName(lang: AppLanguage): String =
    stringResource(
        when (lang) {
            AppLanguage.SYSTEM -> R.string.language_system
            AppLanguage.ZH_CN -> R.string.language_zh_cn
            AppLanguage.EN -> R.string.language_en
            AppLanguage.JA -> R.string.language_ja
        }
    )

/** 弹窗里每个选项的显示名 */
@Composable
private fun languageDisplayName(lang: AppLanguage): String =
    stringResource(
        when (lang) {
            AppLanguage.SYSTEM -> R.string.language_system
            AppLanguage.ZH_CN -> R.string.language_zh_cn
            AppLanguage.EN -> R.string.language_en
            AppLanguage.JA -> R.string.language_ja
        }
    )
