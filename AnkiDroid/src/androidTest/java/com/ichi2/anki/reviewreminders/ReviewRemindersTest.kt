// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.reviewreminders

import android.text.format.DateFormat
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.time.TimeManager
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.tests.InstrumentedTest
import com.ichi2.anki.testutil.GrantStoragePermission
import com.ichi2.anki.testutil.waitUntil
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ConfigAwareSingleFragmentActivity
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DateFormatSymbols

/** Exercises the reminder dialogs through the UI and checks their persisted results. */
@RunWith(AndroidJUnit4::class)
class ReviewRemindersTest : InstrumentedTest() {
    @get:Rule
    val storagePermissionRule = GrantStoragePermission.instance

    @get:Rule
    val compose = createEmptyComposeRule()

    private var originalReminderNotifsRequestShown = false

    private val allDecksName: String
        get() = with(testContext) { TR.sentenceCase.allDecks }

    @Before
    fun setUpReminders() {
        CollectionManager.setColForTests(emptyCol)
        ReviewRemindersDatabase.remindersSharedPrefs.edit { clear() }
        originalReminderNotifsRequestShown = Prefs.reminderNotifsRequestShown
        Prefs.reminderNotifsRequestShown = true
    }

    @After
    fun tearDownReminders() {
        storedReminders().forEach {
            ReviewReminderAlarmManager.unscheduleReviewReminderNotifications(testContext, it)
        }
        ReviewRemindersDatabase.remindersSharedPrefs.edit { clear() }
        Prefs.reminderNotifsRequestShown = originalReminderNotifsRequestShown
    }

    @Test
    fun createReminderWithDefaults() {
        withReminders {
            openAddDialog()
            text(CommonString.save).performClick()

            val reminder = awaitSingleReminder()
            assertThat(reminder.scope, equalTo(ReviewReminderScope.Global))
            assertThat(reminder.cardTriggerThreshold, equalTo(ReviewReminderCardTriggerThreshold(1)))
            assertThat(reminder.enabled, equalTo(true))
            assertThat(reminder.onlyNotifyIfNoReviews, equalTo(false))
            assertReminderRow(reminder, allDecksName)
        }
        withReminders(reminderCount = 1) {
            assertReminderRow(storedReminders().single(), allDecksName)
        }
    }

    @Test
    fun customizeReminderBeforeCreatingIt() {
        val deckName = "Reminder deck"
        val deckId = col.decks.id(deckName)
        val time = ReviewReminderTime(9, 15)
        withReminders {
            openAddDialog()
            selectDeck(deckName)
            selectTime(time)
            threshold().performScrollTo().performTextReplacement("5")
            unstudied().performScrollTo().performClick()
            text(CommonString.save).performClick()

            val reminder = awaitSingleReminder()
            assertThat(reminder.scope, equalTo(ReviewReminderScope.DeckSpecific(deckId)))
            assertThat(reminder.time, equalTo(time))
            assertThat(reminder.cardTriggerThreshold, equalTo(ReviewReminderCardTriggerThreshold(5)))
            assertThat(reminder.onlyNotifyIfNoReviews, equalTo(true))
            assertThat(reminder.enabled, equalTo(true))
            assertReminderRow(reminder, deckName)
        }
        withReminders(scope = ReviewReminderScope.DeckSpecific(deckId), reminderCount = 1) {
            assertReminderRow(storedReminders().single(), deckName)
            openOnlyReminder()
            threshold().performScrollTo().assertTextContains("5")
            unstudied().performScrollTo().assertIsOn()
        }
    }

    @Test
    fun cancelCreatingReminder() {
        withReminders {
            openAddDialog()
            text(CommonString.dialog_cancel).performClick()

            text(CommonString.save).assertDoesNotExist()
            text(CommonString.you_have_no_review_reminders).assertIsDisplayed()
            assertThat(storedReminders(), empty())
        }
    }

