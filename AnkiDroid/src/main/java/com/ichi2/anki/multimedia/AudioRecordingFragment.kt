// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.multimedia

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.compat.CompatHelper.Companion.compat
import com.ichi2.anki.compat.USAGE_TOUCH
import com.ichi2.anki.noteeditor.AudioRecordingViewModel
import com.ichi2.anki.noteeditor.AudioRecordingViewModel.Failure
import com.ichi2.anki.preferences.settingsComposeView
import com.ichi2.anki.recorder.PlayerState
import com.ichi2.anki.recorder.RecorderState
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.utils.ext.collectInViewLifecycle
import com.ichi2.utils.FileUtil
import com.ichi2.utils.Permissions
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds

class AudioRecordingFragment : MultimediaFragment() {
    private val recordingViewModel: AudioRecordingViewModel by viewModels()
    private var allowedToRecord by mutableStateOf(false)
    private val waveform = mutableStateListOf<Int>()

    override val title: String get() = getString(CommonString.multimedia_editor_field_editing_audio)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ankiCacheDirectory = FileUtil.getAnkiCacheDirectory(requireContext(), "temp-media")
        if (ankiCacheDirectory == null) {
            Timber.e("createUI() failed to get cache directory")
            showErrorDialog(errorMessage = resources.getString(CommonString.multimedia_editor_failed))
            return
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (view == null) return@registerForActivityResult
            allowedToRecord = isGranted
            if (!isGranted) showErrorDialog(getString(CommonString.multimedia_editor_audio_permission_refused))
        }

    override fun createMediaView(): View {
        val fields = (0 until note.initialFieldCount).mapNotNull { note.getInitialField(it)?.text }.filter(String::isNotBlank)
        return settingsComposeView(R.id.multimedia_recording_screen) {
            val recorder by recordingViewModel.recorderState.collectAsStateWithLifecycle()
            val player by recordingViewModel.playerState.collectAsStateWithLifecycle()
            val elapsed by recordingViewModel.elapsed.collectAsStateWithLifecycle()
            val position by recordingViewModel.playbackPosition.collectAsStateWithLifecycle()
            AudioRecordingScreen(
                state =
                    AudioRecordingScreenState(
                        allowed = allowedToRecord,
                        recorder = recorder,
                        player = player,
                        elapsed = elapsed,
                        position = position,
                        waveform = waveform,
                        fields = fields,
                    ),
                onRecord = ::onRecordPressed,
                onHoldRelease = ::saveRecording,
                onSave = ::saveRecording,
                onDiscard = ::discardRecording,
                onPlay = ::togglePlayback,
                onBack = recordingViewModel::skipBack,
                onForward = recordingViewModel::skipForward,
                onDone = ::attachRecording,
            )
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        allowedToRecord = Permissions.canRecordAudio(requireContext())
        if (!allowedToRecord && savedInstanceState == null) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        recordingViewModel.recorderState
            .combine(recordingViewModel.playerState) { recorder, player -> recorder to player }
            .collectInViewLifecycle { (recorder, player) ->
                if (recorder != RecorderState.Recording && recorder != RecorderState.Paused) waveform.clear()
                view.keepScreenOn = recorder == RecorderState.Recording || (player as? PlayerState.Ready)?.isPlaying == true
                viewModel.updateCurrentMultimediaPath((recorder as? RecorderState.Recorded)?.file)
            }
        recordingViewModel.amplitudes.filterNotNull().collectInViewLifecycle { amplitude ->
            if (waveform.size == 120) waveform.removeAt(0)
            waveform.add(amplitude)
        }
        recordingViewModel.failure.filterNotNull().collectInViewLifecycle { failure ->
            showFailure(failure)
            recordingViewModel.failureShown()
        }
    }

    override fun onStop() {
        super.onStop()
        view?.keepScreenOn = false
        if (!requireActivity().isChangingConfigurations) recordingViewModel.interrupt()
    }

    private fun attachRecording() {
        recordingViewModel.consumeRecording { file ->
            field.mediaFile = file
            field.hasTemporaryMedia = true
            setMultimediaResultAndFinish(MultimediaResult.Success(indexValue, field))
        }
    }

    private fun onRecordPressed() {
        when (recordingViewModel.recorderState.value) {
            RecorderState.Idle -> recordingViewModel.startRecording()
            RecorderState.Recording -> {
                recordingViewModel.pauseRecording()
                vibrate()
            }
            RecorderState.Paused -> recordingViewModel.resumeRecording()
            is RecorderState.Recorded -> {}
        }
    }

    private fun saveRecording() {
        val state = recordingViewModel.recorderState.value
        if (state != RecorderState.Recording && state != RecorderState.Paused) return
        recordingViewModel.stopRecording()
        vibrate()
        if (recordingViewModel.recorderState.value is RecorderState.Recorded) {
            showSnackbar(CommonString.audio_saved)
        }
    }

    private fun discardRecording() {
        recordingViewModel.discard()
        vibrate()
    }

    private fun togglePlayback() {
        val state = recordingViewModel.playerState.value
        if (state is PlayerState.Ready && state.isPlaying) {
            recordingViewModel.pausePlayback()
        } else {
            recordingViewModel.play()
        }
    }

    private fun showFailure(failure: Failure) {
        val message =
            when (failure) {
                Failure.RECORDING -> CommonString.multimedia_editor_audio_view_recording_failed
                Failure.PLAYBACK -> CommonString.multimedia_editor_audio_view_playing_failed
            }
        showThemedToast(requireContext(), getString(message), true)
    }

    private fun vibrate() = compat.vibrate(requireContext(), 20.milliseconds, USAGE_TOUCH)

    companion object {
        fun getIntent(
            context: Context,
            multimediaExtra: MultimediaActivityExtra,
        ): Intent =
            MultimediaActivity.getIntent(
                context,
                AudioRecordingFragment::class,
                multimediaExtra,
            )
    }
}
