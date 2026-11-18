// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Eric Li <ericli3690@gmail.com>

package com.ichi2.anki.reviewreminders

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.ichi2.anki.common.ALL_DECKS_ID
import com.ichi2.anki.libanki.DeckId
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ReminderDraft(
    val time: ReviewReminderTime,
    val deckId: DeckId,
    val threshold: String,
    val filter: ReviewReminderThresholdFilter,
    val onlyIfNotStudied: Boolean,
) : Parcelable {
    val validThreshold get() = threshold.toIntOrNull()?.takeIf { it >= 0 }
}

class AddEditReminderDialogViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mode = requireNotNull(savedStateHandle.get<AddEditReminderDialog.DialogMode>(AddEditReminderDialog.ARG_DIALOG_MODE))
    private val existingReminder = (mode as? AddEditReminderDialog.DialogMode.Edit)?.reminderToBeEdited
    private val scope = existingReminder?.scope ?: (mode as AddEditReminderDialog.DialogMode.Add).schedulerScope

    internal val draft =
        savedStateHandle.getMutableStateFlow(
            "reminder_draft",
            ReminderDraft(
                time = existingReminder?.time ?: ReviewReminderTime.getCurrentTime(),
                deckId = (scope as? ReviewReminderScope.DeckSpecific)?.did ?: ALL_DECKS_ID,
                threshold = (existingReminder?.cardTriggerThreshold?.threshold ?: 1).toString(),
                filter = existingReminder?.thresholdFilter ?: ReviewReminderThresholdFilter(),
                onlyIfNotStudied = existingReminder?.onlyNotifyIfNoReviews ?: false,
            ),
        )

    fun outputStateAsReminder(): ReviewReminder? {
        val current = draft.value
        val threshold = current.validThreshold ?: return null
        return ReviewReminder.createReviewReminder(
            time = current.time,
            cardTriggerThreshold = ReviewReminderCardTriggerThreshold(threshold),
            scope = if (current.deckId == ALL_DECKS_ID) ReviewReminderScope.Global else ReviewReminderScope.DeckSpecific(current.deckId),
            enabled = existingReminder?.enabled ?: true,
            onlyNotifyIfNoReviews = current.onlyIfNotStudied,
            thresholdFilter = current.filter,
        )
    }
}