    @Test
    fun editReminderChangingDeckAndSettings() {
        val originalScope = ReviewReminderScope.DeckSpecific(col.decks.id("Original deck"))
        val destinationDeckName = "Destination deck"
        val destinationScope = ReviewReminderScope.DeckSpecific(col.decks.id(destinationDeckName))
        val original =
            ReviewReminder
                .createReviewReminder(
                    time = ReviewReminderTime(7, 30),
                    cardTriggerThreshold = ReviewReminderCardTriggerThreshold(2),
                    scope = originalScope,
                    enabled = false,
                ).apply { latestNotifTime = 1L }
        insertReminder(original)

        val updatedTime = ReviewReminderTime(9, 45)
        withReminders(reminderCount = 1) {
            assertReminderRow(original, "Original deck")
            openOnlyReminder()
            editorLabel("Original deck").assertExists()
            editorLabel(original.time.toFormattedString(testContext)).assertExists()
            threshold().performScrollTo().assertTextContains("2")
            unstudied().performScrollTo().assertIsOff()

            selectDeck(destinationDeckName)
            selectTime(updatedTime)
            threshold().performScrollTo().performTextReplacement("7")
            unstudied().performScrollTo().performClick()
            val beforeSave = TimeManager.time.intTimeMS()
            text(CommonString.save).performClick()

            val updated = awaitSingleReminder { it.scope == destinationScope }
            val afterSave = TimeManager.time.intTimeMS()
            assertThat(updated.time, equalTo(updatedTime))
            assertThat(updated.cardTriggerThreshold, equalTo(ReviewReminderCardTriggerThreshold(7)))
            assertThat(updated.onlyNotifyIfNoReviews, equalTo(true))
            assertThat(updated.enabled, equalTo(false))
            assertThat(
                "Editing must reset the last notification time to now",
                updated.latestNotifTime,
                allOf(greaterThanOrEqualTo(beforeSave), lessThanOrEqualTo(afterSave)),
            )
            assertThat(runBlocking { ReviewRemindersDatabase.getRemindersForScope(originalScope).isEmpty() }, equalTo(true))
            assertReminderRow(updated, destinationDeckName)
        }
        withReminders(scope = destinationScope, reminderCount = 1) {
            assertReminderRow(storedReminders().single(), destinationDeckName)
            openOnlyReminder()
            threshold().performScrollTo().assertTextContains("7")
            unstudied().performScrollTo().assertIsOn()
        }
        withReminders(scope = originalScope) {
            text(CommonString.you_have_no_review_reminders).assertIsDisplayed()
        }
    }

    @Test
    fun cancelEditingReminder() {
        val original = ReviewReminder.createReviewReminder(time = ReviewReminderTime(7, 30))
        insertReminder(original)
        withReminders(reminderCount = 1) {
            openOnlyReminder()
            selectTime(ReviewReminderTime(9, 45))
            text(CommonString.dialog_cancel).performClick()

            text(CommonString.save).assertDoesNotExist()
            assertThat(storedReminders(), equalTo(listOf(original)))
            assertReminderRow(original, allDecksName)
        }
        withReminders(reminderCount = 1) {
            assertReminderRow(original, allDecksName)
        }
    }

    @Test
    fun deleteLastReminder() {
        val reminder = ReviewReminder.createReviewReminder(time = ReviewReminderTime(9, 15))
        insertReminder(reminder)
        withReminders(reminderCount = 1) {
            openOnlyReminder()
            openDeleteConfirmation()
            assertThat("Deletion must wait for confirmation", storedReminders(), equalTo(listOf(reminder)))
            confirmationAction(CommonString.dialog_positive_delete).performClick()

            waitUntil(message = { "The deleted reminder is still stored: ${storedReminders()}" }) { storedReminders().isEmpty() }
            assertReminderCount(0)
        }
        withReminders {
            text(CommonString.you_have_no_review_reminders).assertIsDisplayed()
        }
    }

