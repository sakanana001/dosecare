package com.dosecare.app.ui.reminder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.dosecare.app.R
import com.dosecare.app.reminder.ReminderPermissions
import com.dosecare.app.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * v0.9f 提醒设置卡片
 *
 * - 显示当前权限状态 (通知 + 精确闹钟)
 * - 缺失权限时显示对应 "前往设置" 按钮
 * - 提供 "测试提醒 (5s 后)" 按钮 (验证通知流跑通)
 */
@Composable
fun ReminderSettingsCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: ReminderSettingsViewModel = hiltViewModel()

    var notifGranted by remember { mutableStateOf(ReminderPermissions.canPostNotifications(context)) }
    var exactAlarmGranted by remember { mutableStateOf(ReminderPermissions.canScheduleExactAlarms(context)) }
    var testStatus by remember { mutableStateOf<String?>(null) }

    // 申请通知权限 launcher (API 33+)
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted || ReminderPermissions.canPostNotifications(context)
    }

    // 进入设置页时刷一次状态 (用户从系统设置返回时)
    LaunchedEffect(Unit) {
        notifGranted = ReminderPermissions.canPostNotifications(context)
        exactAlarmGranted = ReminderPermissions.canScheduleExactAlarms(context)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (notifGranted && exactAlarmGranted) Icons.Default.NotificationsActive
                    else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.reminder_settings_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.reminder_settings_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // 通知权限状态
            PermissionRow(
                granted = notifGranted,
                grantedText = stringResource(R.string.reminder_perm_notif_granted),
                deniedText = stringResource(R.string.reminder_perm_notif_denied),
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            // 精确闹钟权限状态
            PermissionRow(
                granted = exactAlarmGranted,
                grantedText = stringResource(R.string.reminder_perm_alarm_granted),
                deniedText = stringResource(R.string.reminder_perm_alarm_denied),
                onRequest = {
                    val intent = ReminderPermissions.buildExactAlarmSettingsIntent(context)
                    try {
                        ContextCompat.startActivity(context, intent, null)
                    } catch (e: Exception) {
                        // fallback
                        val fallback = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.parse("package:${context.packageName}")
                        }
                        ContextCompat.startActivity(context, fallback, null)
                    }
                }
            )

            // 测试按钮 — 5 秒后发一条
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    scope.launch {
                        val ok = viewModel.scheduleTestReminder()
                        testStatus = if (ok) {
                            context.getString(R.string.reminder_test_scheduled)
                        } else {
                            context.getString(R.string.reminder_test_failed)
                        }
                    }
                },
                enabled = notifGranted && exactAlarmGranted
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.reminder_test_button))
            }

            testStatus?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // 缺权限时底部提示
            if (!notifGranted || !exactAlarmGranted) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.height(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.reminder_perm_warn),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    granted: Boolean,
    grantedText: String,
    deniedText: String,
    onRequest: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (granted) grantedText else deniedText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        if (!granted) {
            Button(onClick = onRequest) {
                Text(stringResource(R.string.reminder_perm_open))
            }
        }
    }
}

/**
 * v0.9f ReminderSettings ViewModel
 * 注入 ReminderScheduler 供测试按钮调用
 */
@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    private val scheduler: ReminderScheduler
) : ViewModel() {
    /**
     * 测试: 5 秒后发一条提醒 (drugId/test 标记), 验证通知流跑通
     * 返回 true 表示已 enqueue
     */
    fun scheduleTestReminder(): Boolean {
        return try {
            scheduler.scheduleTestReminder(
                drugId = "test_drug_${System.currentTimeMillis()}",
                drugName = "测试药",
                slotTime = "now"
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
