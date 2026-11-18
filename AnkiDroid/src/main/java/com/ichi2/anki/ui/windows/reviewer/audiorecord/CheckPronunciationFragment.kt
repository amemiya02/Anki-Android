// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer.audiorecord

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.databinding.FragmentCheckPronunciationBinding
import com.ichi2.anki.ui.windows.reviewer.ReviewerViewModel
import com.ichi2.anki.utils.ext.collectInViewLifecycle
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import dev.androidbroadcast.vbpd.viewBinding

/**
 * Integrates [AudioRecordView] with [AudioPlayView] to play the recorded audios.
 */
class CheckPronunciationFragment : Fragment(R.layout.fragment_check_pronunciation) {
    private val viewModel: CheckPronunciationViewModel by viewModels()
    private val studyScreenViewModel: ReviewerViewModel by viewModels({ requireParentFragment() })

    private val binding by viewBinding(FragmentCheckPronunciationBinding::bind)

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

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupViewListeners()
        observeViewModel()
        observeStudyScreenViewModel()
    }

    override fun onPause() {
        super.onPause()
        if (requireActivity().isChangingConfigurations) {
            return
        }
        if (binding.recordView.isRecording) {
            binding.recordView.finishRecording()
        }
        viewModel.pausePlayback()
    }

    private fun setupViewListeners() {
        binding.playView.setButtonPressListener(
            object : AudioPlayView.ButtonPressListener {
                override fun onPlayButtonPressed() {
                    viewModel.onPlayOrReplay()
                }

                override fun onCancelButtonPressed() {
                    viewModel.onCancelPlayback()
                }
            },
        )

        binding.recordView.setRecordingListener(
            object : AudioRecordView.RecordingListener {
                override fun onRecordingPermissionRequired() {
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }

                override fun onRecordingStarted() {
                    viewModel.onRecordingStarted()
                }

                override fun onRecordingCanceled() {
                    viewModel.onRecordingCancelled()
                }

                override fun onRecordingCompleted() {
                    viewModel.onRecordingCompleted()
                }
            },
        )
    }

    private fun observeViewModel() {
        viewModel.isPlaybackVisibleFlow.collectInViewLifecycle { isVisible ->
            binding.playView.isVisible = isVisible
            binding.recordView.setRecordDisplayVisibility(!isVisible)
        }
        viewModel.playbackProgressFlow.collectInViewLifecycle { progress ->
            binding.playView.setPlaybackProgress(progress)
        }
        viewModel.playbackProgressBarMaxFlow.collectInViewLifecycle { max ->
            binding.playView.setPlaybackProgressBarMax(max)
        }
        viewModel.isPlayingFlow.collectInViewLifecycle { isPlaying ->
            val iconRes = if (isPlaying) R.drawable.symbol_replay else R.drawable.symbol_play_arrow
            binding.playView.changePlayIcon(iconRes)
        }
        viewModel.replayFlow.collectInViewLifecycle {
            binding.playView.rotateReplayIcon()
        }
    }

    private fun observeStudyScreenViewModel() {
        studyScreenViewModel.voiceRecorderEnabledFlow.collectInViewLifecycle { isEnabled ->
            if (!isEnabled) {
                viewModel.resetAll()
                binding.recordView.forceReset()
            }
        }
        studyScreenViewModel.replayVoiceFlow.collectInViewLifecycle {
            viewModel.onPlayOrReplay()
        }
        studyScreenViewModel.onCardUpdatedFlow.collectInViewLifecycle {
            binding.playView.isVisible = false
            viewModel.onCancelPlayback()
            binding.recordView.setRecordDisplayVisibility(true)
        }
    }
}