    @Test
    fun cancelDeletingReminder() {
        val reminder = ReviewReminder.createReviewReminder(time = ReviewReminderTime(9, 15))
        insertReminder(reminder)
        withReminders(reminderCount = 1) {
            openOnlyReminder()
            openDeleteConfirmation()
            confirmationAction(CommonString.dialog_cancel).performClick()

            text(CommonString.add_edit_reminder_delete_confirmation_title).assertDoesNotExist()
            assertThat(storedReminders(), equalTo(listOf(reminder)))
            text(CommonString.edit_review_reminder).assertIsDisplayed()
            text(CommonString.dialog_cancel).performClick()
            assertReminderRow(reminder, allDecksName)
        }
        withReminders(reminderCount = 1) {
            assertReminderRow(reminder, allDecksName)
        }
    }

    @Test
    fun deleteReminderForDeletedDeck() {
        val deckId = col.decks.id("Deck to delete")
        val orphan =
            ReviewReminder.createReviewReminder(
                time = ReviewReminderTime(9, 15),
                scope = ReviewReminderScope.DeckSpecific(deckId),
            )
        val remaining = ReviewReminder.createReviewReminder(time = ReviewReminderTime(10, 30))
        insertReminder(orphan)
        insertReminder(remaining)
        col.decks.remove(listOf(deckId))

        withReminders(reminderCount = 2) {
            text(R.string.review_reminders_deleted_deck).assertIsDisplayed()
            text(R.string.review_reminders_deleted_deck).performClick()
            openDeleteConfirmation()
            confirmationAction(CommonString.dialog_positive_delete).performClick()

            assertThat(awaitSingleReminder { it.id == remaining.id }, equalTo(remaining))
            assertThat(runBlocking { ReviewRemindersDatabase.getRemindersForScope(orphan.scope).isEmpty() }, equalTo(true))
            assertReminderRow(remaining, allDecksName)
        }
        withReminders(reminderCount = 1) {
            assertReminderRow(remaining, allDecksName)
        }
    }

    @Test
    fun disableAndEnableReminder() {
        val deckName = "Reminder deck"
        val reminder =
            ReviewReminder.createReviewReminder(
                time = ReviewReminderTime(9, 15),
                scope = ReviewReminderScope.DeckSpecific(col.decks.id(deckName)),
                cardTriggerThreshold = ReviewReminderCardTriggerThreshold(5),
                onlyNotifyIfNoReviews = true,
            )
        insertReminder(reminder)

        withReminders(reminderCount = 1) {
            assertReminderRow(reminder, deckName)
            switches()[0].performClick()

            val disabled = awaitSingleReminder { !it.enabled }
            reminder.enabled = false
            assertThat("Disabling must preserve the reminder's other settings", disabled, equalTo(reminder))
            assertReminderRow(disabled, deckName)
            text(CommonString.save).assertDoesNotExist()
        }
        withReminders(reminderCount = 1) {
            assertReminderRow(reminder, deckName)
            switches()[0].performClick()

            val enabled = awaitSingleReminder { it.enabled }
            reminder.enabled = true
            assertThat("Enabling must preserve the reminder's other settings", enabled, equalTo(reminder))
            assertReminderRow(enabled, deckName)
            text(CommonString.save).assertDoesNotExist()
        }
        withReminders(reminderCount = 1) {
            assertReminderRow(reminder, deckName)
        }
    }

    private fun withReminders(
        scope: ReviewReminderScope = ReviewReminderScope.Global,
        reminderCount: Int = 0,
        action: () -> Unit,
    ) {
        ActivityScenario
            .launch<ConfigAwareSingleFragmentActivity>(ScheduleRemindersFragment.getIntent(testContext, scope))
            .use {
                assertReminderCount(reminderCount)
                action()
            }
    }

    private fun text(resource: Int) = compose.onNodeWithText(testContext.getString(resource))

