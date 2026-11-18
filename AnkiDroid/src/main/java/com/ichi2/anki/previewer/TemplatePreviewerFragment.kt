// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.previewer

import android.os.Bundle
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.BundleCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.libanki.CardOrdinal
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation
import kotlinx.coroutines.launch
import timber.log.Timber

class TemplatePreviewerFragment : ComposeCardViewerFragment() {
    override val viewModel: TemplatePreviewerViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        // The OS can clear the temporary Note Type file while the process is gone.
        if (!TemplatePreviewerArguments.isUsable(requireArguments())) {
            Timber.w("Notetype file missing on previewer open; finishing")
            requireActivity().finish()
            return
        }

        super.onViewCreated(view, savedInstanceState)

        val standalone = parentFragment is TemplatePreviewerPage
        val inTemplateEditor = requireArguments().getBoolean(ARG_TEMPLATE_EDITOR)
        val arguments = checkNotNull(BundleCompat.getParcelable(requireArguments(), ARG_KEY, TemplatePreviewerArguments::class.java))
        val noteTypeName = if (arguments.fromTemplateEditor && standalone) arguments.notetype.name else null
        val noteSource =
            when {
                !arguments.fromTemplateEditor -> null
                arguments.usingFirstNote -> getString(R.string.template_previewer_first_note)
                arguments.id != 0L -> getString(R.string.template_previewer_current_note)
                else -> getString(R.string.template_previewer_sample_note)
            }
        var titles by mutableStateOf(emptyList<String>())
        var selectedTab by mutableIntStateOf(-1)
        if (!inTemplateEditor) {
            viewLifecycleOwner.lifecycleScope.launch {
                val emptyFronts = viewModel.cardsWithEmptyFronts?.await()
                titles =
                    viewModel.getTemplateNames().mapIndexed { index, name ->
                        if (emptyFronts?.get(index) == true) {
                            getString(CommonString.card_previewer_empty_front_indicator, name)
                        } else {
                            name
                        }
                    }
                viewModel.ordFlow.collect {
                    selectedTab = viewModel.getCurrentTabIndex()
                }
            }
        }

        (view as ComposeView).setContent {
            AnkiDroidTheme {
                val showingAnswer by viewModel.showingAnswer.collectAsStateWithLifecycle()
                TemplatePreviewerScreen(
                    webView = webViewLayout,
                    titles = titles,
                    selectedTab = selectedTab,
                    showingAnswer = showingAnswer,
                    standalone = standalone,
                    fullBleed = fullBleed,
                    minimumButtonHeight = MaterialTheme.foundation.spacing.primaryButton,
                    noteTypeName = noteTypeName,
                    noteSource = noteSource,
                    onSelect = viewModel::onTabSelected,
                    onToggleSide = viewModel::toggleShowAnswer,
                    onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                )
            }
        }
    }

    /** Updates a regular template in the note editor's live preview. */
    fun updateContent(
        fields: List<String>,
        tags: List<String>,
    ) {
        viewModel.updateContent(fields, tags)
    }

    /** Preserves the selected cloze when edits change the available ordinals. */
    suspend fun getSafeClozeOrd(): CardOrdinal = viewModel.getSafeClozeOrd()

    companion object {
        const val ARG_KEY = "arg_key"
        private const val ARG_TEMPLATE_EDITOR = "template_editor"

        fun newInstance(
            arguments: TemplatePreviewerArguments,
            inTemplateEditor: Boolean = false,
        ): TemplatePreviewerFragment =
            TemplatePreviewerFragment().apply {
                this.arguments =
                    Bundle().apply {
                        putParcelable(ARG_KEY, arguments)
                        putBoolean(ARG_TEMPLATE_EDITOR, inTemplateEditor)
                    }
            }
    }
}
