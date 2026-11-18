// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.reviewer.Axis
import com.ichi2.anki.reviewer.Binding
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.DialogBody
import java.util.Locale
import kotlin.math.abs

private data class AxisReading(
    val value: Float = 0f,
    val reachedMinimum: Boolean = false,
    val reachedMaximum: Boolean = false,
) {
    fun read(value: Float): AxisReading = AxisReading(value, reachedMinimum || value == -1f, reachedMaximum || value == 1f)
}

@Composable
internal fun AxisBindingPicker(onSelect: (Binding.AxisButtonBinding) -> Unit) {
    val axes = remember { mutableStateMapOf<Axis, AxisReading>() }

    // Android routes joystick MotionEvents to the focused View. This bridge draws no UI.
    AndroidView(
        modifier = Modifier.size(1.dp),
        factory = { context ->
            View(context).apply {
                isFocusableInTouchMode = true
                setOnGenericMotionListener { _, event ->
                    if (event.actionMasked != MotionEvent.ACTION_MOVE) return@setOnGenericMotionListener false
                    for (axis in Axis.entries) {
                        val value = event.getAxisValue(axis.motionEventValue)
                        val previous = axes[axis]
                        if (previous != null || value != 0f) axes[axis] = (previous ?: AxisReading()).read(value)
                    }
                    true
                }
                post { requestFocus() }
            }
        },
    )
    if (axes.isEmpty()) {
        DialogBody(stringResource(CommonString.axis_picker_move_joystick))
    }
    for (axis in Axis.entries) {
        val reading = axes[axis] ?: continue
        AxisBindingRow(axis, reading, onSelect)
    }
}

@Composable
private fun AxisBindingRow(
    axis: Axis,
    reading: AxisReading,
    onSelect: (Binding.AxisButtonBinding) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val negative = Binding.AxisButtonBinding(axis, -1f)
    val positive = Binding.AxisButtonBinding(axis, 1f)
    Text(axis.toString(), style = foundation.type.rowTitle, color = foundation.colors.onBackground)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AxisButton(negative, reading.reachedMinimum, onSelect)
        Box(Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
            val color = if (abs(reading.value) == 1f) foundation.colors.emphasis else foundation.colors.outline
            Canvas(Modifier.fillMaxSize()) {
                val displacement = size.width * reading.value.coerceIn(-1f, 1f) / 2f
                drawRect(
                    color,
                    topLeft = Offset(center.x + minOf(0f, displacement), center.y - 2.dp.toPx()),
                    size = Size(abs(displacement), 4.dp.toPx()),
                )
            }
            Text(
                String.format(Locale.getDefault(), "%.3f", reading.value),
                style = foundation.type.body,
                color = foundation.colors.onBackground,
            )
        }
        AxisButton(positive, reading.reachedMaximum, onSelect)
    }
    SettingsDivider()
}

@Composable
private fun AxisButton(
    binding: Binding.AxisButtonBinding,
    enabled: Boolean,
    onSelect: (Binding.AxisButtonBinding) -> Unit,
) {
    val label = binding.toDisplayString(LocalContext.current)
    IconButton(onClick = { onSelect(binding) }, enabled = enabled) {
        Icon(painterResource(R.drawable.symbol_add), "${stringResource(CommonString.binding_add_axis)}: $label")
    }
}