    private fun switches() = compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))

    private fun threshold() = compose.onNodeWithContentDescription(testContext.getString(R.string.review_reminder_card_count))

    private fun unstudied() = text(R.string.review_reminder_only_if_unstudied)

    private fun editorLabel(value: String) =
        compose.onNode(
            hasText(value, substring = true) and hasAnyAncestor(hasAnyDescendant(hasText(testContext.getString(CommonString.save)))),
        )

    private fun openOnlyReminder() {
        val time = storedReminders().single().time.toFormattedString(testContext)
        compose.onNodeWithText(time).performClick()
        text(CommonString.edit_review_reminder).assertExists()
    }

    private fun openAddDialog() {
        compose.onNodeWithContentDescription(testContext.getString(CommonString.add_reminder)).performClick()
        editorLabel(allDecksName).assertExists()
    }

    private fun openDeleteConfirmation() {
        text(CommonString.dialog_positive_delete).performScrollTo().performClick()
        text(CommonString.add_edit_reminder_delete_confirmation_title).assertIsDisplayed()
    }

    private fun confirmationAction(resource: Int) =
        compose.onNode(
            hasText(testContext.getString(resource)) and
                hasAnyAncestor(hasAnyDescendant(hasText(testContext.getString(CommonString.add_edit_reminder_delete_confirmation_title)))),
        )

    private fun selectDeck(deckName: String) {
        text(R.string.review_reminder_deck).performScrollTo().performClick()
        compose.onNodeWithText(deckName).performClick()
        editorLabel(deckName).assertExists()
    }

    private fun selectTime(time: ReviewReminderTime) {
        text(R.string.review_reminder_time).performScrollTo().performClick()
        val clockTitle = testContext.getString(R.string.review_reminder_select_time)
        val clock = hasAnyAncestor(hasAnyDescendant(hasText(clockTitle)))
        val keyboard = testContext.getString(R.string.review_reminder_keyboard_input)
        if (compose.onAllNodes(hasText(keyboard) and clock).fetchSemanticsNodes().isNotEmpty()) {
            text(R.string.review_reminder_keyboard_input).performScrollTo().performClick()
        }
        val inputs = compose.onAllNodes(hasSetTextAction() and clock)
        val is24 = DateFormat.is24HourFormat(testContext)
        val hour = if (is24) time.hour else (time.hour + 11) % 12 + 1
        inputs[0].performClick().performTextReplacement(hour.toString())
        inputs[1].performClick().performTextReplacement(time.minute.toString().padStart(2, '0'))
        if (!is24) {
            val period = DateFormatSymbols.getInstance(testContext.resources.configuration.locales[0]).amPmStrings[time.hour / 12]
            compose.onNode(hasText(period, ignoreCase = true) and clock).performClick()
        }
        text(CommonString.dialog_ok).performClick()
        editorLabel(time.toFormattedString(testContext)).assertExists()
    }

    private fun storedReminders(): List<ReviewReminder> = runBlocking { ReviewRemindersDatabase.getAllReminders().getRemindersList() }

    private fun insertReminder(reminder: ReviewReminder) = runBlocking { ReviewRemindersDatabase.insertReminder(reminder) }

    private fun awaitSingleReminder(predicate: (ReviewReminder) -> Boolean = { true }): ReviewReminder {
        waitUntil(message = { "Expected one saved reminder matching the changes, found ${storedReminders()}" }) {
            storedReminders().singleOrNull()?.let(predicate) == true
        }
        return storedReminders().single()
    }

    private fun assertReminderCount(count: Int) {
        compose.waitUntil(10_000) { switches().fetchSemanticsNodes().size == count }
        val placeholder = text(CommonString.you_have_no_review_reminders)
        if (count == 0) placeholder.assertIsDisplayed() else placeholder.assertDoesNotExist()
    }

    private fun assertReminderRow(
        reminder: ReviewReminder,
        deckName: String,
    ) {
        assertReminderCount(1)
        val time = reminder.time.toFormattedString(testContext)
        compose.onNode(hasText(deckName, substring = true) and hasText(time)).assertIsDisplayed()
        if (reminder.enabled) switches()[0].assertIsOn() else switches()[0].assertIsOff()
    }
}
