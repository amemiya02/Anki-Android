// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.multimedia

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.time.formatAsString
import com.ichi2.anki.preferences.SettingsSectionTitle
import com.ichi2.anki.recorder.PlayerState
import com.ichi2.anki.recorder.RecorderState
import com.ichi2.compose.theme.foundation
import kotlin.time.Duration
import com.ichi2.anki.common.android.R as CommonR

internal data class AudioRecordingScreenState(
    val allowed: Boolean,
    val recorder: RecorderState,
    val player: PlayerState,
    val elapsed: Duration,
    val position: Duration,
    val waveform: List<Int>,
    val fields: List<String>,
)

@Composable
internal fun AudioRecordingScreen(
    state: AudioRecordingScreenState,
    onRecord: () -> Unit,
    onHoldRelease: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onPlay: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onDone: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val recorded = state.recorder is RecorderState.Recorded
    val active = state.recorder == RecorderState.Recording || state.recorder == RecorderState.Paused
    val player = state.player as? PlayerState.Ready
    val playing = player?.isPlaying == true
    Column(
        Modifier
            .fillMaxSize()
            .background(
                foundation.colors.background,
            ).padding(horizontal = foundation.spacing.screenMargin, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.allowed) {
                if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    RecordingWaveform(state.waveform)
                }
                Text(
                    (if (recorded) state.position else state.elapsed).formatAsString(),
                    style = foundation.type.body,
                    color = foundation.colors.onBackground,
                )
                if (recorded) {
                    val position = state.position.inWholeMilliseconds.toFloat()
                    val duration =
                        player
                            ?.duration
                            ?.inWholeMilliseconds
                            ?.toFloat()
                            ?.coerceAtLeast(1f) ?: 1f
                    LinearProgressIndicator(progress = { (position / duration).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        AttachmentIconButton(
                            R.drawable.symbol_replay_5,
                            R.string.note_editor_audio_back_five_seconds,
                            enabled = playing,
                            onClick = onBack,
                        )
                        AttachmentIconButton(
                            if (playing) R.drawable.symbol_pause else R.drawable.symbol_play_arrow,
                            if (playing) R.string.note_editor_audio_pause_playback else CommonString.play_recording,
                            onClick = onPlay,
                        )
                        AttachmentIconButton(
                            R.drawable.symbol_forward_5,
                            R.string.note_editor_audio_forward_five_seconds,
                            enabled = playing,
                            onClick = onForward,
                        )
                        AttachmentIconButton(CommonR.drawable.symbol_delete, CommonString.discard, onClick = onDiscard)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AttachmentIconButton(CommonR.drawable.symbol_delete, CommonString.discard, enabled = active, onClick = onDiscard)
                        RecordingButton(state.recorder, onRecord, onHoldRelease)
                        AttachmentIconButton(R.drawable.symbol_save, CommonString.save_voice, enabled = active, onClick = onSave)
                    }
                }
                if (state.fields.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingsSectionTitle(stringResource(CommonString.audio_recording_field_list))
                        state.fields.forEach { Text(it, style = foundation.type.body, color = foundation.colors.onBackground) }
                    }
                }
            }
        }
        MediaControlsAnchor()
        MediaDoneButton(enabled = recorded, onClick = onDone)
    }
}

@Composable
private fun RecordingButton(
    recorder: RecorderState,
    onRecord: () -> Unit,
    onHoldRelease: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val startOrPause by rememberUpdatedState(onRecord)
    val save by rememberUpdatedState(onHoldRelease)
    val recording = recorder == RecorderState.Recording
    val label =
        stringResource(
            when (recorder) {
                RecorderState.Recording -> R.string.note_editor_audio_pause_recording
                RecorderState.Paused -> R.string.note_editor_audio_resume_recording
                else -> CommonString.record_voice
            },
        )
    Box(
        Modifier
            .size(56.dp)
            .clip(foundation.shapes.button)
            .background(foundation.colors.primary)
            .onKeyEvent {
                if (it.type == KeyEventType.KeyUp && (it.key == Key.Enter || it.key == Key.Spacebar || it.key == Key.DirectionCenter)) {
                    startOrPause()
                    true
                } else {
                    false
                }
            }.focusable()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    startOrPause()
                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        up.consume()
                        if (up.uptimeMillis - down.uptimeMillis >= viewConfiguration.longPressTimeoutMillis) save()
                    }
                }
            }.semantics {
                role = Role.Button
                contentDescription = label
                onClick {
                    startOrPause()
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(if (recording) R.drawable.symbol_pause else R.drawable.symbol_radio_button_checked),
            contentDescription = null,
            tint = foundation.colors.onPrimary,
        )
    }
}

@Composable
private fun RecordingWaveform(amplitudes: List<Int>) {
    val foundation = MaterialTheme.foundation
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(foundation.shapes.button)
            .background(foundation.colors.progressTrack),
    ) {
        val gap = 2.dp.toPx()
        val width = 3.dp.toPx()
        val count = (size.width / (gap + width)).toInt()
        amplitudes.takeLast(count).forEachIndexed { index, amplitude ->
            val height = (amplitude / 70f).coerceIn(gap, size.height)
            drawRoundRect(
                color = foundation.colors.emphasis,
                topLeft = Offset(index * (width + gap), (size.height - height) / 2),
                size = Size(width, height),
                cornerRadius = CornerRadius(gap / 2),
            )
        }
    }
}
