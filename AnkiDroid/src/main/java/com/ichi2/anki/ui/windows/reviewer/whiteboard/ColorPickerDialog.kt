// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Divyansh Kushwaha <thedroiddiv@gmail.com>

package com.ichi2.anki.ui.windows.reviewer.whiteboard

import android.content.Context
import androidx.annotation.ColorInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import android.graphics.Color as AndroidColor

fun Context.showColorPickerDialog(
    @ColorInt initialColor: Int,
    onColorSelected: (Int) -> Unit,
) {
    createPaperDialog(this) { dialog ->
        val start = remember(initialColor) { FloatArray(3).also { AndroidColor.colorToHSV(initialColor, it) } }
        var hue by remember { mutableFloatStateOf(start[0]) }
        var saturation by remember { mutableFloatStateOf(start[1]) }
        var brightness by remember { mutableFloatStateOf(start[2]) }
        var alpha by remember { mutableFloatStateOf(AndroidColor.alpha(initialColor) / 255f) }
        var brightenFirstPick by remember { androidx.compose.runtime.mutableStateOf(brightness <= 0.05f) }
        PaperDialog(
            title = stringResource(CommonString.choose_color),
            onDismissRequest = dialog::dismiss,
            actions =
                listOf(
                    DialogAction(stringResource(CommonString.dialog_cancel), dialog::dismiss),
                    DialogAction(stringResource(CommonString.dialog_ok), {
                        onColorSelected(AndroidColor.HSVToColor((alpha * 255).roundToInt(), floatArrayOf(hue, saturation, brightness)))
                        dialog.dismiss()
                    }, DialogActionStyle.Filled),
                ),
        ) {
            ColorWheel(hue, saturation, brightness, alpha) { h, s ->
                hue = h
                saturation = s
                if (brightenFirstPick) brightness = 1f
                brightenFirstPick = false
            }
            val brightnessLabel = stringResource(CommonString.paper_picker_brightness)
            Text(brightnessLabel)
            Slider(
                brightness,
                onValueChange = {
                    brightness = it
                    brightenFirstPick = false
                },
                modifier = Modifier.semantics { contentDescription = brightnessLabel },
            )
            val alphaLabel = stringResource(CommonString.paper_picker_opacity)
            Text(alphaLabel)
            Slider(alpha, onValueChange = { alpha = it }, modifier = Modifier.semantics { contentDescription = alphaLabel })
        }
    }.show()
}

@Composable
private fun ColorWheel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    alpha: Float,
    onPick: (Float, Float) -> Unit,
) {
    val pick by rememberUpdatedState(onPick)
    val wheelLabel = stringResource(CommonString.paper_picker_wheel)
    val increaseHue = stringResource(CommonString.paper_picker_next_hue)
    val decreaseHue = stringResource(CommonString.paper_picker_previous_hue)
    val moreSaturation = stringResource(CommonString.paper_picker_more_saturation)
    val lessSaturation = stringResource(CommonString.paper_picker_less_saturation)
    val colors = remember { (0..6).map { Color.hsv((it % 6) * 60f, 1f, 1f) } }
    Canvas(
        Modifier
            .sizeIn(maxWidth = 300.dp, maxHeight = 300.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics {
                contentDescription = wheelLabel
                stateDescription =
                    String.format(
                        java.util.Locale.ROOT,
                        "#%08X",
                        AndroidColor.HSVToColor((alpha * 255).roundToInt(), floatArrayOf(hue, saturation, brightness)),
                    )
                customActions =
                    listOf(
                        CustomAccessibilityAction(increaseHue) {
                            pick((hue + 15f) % 360f, saturation)
                            true
                        },
                        CustomAccessibilityAction(decreaseHue) {
                            pick((hue + 345f) % 360f, saturation)
                            true
                        },
                        CustomAccessibilityAction(moreSaturation) {
                            pick(hue, (saturation + 0.1f).coerceAtMost(1f))
                            true
                        },
                        CustomAccessibilityAction(lessSaturation) {
                            pick(hue, (saturation - 0.1f).coerceAtLeast(0f))
                            true
                        },
                    )
            }.pointerInput(Unit) {
                fun select(point: Offset) {
                    val radius = minOf(size.width, size.height) / 2f
                    if (radius <= 0f) return
                    val delta = point - Offset(size.width / 2f, size.height / 2f)
                    val angle = ((atan2(delta.y, delta.x) * 180f / PI).toFloat() + 360f) % 360f
                    pick(angle, (delta.getDistance() / radius).coerceIn(0f, 1f))
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    select(down.position)
                    do {
                        val finger = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        finger.consume()
                        select(finger.position)
                    } while (finger.pressed)
                }
            },
    ) {
        val radius = size.minDimension / 2f
        drawCircle(Brush.sweepGradient(colors, center), radius)
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.Transparent), center, radius), radius)
        val angle = hue * PI / 180
        val marker = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * saturation * (radius - 8.dp.toPx()).coerceAtLeast(0f)
        drawCircle(Color.hsv(hue, saturation, brightness, alpha), 7.dp.toPx(), marker)
        drawCircle(Color.White, 7.dp.toPx(), marker, style = Stroke(3.dp.toPx()))
        drawCircle(Color.Black, 8.5.dp.toPx(), marker, style = Stroke(1.dp.toPx()))
    }
}
