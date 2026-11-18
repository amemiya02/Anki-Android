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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.media3.common.TrackSelectionOverride
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.time.formatAsString
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import kotlin.time.Duration.Companion.milliseconds
import androidx.media3.ui.R as MediaR

internal data class AttachmentPlaybackState(
    val position: Long = 0L,
    val duration: Long = 0L,
    val playing: Boolean = false,
    val speed: Float = 1f,
    val tracks: List<AttachmentAudioTrack> = emptyList(),
    val selectedTrack: TrackSelectionOverride? = null,
)

internal data class AttachmentAudioTrack(
    val name: String,
    val selection: TrackSelectionOverride,
)

private enum class PlaybackMenu {
    Settings,
    Speed,
    AudioTracks,
}

private val playbackSpeeds = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
internal fun AttachmentPlaybackControls(
    state: AttachmentPlaybackState,
    onPlayPause: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeed: (Float) -> Unit,
    onAudioTrack: (TrackSelectionOverride?) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    var menu by rememberSaveable { mutableStateOf<PlaybackMenu?>(null) }
    var scrubPosition by remember { mutableStateOf<Float?>(null) }
    val position = scrubPosition ?: state.position.toFloat()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(position.toLong().milliseconds.formatAsString(), color = foundation.colors.onBackgroundMuted, style = foundation.type.caption)
        Text(state.duration.milliseconds.formatAsString(), color = foundation.colors.onBackgroundMuted, style = foundation.type.caption)
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
        AttachmentIconButton(R.drawable.symbol_settings, MediaR.string.exo_controls_settings_description) { menu = PlaybackMenu.Settings }
    }
    val speedLabel = stringResource(MediaR.string.exo_controls_playback_speed)
    val audioLabel = stringResource(MediaR.string.exo_track_selection_title_audio)
    val speedNames = stringArrayResource(MediaR.array.exo_controls_playback_speeds)
    val speedIndex = playbackSpeeds.indexOf(state.speed)
    when (menu) {
        PlaybackMenu.Settings -> {
            val speedOption = DialogChoice(speedLabel, speedNames.getOrNull(speedIndex))
            val audioOption =
                state.tracks.takeIf { it.isNotEmpty() }?.let { tracks ->
                    val track = tracks.firstOrNull { it.selection == state.selectedTrack }
                    DialogChoice(audioLabel, track?.name ?: stringResource(MediaR.string.exo_track_selection_auto))
                }
            SingleChoiceDialog(
                title = stringResource(MediaR.string.exo_controls_settings_description),
                choices = listOfNotNull(speedOption, audioOption),
                selectedIndex = null,
                onSelect = { index -> menu = if (index == 0) PlaybackMenu.Speed else PlaybackMenu.AudioTracks },
                onDismissRequest = { menu = null },
            )
        }
        PlaybackMenu.Speed ->
            SingleChoiceDialog(
                title = speedLabel,
                choices = speedNames.map { DialogChoice(it) },
                selectedIndex = speedIndex.takeIf { it >= 0 },
                onSelect = {
                    onSpeed(playbackSpeeds[it])
                    menu = null
                },
                onDismissRequest = { menu = null },
            )
        PlaybackMenu.AudioTracks ->
            SingleChoiceDialog(
                title = audioLabel,
                choices =
                    listOf(DialogChoice(stringResource(MediaR.string.exo_track_selection_auto))) +
                        state.tracks.map { DialogChoice(it.name) },
                selectedIndex = state.tracks.indexOfFirst { it.selection == state.selectedTrack } + 1,
                onSelect = {
                    onAudioTrack(state.tracks.getOrNull(it - 1)?.selection)
                    menu = null
                },
                onDismissRequest = { menu = null },
            )
        null -> Unit
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
