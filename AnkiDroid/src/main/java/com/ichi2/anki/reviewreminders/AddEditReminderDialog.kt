// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Eric Li <ericli3690@gmail.com>

package com.ichi2.anki.reviewreminders

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.view.Gravity
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.common.ALL_DECKS_ID
import com.ichi2.anki.dialogs.ConfirmationDialog
import com.ichi2.anki.dialogs.registerDeckSelectedHandler
import com.ichi2.anki.dialogs.startDeckSelection
import com.ichi2.anki.isDefaultDeckEmpty
import com.ichi2.anki.libanki.Consts
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.getParcelableCompat
import com.ichi2.anki.utils.ext.requireParcelable
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.utils.Permissions
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize

class AddEditReminderDialog : DialogFragment() {
    @Parcelize
    sealed class DialogMode : Parcelable {
        data class Add(
            val schedulerScope: ReviewReminderScope,
        ) : DialogMode()

        data class Edit(
            val reminderToBeEdited: ReviewReminder,
        ) : DialogMode()
    }

    private val viewModel: AddEditReminderDialogViewModel by viewModels()
    private val mode by lazy { requireArguments().requireParcelable<DialogMode>(ARG_DIALOG_MODE) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        registerDeckSelectedHandler { deck ->
            val id =
                when (deck) {
                    is SelectableDeck.Deck -> deck.deckId
                    SelectableDeck.AllDecks -> ALL_DECKS_ID
                    null -> return@registerDeckSelectedHandler
                }
            viewModel.draft.update { it.copy(deckId = id) }
        }
        childFragmentManager.setFragmentResultListener(ReviewReminderTimeDialog.REQUEST_TIME, this) { _, result ->
            val time = result.requireParcelable<ReviewReminderTime>(ReviewReminderTimeDialog.RESULT_TIME)
            viewModel.draft.update { it.copy(time = time) }
        }

        childFragmentManager.setFragmentResultListener(REQUEST_DELETE_REMINDER, this) { _, _ -> finish(null) }

        return createPaperDialog(requireContext(), onOutsideClick = ::dismiss) {
            val draft by viewModel.draft.collectAsStateWithLifecycle()
            var deckName by remember(draft.deckId) { mutableStateOf<String?>(null) }
            LaunchedEffect(draft.deckId) {
                val (id, name) = resolveDeck(draft.deckId)
                if (id == draft.deckId) {
                    deckName = name
                } else {
                    viewModel.draft.update { it.copy(deckId = id) }
                }
            }
            ReminderEditor(
                draft = draft,
                deckName = deckName,
                editing = mode is DialogMode.Edit,
                onDraftChange = { viewModel.draft.value = it },
                onTime = { ReviewReminderTimeDialog.newInstance(draft.time).show(childFragmentManager, "reminder_time") },
                onDeck = { startDeckSelection(allowAll = true, allowFiltered = true, skipEmptyDefault = true) },
                onDismiss = ::dismiss,
                onSave = ::saveReminder,
                onDelete = ::confirmDelete,
            )
        }.apply {
            window?.setGravity(Gravity.BOTTOM)
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private suspend fun resolveDeck(id: DeckId): Pair<DeckId, String> {
        if (id == ALL_DECKS_ID) return id to TR.sentenceCase.allDecks
        val selected =
            if (id == Consts.DEFAULT_DECK_ID && isDefaultDeckEmpty()) {
                null
            } else {
                withCol { decks.nameIfExists(id) }
            }
        if (selected != null) return id to selected
        return if (isDefaultDeckEmpty()) {
            ALL_DECKS_ID to TR.sentenceCase.allDecks
        } else {
            Consts.DEFAULT_DECK_ID to withCol { decks.name(Consts.DEFAULT_DECK_ID) }
        }
    }

    private fun saveReminder() {
        val reminder = viewModel.outputStateAsReminder() ?: return
        finish(reminder)
        if (!Prefs.reminderNotifsRequestShown) {
            Permissions.showNotificationsPermissionBottomSheetIfNeeded(requireActivity(), parentFragmentManager) {
                Prefs.reminderNotifsRequestShown = true
            }
        }
    }

    private fun confirmDelete() {
        ConfirmationDialog
            .newInstance(
                requestKey = REQUEST_DELETE_REMINDER,
                title = getString(CommonString.add_edit_reminder_delete_confirmation_title),
                message = getString(CommonString.add_edit_reminder_delete_confirmation_message),
                positiveButtonText = getString(CommonString.dialog_positive_delete),
                destructive = true,
            ).show(childFragmentManager, "delete_reminder")
    }

    private fun finish(reminder: ReviewReminder?) {
        setFragmentResult(
            REQUEST_ADD_EDIT_REMINDER,
            Bundle().apply {
                putParcelable(RESULT_MODE, mode)
                putParcelable(RESULT_REMINDER, reminder)
            },
        )
        dismiss()
    }

    companion object {
        const val ARG_DIALOG_MODE = "arg_dialog_mode"
        private const val REQUEST_DELETE_REMINDER = "request_delete_reminder"
        private const val REQUEST_ADD_EDIT_REMINDER = "request_add_edit"
        private const val RESULT_MODE = "result_mode"
        private const val RESULT_REMINDER = "result_reminder"

        fun Fragment.registerAddEditReminderHandler(
            action: (newOrModifiedReminder: ReviewReminder?, modeOfFinishedDialog: DialogMode) -> Unit,
        ) {
            childFragmentManager.setFragmentResultListener(REQUEST_ADD_EDIT_REMINDER, viewLifecycleOwner) { _, bundle ->
                val mode = bundle.getParcelableCompat<DialogMode>(RESULT_MODE) ?: return@setFragmentResultListener
                action(bundle.getParcelableCompat<ReviewReminder>(RESULT_REMINDER), mode)
            }
        }

        fun getInstance(dialogMode: DialogMode): AddEditReminderDialog =
            AddEditReminderDialog().apply {
                arguments = Bundle().apply { putParcelable(ARG_DIALOG_MODE, dialogMode) }
            }
    }
}
