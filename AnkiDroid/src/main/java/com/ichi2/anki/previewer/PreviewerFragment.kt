// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.previewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.ichi2.anki.CommonString
import com.ichi2.anki.DispatchKeyEventListener
import com.ichi2.anki.Flag
import com.ichi2.anki.browser.IdsFile
import com.ichi2.anki.common.destinations.navigate
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.reviewer.BindingMap
import com.ichi2.anki.reviewer.BindingProcessor
import com.ichi2.anki.reviewer.MappableBinding
import com.ichi2.anki.utils.ext.collectInViewLifecycle
import com.ichi2.anki.utils.ext.getIntOrNull
import com.ichi2.anki.utils.ext.requireParcelable
import com.ichi2.anki.utils.ext.sharedPrefs
import com.ichi2.anki.utils.savedStateViewModelFactory
import com.ichi2.compose.theme.AnkiDroidTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

class PreviewerFragment :
    ComposeCardViewerFragment(),
    DispatchKeyEventListener,
    BindingProcessor<MappableBinding, PreviewerAction> {
    override val viewModel: PreviewerViewModel by viewModels {
        savedStateViewModelFactory { handle ->
            val arguments = requireArguments()
            // Read before constructing the ViewModel, which immediately loads the first card.
            val ids =
                try {
                    arguments.requireParcelable<IdsFile>(ARG_CARD_IDS_FILE).getIdsOrNull()
                } catch (e: IOException) {
                    throw UnavailableSelectionException(e)
                }
            val initialIndex = requireNotNull(arguments.getIntOrNull(ARG_CURRENT_INDEX))
            if (ids == null || initialIndex !in ids.indices) throw UnavailableSelectionException()
            PreviewerViewModel(handle, ids, initialIndex)
        }
    }

    private lateinit var bindingMap: BindingMap<MappableBinding, PreviewerAction>

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        try {
            viewModel
        } catch (e: UnavailableSelectionException) {
            Timber.w(e, "Failed to read previewer IDs")
            showThemedToast(requireContext(), CommonString.something_wrong, false)
            finishScreen()
            return
        }
        super.onViewCreated(view, savedInstanceState)

        bindingMap = BindingMap(sharedPrefs(), PreviewerAction.entries, this)
        view.isFocusableInTouchMode = true
        view.setOnGenericMotionListener { _, event -> bindingMap.onGenericMotionEvent(event) }
        viewModel.showingAnswer.collectInViewLifecycle {
            // Keep controller input on the preview frame when a new side is displayed.
            view.requestFocus()
        }

        var flagNames by mutableStateOf(emptyMap<Flag, String>())
        viewLifecycleOwner.lifecycleScope.launch { flagNames = Flag.queryDisplayNames(requireContext()) }
        (view as ComposeView).setContent {
            AnkiDroidTheme {
                PreviewerScreen(
                    viewModel = viewModel,
                    webView = webViewLayout,
                    flagNames = flagNames,
                    fullBleed = fullBleed,
                    onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                    onEdit = ::editCard,
                )
            }
        }
    }

    override fun processAction(
        action: PreviewerAction,
        binding: MappableBinding,
    ): Boolean {
        when (action) {
            PreviewerAction.MARK -> viewModel.toggleMark()
            PreviewerAction.EDIT -> editCard()
            PreviewerAction.TOGGLE_BACKSIDE_ONLY -> viewModel.toggleBackSideOnly()
            PreviewerAction.REPLAY_AUDIO -> viewModel.replayMedia()
            PreviewerAction.TOGGLE_FLAG_RED -> viewModel.toggleFlag(Flag.RED)
            PreviewerAction.TOGGLE_FLAG_ORANGE -> viewModel.toggleFlag(Flag.ORANGE)
            PreviewerAction.TOGGLE_FLAG_GREEN -> viewModel.toggleFlag(Flag.GREEN)
            PreviewerAction.TOGGLE_FLAG_BLUE -> viewModel.toggleFlag(Flag.BLUE)
            PreviewerAction.TOGGLE_FLAG_PINK -> viewModel.toggleFlag(Flag.PINK)
            PreviewerAction.TOGGLE_FLAG_TURQUOISE -> viewModel.toggleFlag(Flag.TURQUOISE)
            PreviewerAction.TOGGLE_FLAG_PURPLE -> viewModel.toggleFlag(Flag.PURPLE)
            PreviewerAction.UNSET_FLAG -> viewModel.setFlag(Flag.NONE)
            PreviewerAction.BACK ->
                viewLifecycleOwner.lifecycleScope.launch {
                    if (viewModel.isBackButtonEnabled.first()) viewModel.onPreviousButtonClick()
                }
            PreviewerAction.NEXT ->
                viewLifecycleOwner.lifecycleScope.launch {
                    if (viewModel.isNextButtonEnabled.first()) viewModel.onNextButtonClick()
                }
        }
        return true
    }

    private fun editCard() {
        viewLifecycleOwner.lifecycleScope.launch {
            navigate(viewModel.getNoteEditorDestination())
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN || !::bindingMap.isInitialized) return false
        return bindingMap.onKeyDown(event)
    }

    /** Distinguishes selection failures from I/O failures during ViewModel construction. */
    private class UnavailableSelectionException(
        cause: IOException? = null,
    ) : Exception("Unable to load preview selection", cause)

    companion object {
        /** Index of the card to be first displayed among the IDs provided by [ARG_CARD_IDS_FILE] */
        const val ARG_CURRENT_INDEX = "currentIndex"

        /** Argument key to a [IdsFile] with the IDs of the cards to be displayed */
        const val ARG_CARD_IDS_FILE = "cardIdsFile"

        fun getIntent(
            context: Context,
            idsFile: IdsFile,
            currentIndex: Int,
        ): Intent {
            val arguments =
                Bundle().apply {
                    putInt(ARG_CURRENT_INDEX, currentIndex)
                    putParcelable(ARG_CARD_IDS_FILE, idsFile)
                }
            return CardViewerActivity.getIntent(context, PreviewerFragment::class, arguments)
        }
    }
}
