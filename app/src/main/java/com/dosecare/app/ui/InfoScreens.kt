package com.dosecare.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * v0.8a Phase F7: 关于 / 开源许可证 / 联系作者 Screen
 *
 * 全部走 Scaffold + TopAppBar(back), 内容用 Card 简单铺.
 * 设计参考: 简洁单色, 卡片分层, 关键信息一目了然.
 *
 * v0.8d 增强: 加 "联系作者" mailto: 链接 + "仓库源代码" GitHub 链接
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "DoseCare",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("v0.8d · Debug", style = MaterialTheme.typography.bodyMedium)
                    Text("com.dosecare.app · AGPL-3.0", style = MaterialTheme.typography.labelSmall)
                }
            }

            // ====== v0.8d 新增: 仓库源代码 + 联系作者 (快速入口) ======
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "🔗 仓库源代码",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "https://github.com/dosecare/app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                runCatching { uriHandler.openUri("https://github.com/dosecare/app") }
                            }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "README / CHANGELOG / CONTRIBUTING / SECURITY 都在仓库根目录," +
                            " 提交 Issue 之前请先阅读 CONTRIBUTING.md。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "💌 联系作者",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "daisukikiki01@gmail.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                runCatching { uriHandler.openUri("mailto:daisukikiki01@gmail.com") }
                            }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "点上面邮箱直接发邮件 (默认邮件客户端)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("📚 数据源", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("• DrugCatalog: AGNP 2017 + FDA DailyMed + Flockhart CYP Table", style = MaterialTheme.typography.bodySmall)
                    Text("• PK 参数: 1/2/3 房室口服模型 (按 drug.pkModel 自动选)", style = MaterialTheme.typography.bodySmall)
                    Text("• 治疗窗: AGNP 2017 TDM 共识指南", style = MaterialTheme.typography.bodySmall)
                    Text("• 相互作用: RuleEngine 6 类自研规则", style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚠️ 局限性", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("• 1 房室模型对部分药不够准确(苯妥英非线性代谢、锂肾清除)", style = MaterialTheme.typography.bodySmall)
                    Text("• 实际浓度受 CYP 基因型/肝肾/食物/合并用药影响,2-3 倍偏差属正常", style = MaterialTheme.typography.bodySmall)
                    Text("• 不替代医师面诊; 急性不良反应请立即就医", style = MaterialTheme.typography.bodySmall)
                }
            }

            // ====== v0.8b: 开源许可证 (合并) ======
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "📜 开源许可证",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("GNU Affero General Public License v3.0 (AGPL-3.0)", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Copyright (C) 2024-2026 DoseCare contributors\n" +
                            "This program is free software: you can redistribute it and/or modify " +
                            "it under the terms of the GNU Affero General Public License as published by " +
                            "the Free Software Foundation, either version 3 of the License, or " +
                            "(at your option) any later version.\n\n" +
                            "This program is distributed in the hope that it will be useful, " +
                            "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
                            "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "📦 主要依赖",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    LicenseRow("Jetpack Compose", "Apache 2.0", "https://www.apache.org/licenses/LICENSE-2.0")
                    LicenseRow("Room", "Apache 2.0", null)
                    LicenseRow("Hilt", "Apache 2.0", null)
                    LicenseRow("SQLCipher (net.zetetic:sqlcipher-android)", "BSD-style", "https://www.zetetic.net/sqlcipher/license/")
                    LicenseRow("Kotlinx Serialization", "Apache 2.0", null)
                    LicenseRow("Kotlinx Coroutines", "Apache 2.0", null)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "📊 数据源许可证",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    LicenseRow("AGNP 2017 TDM 共识", "CC BY-NC-SA 4.0", "https://doi.org/10.1007/s40262-017-0609-1")
                    LicenseRow("FDA DailyMed", "Public Domain", "https://dailymed.nlm.nih.gov")
                    LicenseRow("Flockhart CYP Table", "Public Domain (学术)", "https://drug-interactions.medicine.iu.edu")
                }
            }
        }
    }
}

@Composable
private fun LicenseRow(name: String, license: String, url: String?) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(license, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (url != null) {
            Text(url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("联系作者") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("🐛 Bug 反馈 / 功能建议", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("GitHub Issues:", style = MaterialTheme.typography.labelMedium)
                    Text(
                        "https://github.com/dosecare/app/issues",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                runCatching { uriHandler.openUri("https://github.com/dosecare/app/issues") }
                            }
                    )
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("💌 邮件", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "daisukikiki01@gmail.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                runCatching { uriHandler.openUri("mailto:daisukikiki01@gmail.com") }
                            }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "点上面直接跳到默认邮件客户端",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚕️ 临床数据更正", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("欢迎药师/医师朋友指出 PK 参数、治疗窗、CYP 数据的错误," +
                            "可附 PMID/官方指南, 我们会尽快复核。",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
