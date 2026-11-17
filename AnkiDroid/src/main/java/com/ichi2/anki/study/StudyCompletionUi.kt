// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.study

import androidx.fragment.app.FragmentActivity
import com.ichi2.anki.CommonString
import com.ichi2.anki.snackbar.showSnackbar

/** Presents the end of a review session in the activity that owns its result. */
object StudyCompletionUi {
    fun openFinishedScreen(activity: FragmentActivity) {
        activity.startActivity(FinishedFragment.getIntent(activity))
    }

    /** A study screen nobody awaits a result from (opened from a shortcut) ran out of Cards. */
    fun displayFromShortcut(activity: FragmentActivity) {
        openFinishedScreen(activity)
    }

    fun onReviewsCompleted(
        activity: FragmentActivity,
        state: DeckStudyState,
    ) {
        when (state.availability) {
            StudyAvailability.NoCardsAvailable -> openFinishedScreen(activity)
            StudyAvailability.CardsAvailable -> activity.showSnackbar(CommonString.studyoptions_no_cards_due)
        }
    }
}
