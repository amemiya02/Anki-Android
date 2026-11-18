// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 Eric Li <ericli3690@gmail.com>

package com.ichi2.anki.reviewreminders

import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.google.testing.junit.testparameterinjector.TestParameter
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CommonString
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.R
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.reviewreminders.AddEditReminderDialog.DialogMode
import org.junit.Rule
import org.junit.Test

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class AddEditReminderDialogScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun add_reminder() =
        withScheduleRemindersFragment { fragment ->
            compose.onNodeWithContentDescription(fragment.getString(CommonString.add_reminder)).performClick()
            advanceRobolectricLooper()
            compose.onNodeWithText(fragment.getString(CommonString.add_review_reminder)).assertIsDisplayed()
            captureScreen("add_reminder")

            compose.onNodeWithContentDescription(fragment.getString(R.string.review_reminder_increase)).performScrollTo().performClick()
            compose.onNodeWithText(fragment.getString(R.string.review_reminder_only_if_unstudied)).performScrollTo()
            captureScreen("add_reminder_conditions")
        }

    @Test
    fun edit_reminder_and_delete_confirmation() {
        val reminder = ReviewReminder.createReviewReminder(time = ReviewReminderTime(9, 0), onlyNotifyIfNoReviews = true)
        withEditor(reminder) {
            captureScreen("edit_reminder")
            compose.onNodeWithText(getString(CommonString.dialog_positive_delete)).performScrollTo().performClick()
            advanceRobolectricLooper()
            compose.onNodeWithText(getString(CommonString.add_edit_reminder_delete_confirmation_title)).assertIsDisplayed()
            captureScreen("delete_reminder_confirmation")
        }
    }

    @Test
    fun time_picker(
        @TestParameter use24HourClock: Boolean,
    ) {
        val clock = if (use24HourClock) "24" else "12"
        Settings.System.putString(targetContext.contentResolver, Settings.System.TIME_12_24, clock)
        val reminder = ReviewReminder.createReviewReminder(time = ReviewReminderTime(17, 45))
        withEditor(reminder) {
            compose.onNodeWithText(getString(R.string.review_reminder_time)).performScrollTo().performClick()
            advanceRobolectricLooper()
            val dial = getString(R.string.review_reminder_clock_input)
            val keyboard = getString(R.string.review_reminder_keyboard_input)
            if (compose
                    .onAllNodes(
                        androidx.compose.ui.test
                            .hasText(dial),
                    ).fetchSemanticsNodes()
                    .isNotEmpty()
            ) {
                compose.onNodeWithText(dial).performScrollTo().performClick()
                compose.onNodeWithText(getString(R.string.review_reminder_select_time)).performScrollTo()
            }
            captureScreen("time_picker_${clock}h_dial")
            compose.onNodeWithText(keyboard).performScrollTo().performClick()
            captureScreen("time_picker_${clock}h_keyboard")
        }
    }

    private fun withEditor(
        reminder: ReviewReminder,
        block: AddEditReminderDialog.() -> Unit,
    ) = withScheduleRemindersFragment { fragment ->
        val dialog = AddEditReminderDialog.getInstance(DialogMode.Edit(reminder))
        dialog.show(fragment.childFragmentManager, "editor")
        advanceRobolectricLooper()
        compose.onNodeWithText(fragment.getString(CommonString.edit_review_reminder)).assertIsDisplayed()
        dialog.block()
    }
}
