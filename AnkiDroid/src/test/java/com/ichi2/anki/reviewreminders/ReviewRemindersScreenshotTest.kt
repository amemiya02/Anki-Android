// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 Eric Li <ericli3690@gmail.com>

package com.ichi2.anki.reviewreminders

import android.app.NotificationManager
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.fragment.app.commit
import androidx.test.core.app.ActivityScenario
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CommonString
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.OptionalPermissionSet
import com.ichi2.anki.R
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.StudyOptionsActivity
import com.ichi2.anki.common.destinations.StudyOptionsDestination
import com.ichi2.anki.common.destinations.launchActivity
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.anki.reviewreminders.ScheduleRemindersFragment.FragmentHost
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.ui.windows.permissions.PermissionsBottomSheet
import com.ichi2.anki.withDeckPicker
import com.ichi2.testutils.BackupManagerTestUtilities
import com.ichi2.testutils.simulateSystemBars
import com.ichi2.testutils.windowInsetsOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class ReviewRemindersScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    @After
    fun clearReminders() {
        ReviewRemindersDatabase.remindersSharedPrefs.edit { clear() }
    }

    @Test
    fun reminders_and_checks_in_settings() {
        targetContext.setTroubleshootingChecks(batteryOptimization = CheckResult.Warning)
        Settings.System.putString(targetContext.contentResolver, Settings.System.TIME_12_24, "24")
        val japanese = ReviewReminderScope.DeckSpecific(addDeck("日本語"))
        val deleted = ReviewReminderScope.DeckSpecific(addDeck("Deleted Deck"))
        insertReminder(ReviewReminderTime(8, 0), threshold = 10)
        insertReminder(ReviewReminderTime(12, 30), scope = japanese)
        insertReminder(ReviewReminderTime(19, 0), scope = deleted)
        insertReminder(ReviewReminderTime(21, 30), enabled = false, onlyIfNotStudied = true)
        col.decks.remove(listOf(deleted.did))

        setPhoneQualifiers()
        val intent = PreferencesActivity.getIntent(targetContext, ScheduleRemindersFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            compose.onNodeWithText(targetContext.getString(CommonString.review_reminders_title)).assertIsDisplayed()
            val check = targetContext.getString(R.string.review_reminders_check_action)
            captureScreen("review_reminders")
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(check))
            compose.onNodeWithText(check).performClick()
            compose.onNodeWithText(targetContext.getString(R.string.review_reminder_check_title)).assertIsDisplayed()
            captureScreen("check_reminders")
        }
    }

    @Test
    fun standalone_cutout_and_scroll() {
        RuntimeEnvironment.setQualifiers("+land")
        repeat(12) { insertReminder(ReviewReminderTime(8 + it, 0)) }
        withStandaloneScheduleReminders { activity ->
            activity.window.simulateSystemBars(
                with(activity) { windowInsetsOf(cutoutLeft = 32.dp, navBarBottom = 48.dp) },
                insetTarget = activity.findViewById(android.R.id.content),
            )
            val settings = activity.getString(CommonString.schedule_reminders_notification_settings_menu)
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(settings))
            compose.onNodeWithText(settings).assertIsDisplayed()
            captureScreen("standalone_landscape_end")
        }
    }

    @Test
    fun study_options_activity() {
        val deckId = addDeck("Test Deck")
        val scope = ReviewReminderScope.DeckSpecific(deckId)
        insertReminder(ReviewReminderTime(8, 0), scope)
        launchActivity<StudyOptionsActivity>(StudyOptionsDestination).use { scenario ->
            scenario.onActivity { activity ->
                activity.fragment!!.childFragmentManager.commit {
                    replace(R.id.studyoptions_frame, ScheduleRemindersFragment.newInstance(scope, FragmentHost.STUDY_OPTIONS_FRAME))
                }
                advanceRobolectricLooper()
                activity.simulateSystemBars()
                compose.onNodeWithText("Test Deck", useUnmergedTree = true).assertIsDisplayed()
                captureScreen("study_options_activity")
            }
        }
    }

    @Test
    fun study_options_panel() {
        setTabletQualifiers()
        val scope = ReviewReminderScope.DeckSpecific(addDeck("Test Deck"))
        insertReminder(ReviewReminderTime(8, 0), scope)
        withDeckPicker(deckCount = 1, withCards = true) { deckPicker ->
            deckPicker.supportFragmentManager.commit {
                replace(
                    R.id.studyoptions_fragment,
                    ScheduleRemindersFragment.newInstance(scope, FragmentHost.STUDY_OPTIONS_FRAGMENT),
                )
            }
            advanceRobolectricLooper()
            deckPicker.simulateSystemBars(cutoutLeft = 32.dp)
            compose.onNodeWithText(deckPicker.getString(CommonString.review_reminders_title)).assertIsDisplayed()
            captureScreen("study_options_panel")
        }
        BackupManagerTestUtilities.reset()
    }

    @Test
    fun notification_permission_after_adding() {
        shadowOf(targetContext.getSystemService<NotificationManager>()!!).setNotificationsEnabled(false)
        Prefs.reminderNotifsRequestShown = false
        Prefs.notificationsPermissionRequested = false
        withScheduleRemindersFragment { fragment ->
            compose.onNodeWithContentDescription(fragment.getString(CommonString.add_reminder)).performClick()
            advanceRobolectricLooper()
            compose.onNodeWithText(fragment.getString(CommonString.save)).performClick()
            advanceRobolectricLooperUntil {
                fragment.childFragmentManager.fragments
                    .filterIsInstance<PermissionsBottomSheet>()
                    .singleOrNull()
                    ?.dialog
                    ?.isShowing == true
            }
            captureScreen("notification_permission")
        }
    }

    @Test
    fun notification_permission_before_api_33() {
        shadowOf(targetContext.getSystemService<NotificationManager>()!!).setNotificationsEnabled(false)
        withScheduleRemindersFragment { fragment ->
            PermissionsBottomSheet.launch(fragment.childFragmentManager, OptionalPermissionSet.LEGACY_NOTIFICATIONS)
            advanceRobolectricLooper()
            captureScreen("notification_permission_legacy")
        }
    }

    private fun insertReminder(
        time: ReviewReminderTime,
        scope: ReviewReminderScope = ReviewReminderScope.Global,
        threshold: Int = 1,
        enabled: Boolean = true,
        onlyIfNotStudied: Boolean = false,
    ) = runBlocking {
        ReviewRemindersDatabase.insertReminder(
            ReviewReminder.createReviewReminder(
                time = time,
                cardTriggerThreshold = ReviewReminderCardTriggerThreshold(threshold),
                scope = scope,
                enabled = enabled,
                onlyNotifyIfNoReviews = onlyIfNotStudied,
            ),
        )
    }
}
