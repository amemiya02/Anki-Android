// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer.audiorecord

import android.text.format.DateUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.compose.theme.foundation
import kotlin.math.abs
import com.ichi2.anki.common.android.R as CommonR

@Composable
internal fun PronunciationControls(
    recording: Boolean,
    elapsedSeconds: Long,
    playbackVisible: Boolean,
    playing: Boolean,
    progress: Float,
    replayCount: Int,
    onStart: () -> Boolean,
    onFinish: () -> Unit,
    onCancelRecording: () -> Unit,
    onPlay: () -> Unit,
    onClear: () -> Unit,
) {
    var showingGestures by remember { mutableStateOf(false) }
    val paper = MaterialTheme.foundation
    val start by rememberUpdatedState(onStart)
    val finish by rememberUpdatedState(onFinish)
    val cancel by rememberUpdatedState(onCancelRecording)
    val isRecording by rememberUpdatedState(recording)

    fun activate(): Boolean {
        if (!isRecording) return start()
        finish()
        return true
    }
    val windowSize = LocalWindowInfo.current.containerSize
    val haptics = LocalHapticFeedback.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = paper.spacing.touchTarget).background(paper.colors.background),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        if (recording) {
            Text(
                DateUtils.formatElapsedTime(elapsedSeconds),
                Modifier.weight(1f),
                style = paper.type.rowTitle,
                color = paper.colors.onBackground,
            )
            if (showingGestures) {
                Text(
                    stringResource(CommonString.slide_to_cancel),
                    Modifier.weight(1f),
                    style = paper.type.caption,
                    color = paper.colors.onBackgroundMuted,
                )
                Icon(painterResource(R.drawable.symbol_lock), null, tint = paper.colors.onBackgroundMuted)
            }
            IconButton(onClick = onCancelRecording) {
                Icon(
                    painterResource(CommonR.drawable.symbol_close),
                    stringResource(CommonString.dialog_cancel),
                    tint = paper.colors.onBackground,
                )
            }
        } else if (playbackVisible) {
            val rotation = remember { Animatable(0f) }
            LaunchedEffect(replayCount) {
                if (replayCount > 0) {
                    rotation.snapTo(0f)
                    rotation.animateTo(-360f, tween(400))
                }
            }
            IconButton(onClick = onPlay) {
                Icon(
                    painterResource(if (playing) R.drawable.symbol_replay else R.drawable.symbol_play_arrow),
                    stringResource(if (playing) CommonString.replay_voice else CommonString.play_recording),
                    Modifier.rotate(rotation.value),
                    tint = paper.colors.onBackground,
                )
            }
            LinearProgressIndicator(progress = {
                progress.coerceIn(0f, 1f)
            }, modifier = Modifier.weight(1f), color = paper.colors.primary, trackColor = paper.colors.progressTrack)
            IconButton(onClick = onClear) {
                Icon(
                    painterResource(CommonR.drawable.symbol_close),
                    stringResource(CommonString.dialog_cancel),
                    tint = paper.colors.onBackground,
                )
            }
        }
        // Keep this pointer target in composition when recording starts. A short tap locks,
        // holding and releasing finishes, and dragging up/left locks or discards the take.
        Box(
            Modifier
                .size(paper.spacing.touchTarget)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    onClick { activate() }
                }.onKeyEvent {
                    if (it.key != Key.Enter && it.key != Key.DirectionCenter && it.key != Key.Spacebar) return@onKeyEvent false
                    if (it.type == KeyEventType.KeyUp) activate()
                    true
                }.focusable()
                .pointerInput(windowSize) {
                    val cancelDistance = maxOf(60.dp.toPx(), windowSize.width * .25f)
                    val lockDistance = maxOf(80.dp.toPx(), windowSize.height * .25f)
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        if (isRecording) {
                            finish()
                            return@awaitEachGesture
                        }
                        if (!start()) return@awaitEachGesture
                        val released =
                            withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                val up = waitForUpOrCancellation()
                                if (up == null) cancel() else up.consume()
                                true
                            }
                        if (released == true) return@awaitEachGesture
                        showingGestures = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        while (true) {
                            val pointer = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            val delta = pointer.position - down.position
                            pointer.consume()
                            if (-delta.x >= cancelDistance && abs(delta.x) > abs(delta.y)) {
                                cancel()
                                break
                            }
                            if (-delta.y >= lockDistance && abs(delta.y) > abs(delta.x)) break
                            if (!pointer.pressed) {
                                finish()
                                break
                            }
                        }
                        showingGestures = false
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(if (recording) R.drawable.symbol_stop else CommonR.drawable.symbol_mic),
                stringResource(if (recording) CommonString.stop_recording else CommonString.record_voice),
                tint = if (recording) paper.colors.emphasis else paper.colors.onBackground,
            )
        }
    }
}
