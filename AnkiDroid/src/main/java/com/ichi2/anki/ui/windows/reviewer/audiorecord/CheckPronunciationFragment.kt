// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer.audiorecord

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ichi2.anki.CommonString
import com.ichi2.anki.recorder.RecorderState
import com.ichi2.anki.ui.windows.reviewer.ReviewerViewModel
import com.ichi2.anki.utils.ext.collectInViewLifecycle
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.utils.Permissions

/** Records and plays the learner’s pronunciation alongside the current Card. */
class CheckPronunciationFragment : Fragment() {
    private val viewModel: CheckPronunciationViewModel by viewModels()
    private val studyScreenViewModel: ReviewerViewModel by viewModels({ requireParentFragment() })

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                createPaperDialog(requireContext()) { dialog ->
                    ErrorDialog(
                        title = getString(CommonString.permission_denied),
                        message = getString(CommonString.microphone_permission_denied_message),
                        onDismissRequest = dialog::dismiss,
                        dismissText = getString(CommonString.dialog_cancel),
                        primaryAction =
                            DialogAction(getString(CommonString.dialog_ok), {
                                dialog.dismiss()
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.fromParts("package", requireContext().packageName, null)
                                startActivity(intent)
                            }),
                    )
                }.show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AnkiDroidTheme {
                val state by viewModel.recordingState.collectAsState()
                val elapsed by viewModel.elapsed.collectAsState()
                val playback by viewModel.isPlaybackVisibleFlow.collectAsState()
                val playing by viewModel.isPlayingFlow.collectAsState(initial = false)
                val position by viewModel.playbackProgressFlow.collectAsState(initial = 0)
                val duration by viewModel.playbackProgressBarMaxFlow.collectAsState(initial = 1)
                val replayCount by produceState(0, viewModel) { viewModel.replayFlow.collect { value++ } }
                PronunciationControls(
                    recording = state == RecorderState.Recording,
                    elapsedSeconds = elapsed.inWholeSeconds,
                    playbackVisible = playback,
                    playing = playing,
                    progress = position.toFloat() / duration,
                    replayCount = replayCount,
                    onStart = {
                        if (Permissions.canRecordAudio(context)) {
                            viewModel.onRecordingStarted()
                            viewModel.recordingState.value == RecorderState.Recording
                        } else {
                            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            false
                        }
                    },
                    onFinish = viewModel::onRecordingCompleted,
                    onCancelRecording = viewModel::onRecordingCancelled,
                    onPlay = viewModel::onPlayOrReplay,
                    onClear = viewModel::onCancelPlayback,
                )
            }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        studyScreenViewModel.voiceRecorderEnabledFlow.collectInViewLifecycle { enabled ->
            if (!enabled) viewModel.resetAll()
        }
        studyScreenViewModel.replayVoiceFlow.collectInViewLifecycle { viewModel.onPlayOrReplay() }
        studyScreenViewModel.onCardUpdatedFlow.collectInViewLifecycle { viewModel.onCancelPlayback() }
    }

    override fun onPause() {
        super.onPause()
        if (requireActivity().isChangingConfigurations) return
        if (viewModel.recordingState.value == RecorderState.Recording) viewModel.onRecordingCompleted()
        viewModel.pausePlayback()
    }
}
