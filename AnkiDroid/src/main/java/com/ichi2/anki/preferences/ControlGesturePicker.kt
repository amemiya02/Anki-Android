// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.hardware.SensorManager
import android.view.GestureDetector
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ichi2.anki.CommonString
import com.ichi2.anki.cardviewer.Gesture
import com.ichi2.anki.cardviewer.TapGestureMode
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.reviewer.GestureMapper
import com.ichi2.compose.theme.foundation
import com.ichi2.ui.OnGestureListener
import com.squareup.seismic.ShakeDetector

internal val whiteboardGestures =
    listOf(Gesture.TWO_FINGER_TAP, Gesture.THREE_FINGER_TAP, Gesture.FOUR_FINGER_TAP, Gesture.SHAKE)

private val tapTargets =
    mapOf(
        Gesture.TAP_TOP_LEFT to Offset(0.12f, 0.12f),
        Gesture.TAP_TOP to Offset(0.5f, 0.12f),
        Gesture.TAP_TOP_RIGHT to Offset(0.88f, 0.12f),
        Gesture.TAP_LEFT to Offset(0.12f, 0.5f),
        Gesture.TAP_CENTER to Offset(0.5f, 0.5f),
        Gesture.TAP_RIGHT to Offset(0.88f, 0.5f),
        Gesture.TAP_BOTTOM_LEFT to Offset(0.12f, 0.88f),
        Gesture.TAP_BOTTOM to Offset(0.5f, 0.88f),
        Gesture.TAP_BOTTOM_RIGHT to Offset(0.88f, 0.88f),
    )

private val fourPointTaps = setOf(Gesture.TAP_TOP, Gesture.TAP_LEFT, Gesture.TAP_RIGHT, Gesture.TAP_BOTTOM)

internal fun TapGestureMode.availableGestures(): List<Gesture> =
    Gesture.entries.filter { this == TapGestureMode.NINE_POINT || it !in tapTargets || it in fourPointTaps }

@Composable
internal fun rememberControlGestureMapper(): GestureMapper {
    val context = LocalContext.current
    return remember(context) { GestureMapper().apply { init(context.sharedPrefs()) } }
}

@Composable
internal fun DetectShake(onShake: () -> Unit) {
    val context = LocalContext.current
    val onShakeCurrent by rememberUpdatedState(onShake)
    DisposableEffect(context) {
        val detector = ShakeDetector { onShakeCurrent() }
        ContextCompat.getSystemService(context, SensorManager::class.java)?.let {
            detector.start(it, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { detector.stop() }
    }
}

/** Draws the touch area in physical screen coordinates so directions stay the same in RTL. */
@Composable
internal fun GesturePad(
    mapper: GestureMapper,
    gesture: Gesture?,
    onGesture: (Gesture) -> Unit,
) {
    val context = LocalContext.current
    val onGestureCurrent by rememberUpdatedState(onGesture)
    var size by remember { mutableStateOf(IntSize.Zero) }
    val detector =
        remember(context, mapper) {
            GestureDetector(context, OnGestureListener({ size }, mapper) { onGestureCurrent(it) })
        }
    val colors = MaterialTheme.foundation.colors
    val description = stringResource(CommonString.binding_add_gesture)
    Canvas(
        Modifier
            .widthIn(max = 250.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { contentDescription = description }
            .onSizeChanged { size = it }
            .pointerInteropFilter(detector::onTouchEvent),
    ) {
        drawRect(
            colors.outline,
            style =
                androidx.compose.ui.graphics.drawscope
                    .Stroke(1.dp.toPx()),
        )
        for ((tap, location) in tapTargets) {
            if (mapper.tapGestureMode == TapGestureMode.FOUR_POINT && tap !in fourPointTaps) continue
            drawCircle(
                color = if (gesture == tap) colors.emphasis else colors.onBackgroundMuted,
                radius = if (gesture == tap) 9.dp.toPx() else 4.dp.toPx(),
                center = Offset(this.size.width * location.x, this.size.height * location.y),
            )
        }
        val direction =
            when (gesture) {
                Gesture.SWIPE_UP -> Offset(0f, -1f)
                Gesture.SWIPE_DOWN -> Offset(0f, 1f)
                Gesture.SWIPE_LEFT -> Offset(-1f, 0f)
                Gesture.SWIPE_RIGHT -> Offset(1f, 0f)
                else -> null
            }
        if (direction != null) {
            val tip = center + direction * (this.size.minDimension * 0.28f)
            val tail = center - direction * (this.size.minDimension * 0.28f)
            val head = tip - direction * 18.dp.toPx()
            val wing = Offset(-direction.y, direction.x) * 12.dp.toPx()
            val stroke = 3.dp.toPx()
            drawLine(colors.emphasis, tail, tip, strokeWidth = stroke)
            drawLine(colors.emphasis, tip, head + wing, strokeWidth = stroke)
            drawLine(colors.emphasis, tip, head - wing, strokeWidth = stroke)
        }
    }
}
