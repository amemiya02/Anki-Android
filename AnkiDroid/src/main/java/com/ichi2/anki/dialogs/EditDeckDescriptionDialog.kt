// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.activity.addCallback
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.StudyOptionsFragment
import com.ichi2.anki.dialogs.EditDeckDescriptionDialogViewModel.DismissType
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.dismissAllDialogFragments
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Allows a user to edit the [deck description][com.ichi2.anki.libanki.Deck.description]
 *
 * This is visible on [StudyOptionsFragment]
 */
class EditDeckDescriptionDialog : DialogFragment() {
    private val viewModel: EditDeckDescriptionDialogViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false
        return createPaperDialog(requireContext(), onOutsideClick = { viewModel.onBackRequested() }) {
            EditDescriptionContent()
        }.apply {
            setCanceledOnTouchOutside(false)
            onBackPressedDispatcher.addCallback(this) {
                val window = requireNotNull(window)
                val decor = window.decorView
                if (ViewCompat.getRootWindowInsets(decor)?.isVisible(WindowInsetsCompat.Type.ime()) == true) {
                    WindowInsetsControllerCompat(window, decor).hide(WindowInsetsCompat.Type.ime())
                } else {
                    viewModel.onBackRequested()
                }
            }
        }
    }

    @Composable
    private fun EditDescriptionContent() {
        val description by viewModel.flowOfDescription.collectAsState()
        val formatAsMarkdown by viewModel.flowOfFormatAsMarkdown.collectAsState()
        val ready by viewModel.flowOfInitCompleted.collectAsState()
        val hasChanges by viewModel.flowOfHasChanges.collectAsState(initial = false)
        PaperDialog(
            title = if (ready) viewModel.windowTitle else getString(CommonString.edit_deck_description),
            onDismissRequest = { viewModel.onBackRequested() },
            actions =
                listOf(
                    DialogAction(getString(CommonString.close), { viewModel.onBackRequested() }),
                    DialogAction(getString(CommonString.save), { viewModel.saveAndExit() }, DialogActionStyle.Filled, enabled = hasChanges),
                ),
        ) {
            val focus = remember { FocusRequester() }
            val keyboard = LocalSoftwareKeyboardController.current
            OutlinedTextField(
                value = description,
                onValueChange = { viewModel.description = it },
                label = { Text(getString(CommonString.deck_description_field_hint)) },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                enabled = ready,
                minLines = 3,
            )
            DialogChoiceRow(
                choice = DialogChoice(getString(CommonString.format_deck_description_as_markdown), enabled = ready),
                checked = formatAsMarkdown,
                onClick = { viewModel.formatAsMarkdown = !formatAsMarkdown },
                multiple = true,
            )
            TextButton(onClick = ::showMarkdownHelp) {
                Text(getString(CommonString.help_button_content_description, getString(CommonString.format_deck_description_as_markdown)))
            }
            if (ready) {
                LaunchedEffect(focus) {
                    focus.requestFocus()
                    keyboard?.show()
                }
            }
        }
    }

    private fun showMarkdownHelp() {
        createPaperDialog(requireContext()) { dialog ->
            ErrorDialog(
                title = getString(CommonString.format_deck_description_as_markdown),
                message = TR.deckConfigDescriptionNewHandlingHint().replace("\n", " ").replace("  ", " "),
                onDismissRequest = dialog::dismiss,
            )
        }.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            viewModel.flowOfDismissDialog.filterNotNull().collect { dismissType ->
                requireActivity().dismissAllDialogFragments()
                if (dismissType == DismissType.Saved) {
                    showSnackbar(CommonString.deck_description_saved)
                    requireActivity().invalidateOptionsMenu()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.flowOfShowDiscardChanges.collect { showDiscardChangesDialog() }
        }
    }

    fun showDiscardChangesDialog() {
        createPaperDialog(requireContext()) { dialog ->
            ConfirmationDialog(
                title = TR.cardTemplatesDiscardChanges(),
                message = "",
                confirm =
                    DialogAction(getString(CommonString.discard), {
                        dialog.dismiss()
                        viewModel.closeWithoutSaving()
                    }, DialogActionStyle.Destructive),
                onDismissRequest = dialog::dismiss,
                dismissText = with(requireContext()) { TR.sentenceCase.keepEditing },
            )
        }.show()
    }

    companion object {
        fun newInstance(deckId: DeckId): EditDeckDescriptionDialog =
            EditDeckDescriptionDialog().apply {
                arguments =
                    Bundle().apply {
                        putLong(EditDeckDescriptionDialogViewModel.ARG_DECK_ID, deckId)
                    }
            }
    }
}
