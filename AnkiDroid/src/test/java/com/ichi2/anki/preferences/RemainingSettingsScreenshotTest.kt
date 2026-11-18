// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
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
import com.ichi2.testutils.ext.reopenWithLanguage
import org.junit.Rule
import org.junit.Test
import kotlin.reflect.KClass

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class RemainingSettingsScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    enum class Page(
        val destination: KClass<out Fragment>,
        val firstSetting: Int,
        val lastSetting: Int,
    ) {
        GENERAL(GeneralSettingsFragment::class, CommonString.language, CommonString.card_browser_hide_media),
        ACCESSIBILITY(AccessibilitySettingsFragment::class, CommonString.card_zoom, CommonString.remove_animations),
        SYNC(SyncSettingsFragment::class, CommonString.menu_my_account, CommonString.sync_server_title),
        SYNC_SERVER(
            CustomSyncServerSettingsFragment::class,
            CommonString.custom_sync_server_base_url_title,
            CommonString.menu_my_account,
        ),
        NOTIFICATIONS(
            NotificationsSettingsFragment::class,
            CommonString.notification_pref_title,
            CommonString.notification_minimum_cards_due_blink,
        ),
        BACKUPS(
            BackupLimitsSettingsFragment::class,
            CommonString.pref__minutes_between_automatic_backups__title,
            CommonString.pref__monthly_backups_to_keep__title,
        ),
        ADVANCED(AdvancedSettingsFragment::class, CommonString.col_path, CommonString.allow_card_external_launch_title),
        DEVELOPER(DeveloperOptionsFragment::class, R.string.settings_dev_test_crash, R.string.settings_dev_profiles),
    }

    @Test
    fun showsAndScrollsSettings(
        @TestParameter page: Page,
    ) {
        setPhoneQualifiers()
        runTest { col.reopenWithLanguage(language.option) }
        val intent = PreferencesActivity.getIntent(targetContext, page.destination)
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            compose.onNodeWithText(targetContext.getString(page.firstSetting)).performScrollTo().assertIsDisplayed()
            captureScreen("${page.name.lowercase()}_start")
            compose.onNodeWithText(targetContext.getString(page.lastSetting)).performScrollTo().assertIsDisplayed()
            captureScreen("${page.name.lowercase()}_end")
        }
    }
}
