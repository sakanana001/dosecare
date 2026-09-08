package com.dosecare.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dosecare.app.R

/**
 * 主题色选择器 (v0.8d 新增)
 *
 * - SV 圆盘: 横向 saturation (0-1, 白→纯色), 纵向 value (0-1, 纯色→黑)
 * - Hue 滑块: 0-360° 取色
 * - HEX 输入: 直接敲 #RRGGBB
 * - 预览圆 + OK/Cancel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val initialHsv = remember(initial) {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (initial.red * 255).toInt(),
            (initial.green * 255).toInt(),
            (initial.blue * 255).toInt(),
            hsv
        )
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    val color = Color.hsv(hue, sat, value)
    var hexInput by remember(color) { mutableStateOf("#%06X".format(color.toArgb() and 0xFFFFFF)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.color_picker_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 预览 + 当前色值
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            hexInput,
                            style = MaterialTheme.typography.titleMedium
                        )
                        // TODO(v0.9b): "R G B" label 需要 i18n
                        Text(
                            "R ${color.red.times(255).toInt()}  " +
                                "G ${color.green.times(255).toInt()}  " +
                                "B ${color.blue.times(255).toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // SV 圆盘 (240dp)
                val padSize: Dp = 240.dp
                SaturationValuePicker(
                    hue = hue,
                    sat = sat,
                    value = value,
                    padSize = padSize,
                    modifier = Modifier
                        .size(padSize)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
                    onChange = { s, v ->
                        sat = s
                        value = v
                    }
                )

                // Hue 滑块
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.color_picker_hue), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(40.dp))
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.hsv(hue, 1f, 1f),
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        ),
                        track = { HueTrack() }
                    )
                }

                // Value 滑块
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.color_picker_value), style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(40.dp))
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.hsv(hue, sat, value),
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        ),
                        track = { ValueTrack(hue, sat) }
                    )
                }

                // HEX 输入
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input.uppercase().take(7)
                        val parsed = parseHexColor(input)
                        if (parsed != null) {
                            val hsv = FloatArray(3)
                            android.graphics.Color.RGBToHSV(
                                (parsed.red * 255).toInt(),
                                (parsed.green * 255).toInt(),
                                (parsed.blue * 255).toInt(),
                                hsv
                            )
                            hue = hsv[0]
                            sat = hsv[1]
                            value = hsv[2]
                        }
                    },
                    label = { Text("HEX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(color) }) { Text(stringResource(R.string.common_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

/**
 * Saturation-Value 2D 取色器
 * - 横轴 S: 0 (白) → 1 (纯色)
 * - 纵轴 V: 0 (黑) 在下 → 1 (纯色) 在上
 */
@Composable
private fun SaturationValuePicker(
    hue: Float,
    sat: Float,
    value: Float,
    padSize: Dp,
    modifier: Modifier,
    onChange: (Float, Float) -> Unit
) {
    val sizePx = with(LocalDensity.current) { padSize.toPx() }
    Box(
        modifier = modifier
            .pointerInput(hue) {
                awaitEachGesture {
                    val sizeF = size.width.toFloat().coerceAtLeast(1f)
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    var s = (firstDown.position.x / sizeF).coerceIn(0f, 1f)
                    var v = (1f - firstDown.position.y / sizeF).coerceIn(0f, 1f)
                    onChange(s, v)
                    firstDown.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        s = (change.position.x / sizeF).coerceIn(0f, 1f)
                        v = (1f - change.position.y / sizeF).coerceIn(0f, 1f)
                        onChange(s, v)
                        change.consume()
                    }
                }
            }
    ) {
        // Layer 1: hue 纯色填底
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.hsv(hue, 1f, 1f))
        )
        // Layer 2: 横向 white → transparent (左白右纯色, 降饱和)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
        )
        // Layer 3: 纵向 transparent → black (上纯色下黑, 降亮度)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        )
        // 选中点指示
        val padSizeValue = padSize.value  // Float, e.g. 240f
        val indicatorSizeDp = 20.dp
        val halfIndicatorDp = 10.dp
        Box(
            modifier = Modifier
                .offset(
                    x = (sat * padSizeValue).dp - halfIndicatorDp,
                    y = ((1f - value) * padSizeValue).dp - halfIndicatorDp
                )
                .size(indicatorSizeDp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

/** Hue 滑块轨道: 横向 rainbow 渐变 */
@Composable
private fun HueTrack() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.hsv(0f, 1f, 1f),
                        Color.hsv(60f, 1f, 1f),
                        Color.hsv(120f, 1f, 1f),
                        Color.hsv(180f, 1f, 1f),
                        Color.hsv(240f, 1f, 1f),
                        Color.hsv(300f, 1f, 1f),
                        Color.hsv(360f, 1f, 1f),
                    )
                )
            )
    )
}

/** Value 滑块轨道: 当前 hue+sat, 从黑渐变到纯色 */
@Composable
private fun ValueTrack(hue: Float, sat: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.hsv(hue, sat, 0f),
                        Color.hsv(hue, sat, 1f),
                    )
                )
            )
    )
}

/**
 * 解析 "#RRGGBB" 或 "RRGGBB" 字符串为 Compose Color
 * 失败返回 null
 */
private fun parseHexColor(input: String): Color? {
    val cleaned = input.removePrefix("#").trim()
    if (cleaned.length != 6) return null
    return try {
        val argb = 0xFF000000.toInt() or cleaned.toLong(16).toInt()
        Color(argb)
    } catch (_: NumberFormatException) {
        null
    }
}
