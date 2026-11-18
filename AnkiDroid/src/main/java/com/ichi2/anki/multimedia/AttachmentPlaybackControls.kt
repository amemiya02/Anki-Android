// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.multimedia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.time.formatAsString
import com.ichi2.compose.theme.foundation
import kotlin.time.Duration.Companion.milliseconds
import androidx.media3.ui.R as MediaR

internal data class AttachmentPlaybackState(
    val position: Long = 0L,
    val duration: Long = 0L,
    val playing: Boolean = false,
)

@Composable
internal fun AttachmentPlaybackControls(
    state: AttachmentPlaybackState,
    onPlayPause: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    var scrubPosition by remember { mutableStateOf<Float?>(null) }
    val position = scrubPosition ?: state.position.toFloat()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(position.toLong().milliseconds.formatAsString(), style = MaterialTheme.foundation.type.caption)
        Text(state.duration.milliseconds.formatAsString(), style = MaterialTheme.foundation.type.caption)
    }
    Slider(
        modifier = Modifier.fillMaxWidth(),
        value = position,
        onValueChange = { scrubPosition = it },
        onValueChangeFinished = {
            scrubPosition?.let { onSeek(it.toLong()) }
            scrubPosition = null
        },
        valueRange = 0f..state.duration.toFloat().coerceAtLeast(1f),
        enabled = state.duration > 0,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        AttachmentIconButton(R.drawable.symbol_replay_5, R.string.note_editor_audio_back_five_seconds, onClick = onBack)
        AttachmentIconButton(
            if (state.playing) R.drawable.symbol_pause else R.drawable.symbol_play_arrow,
            if (state.playing) R.string.note_editor_audio_pause_playback else CommonString.play_recording,
            onClick = onPlayPause,
        )
        AttachmentIconButton(R.drawable.symbol_fast_forward, MediaR.string.exo_controls_fastforward_description, onClick = onForward)
    }
}

@Composable
internal fun AttachmentIconButton(
    icon: Int,
    label: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(enabled = enabled, onClick = onClick) {
        Icon(painterResource(icon), stringResource(label))
    }
}

@Composable
internal fun MediaDoneButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = foundation.spacing.touchTarget),
        shape = MaterialTheme.foundation.shapes.button,
        enabled = enabled,
        colors =
            ButtonDefaults.buttonColors(
                disabledContainerColor = foundation.colors.progressTrack,
                disabledContentColor = foundation.colors.onBackground,
            ),
    ) {
        Text(stringResource(CommonString.multimedia_editor_field_editing_done))
    }
}
