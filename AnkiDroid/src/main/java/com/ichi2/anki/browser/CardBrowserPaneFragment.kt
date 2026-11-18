// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import anki.collection.OpChanges
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.CommonString
import com.ichi2.anki.DispatchKeyEventListener
import com.ichi2.anki.NoteEditorFragment
import com.ichi2.anki.R
import com.ichi2.anki.android.input.ShortcutGroupProvider
import com.ichi2.anki.browser.CardBrowserViewModel.ChangeMultiSelectMode.SingleSelectCause
import com.ichi2.anki.browser.CardBrowserViewModel.NoteEditorCommand
import com.ichi2.anki.common.destinations.NoteEditorDestination
import com.ichi2.anki.common.destinations.navigate
import com.ichi2.anki.common.utils.ext.getLongExtra
import com.ichi2.anki.dialogs.DiscardChangesDialog
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.observability.ChangeManager
import com.ichi2.anki.settings.uiConfigPrefs
import com.ichi2.anki.ui.ResizablePaneLayout
import com.ichi2.anki.ui.ResizablePaneManager
import com.ichi2.anki.ui.ResizingDivider
import com.ichi2.anki.utils.ext.collectInViewLifecycle

/** A standalone Browse destination, with a live Note Editor beside it on tablets. */
class CardBrowserPaneFragment :
    Fragment(),
    ChangeManager.Subscriber,
    DispatchKeyEventListener,
    ShortcutGroupProvider {
    private val launchIntent: Intent?
        get() = BundleCompat.getParcelable(requireArguments(), ARG_LAUNCH, Intent::class.java)
    private val useEditorPane
        get() = !resources.getBoolean(R.bool.bottom_navigation_available)

    internal val browserViewModel by viewModels<CardBrowserViewModel> {
        CardBrowserViewModel.factory(
            lastDeckIdRepository = AnkiDroidApp.instance.sharedPrefsLastDeckIdRepository,
            idsFileDirectory = IdsFile.getDirectory(),
            options = launchIntent?.toCardBrowserLaunchOptions(),
            isFragmented = useEditorPane,
            initialDeckId = launchIntent?.getLongExtra(CardBrowserViewModel.EXTRA_DECK_ID),
        )
    }

    private val browser: CardBrowserFragment?
        get() = childFragmentManager.findFragmentById(R.id.card_browser_frame) as? CardBrowserFragment

    private val editor: NoteEditorFragment?
        get() = childFragmentManager.findFragmentById(R.id.note_editor_frame) as? NoteEditorFragment

    private var editorPane: FragmentContainerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChangeManager.subscribe(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = inflater.context
        val browserPane = FragmentContainerView(context).apply { id = R.id.card_browser_frame }
        if (!useEditorPane) return browserPane
        val notePane = FragmentContainerView(context).apply { id = R.id.note_editor_frame }
        val divider = ResizingDivider(context).apply { id = R.id.card_browser_resizing_divider }
        val root =
            ResizablePaneLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(browserPane, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 3f))
                addView(divider, (8 * resources.displayMetrics.density).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
                addView(notePane, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 2f))
            }

        // The split panes own the horizontal and bottom insets inside their individual screens.
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            view.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
            insets
        }
        editorPane = notePane
        ResizablePaneManager(
            parentLayout = root,
            divider = divider,
            startPane = browserPane,
            endPane = notePane,
            sharedPrefs = context.uiConfigPrefs(),
            startPaneWeightKey = "cardBrowserPaneWeight",
            endPaneWeightKey = "cardBrowserNoteEditorPaneWeight",
        )
        return root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        // Instantiate the launch-specific model before the child resolves its ViewModel store.
        browserViewModel
        if (browser == null) {
            childFragmentManager.commit {
                setReorderingAllowed(true)
                add(R.id.card_browser_frame, CardBrowserFragment())
            }
        }
        browserViewModel.flowOfNoteEditorCommand.collectInViewLifecycle(block = ::handleEditorCommand)
    }

    override fun onResume() {
        super.onResume()
        browserViewModel.refreshColumnsFromPrefs()
    }

    override fun onPause() {
        browserViewModel.removeUnsubmittedInput()
        super.onPause()
    }

    override fun onDestroyView() {
        editorPane = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        if (isRemoving) browserViewModel.deleteSavedSelectionFile()
        super.onDestroy()
    }

    internal fun navigateBack() {
        if (browserViewModel.isInMultiSelectMode) {
            browserViewModel.endMultiSelectMode(SingleSelectCause.NavigateBack)
        } else {
            finishScreen(Activity.RESULT_OK)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        event.action == KeyEvent.ACTION_UP && browser?.onKeyUp(event.keyCode, event) == true

    override val shortcuts
        get() = browser?.shortcuts

    private fun handleEditorCommand(command: NoteEditorCommand) {
        when (command) {
            is NoteEditorCommand.LoadInPane -> {
                editorPane?.isVisible = true
                val editing = editor
                launchCatchingTask {
                    when {
                        editing != null && editing.isEditingSameCards(command.destination) -> {
                            if (!editing.hasUnsavedChanges()) editing.reloadNoteFromCollection()
                        }
                        editing?.hasUnsavedChanges() == true -> confirmEditingChange(command.destination)
                        else -> replaceEditor(command.destination)
                    }
                }
            }
            is NoteEditorCommand.LaunchActivity -> navigate(command.destination)
            NoteEditorCommand.HidePane -> editorPane?.isVisible = false
        }
    }

    private fun confirmEditingChange(destination: NoteEditorDestination) {
        DiscardChangesDialog.showDialog(
            context = requireContext(),
            positiveButtonText = getString(CommonString.save),
            negativeButtonText = getString(CommonString.discard),
            neutralButtonText = getString(CommonString.dialog_cancel),
            message = getString(CommonString.save_changes_message),
            positiveMethod = {
                launchCatchingTask {
                    editor?.saveNote()
                    replaceEditor(destination)
                }
            },
            negativeMethod = { replaceEditor(destination) },
            neutralMethod = {},
        )
    }

    private fun replaceEditor(destination: NoteEditorDestination) {
        childFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.note_editor_frame, NoteEditorFragment.newInstance(destination))
        }
    }

    override fun opExecuted(
        changes: OpChanges,
        handler: Any?,
    ) {
        if (!isAdded || handler === this || handler === browserViewModel) return
        if (changes.browserSidebar || changes.browserTable || changes.noteText || changes.card) {
            val feedback = if (handler is NoteEditorFragment) BrowserSearchFeedbackMode.COUNT_ONLY else BrowserSearchFeedbackMode.NONE
            browserViewModel.launchSearchForCards(feedbackMode = feedback)
            browserViewModel.endMultiSelectMode(SingleSelectCause.Other)
        }
    }

    companion object {
        private const val ARG_LAUNCH = "browserLaunch"

        internal fun arguments(intent: Intent): Bundle = Bundle().apply { putParcelable(ARG_LAUNCH, intent) }
    }
}
