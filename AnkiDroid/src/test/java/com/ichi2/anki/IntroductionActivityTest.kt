// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2026 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Application
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.ui.windows.permissions.PermissionsActivity
import com.ichi2.testutils.withDeniedPermissions
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class IntroductionActivityTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `raw first launch opens the welcome and a returning owner opens Decks`() {
        getPreferences().edit { remove("lastVersion") }
        val first = startRegularActivity<DeckPicker>()
        assertNull(shadowOf(first).nextStartedActivity)
        choice(CommonString.paper_welcome_start_empty).assertIsDisplayed()
        first.finish()

        setIntroductionSlidesShown(true)
        val returning = startRegularActivity<DeckPicker>()
        assertFalse(returning.isFinishing)
        assertNull(shadowOf(returning).nextStartedActivity)
    }

    @Test
    fun `an owner upgrading from an older install goes straight to Decks`() {
        getPreferences().edit { remove(IntroductionActivity.PREF_INTRODUCTION_SLIDES_SHOWN) }
        InitialActivity.setUpgradedToLatestVersion(getPreferences())

        val returning = startRegularActivity<DeckPicker>()
        assertFalse(returning.isFinishing)
        assertNull(shadowOf(returning).nextStartedActivity)
    }

    @Test
    fun `a newcomer can choose to set up a self-hosted server`() =
        withWelcome { scenario ->
            choice(CommonString.paper_welcome_sync).performScrollTo().assertIsDisplayed().performClick()

            scenario.assertDeckPickerIntent(DeckPicker.EXTRA_SYNC_SETUP)
        }

    // Android Q uses legacy storage permissions, which may have been denied.
    @Config(sdk = [Build.VERSION_CODES.Q])
    @Test
    fun `Import without storage permission opens PermissionsActivity`() =
        runTest {
            withDeniedPermissions(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE) {
                withWelcome { scenario ->
                    choice(CommonString.paper_welcome_import).performScrollTo().performClick()

                    scenario.onActivity { activity ->
                        val intent = assertNotNull(shadowOf(activity).nextStartedActivityForResult?.intent)
                        assertEquals(PermissionsActivity::class.java.name, intent.component?.className)
                        assertFalse(activity.hasShownAppIntro())
                    }
                }
            }
        }

    @Test
    fun `Import opens the deck list ready to pick a package`() =
        withWelcome { scenario ->
            choice(CommonString.paper_welcome_import).performScrollTo().performClick()

            scenario.assertDeckPickerIntent(DeckPicker.EXTRA_IMPORT)
        }

    @Test
    fun `Start with no Decks opens the deck list and completes first run`() =
        withWelcome { scenario ->
            choice(CommonString.paper_welcome_start_empty).performClick()

            scenario.assertDeckPickerIntent()
        }

    @Test
    fun `recreation preserves the AnkiWeb steps and Choose the file opens the importer`() =
        withWelcome { scenario ->
            choice(CommonString.paper_sync_from_ankiweb).performScrollTo().performClick()
            scenario.recreate()

            text(CommonString.paper_welcome_migration_sync).performScrollTo().assertIsDisplayed()
            text(CommonString.paper_welcome_migration_export).performScrollTo().assertIsDisplayed()
            text(CommonString.paper_welcome_migration_import).performScrollTo().assertIsDisplayed()
            assertFalse(targetContext.hasShownAppIntro())
            choice(CommonString.paper_sync_choose_file).performClick()

            scenario.assertDeckPickerIntent(DeckPicker.EXTRA_IMPORT)
        }

    private fun ActivityScenario<IntroductionActivity>.assertDeckPickerIntent(extra: String? = null) {
        assertTrue(targetContext.hasShownAppIntro())
        // IntroductionActivity finishes after starting DeckPicker. Use the shared app shadow to read its launch.
        val intent = assertNotNull(shadowOf(targetContext.applicationContext as Application).nextStartedActivity)
        assertEquals(DeckPicker::class.java.name, intent.component?.className)
        assertEquals(extra == DeckPicker.EXTRA_IMPORT, intent.getBooleanExtra(DeckPicker.EXTRA_IMPORT, false))
        assertEquals(extra == DeckPicker.EXTRA_SYNC_SETUP, intent.getBooleanExtra(DeckPicker.EXTRA_SYNC_SETUP, false))
        assertFalse(intent.hasExtra(DeckPicker.EXTRA_SYNC_FROM_LOGIN))
    }

    private fun withWelcome(block: (ActivityScenario<IntroductionActivity>) -> Unit) {
        getPreferences().edit {
            remove("lastVersion")
            remove(IntroductionActivity.PREF_INTRODUCTION_SLIDES_SHOWN)
        }
        ActivityScenario.launch(IntroductionActivity::class.java).use(block)
    }

    private fun choice(
        @StringRes label: Int,
    ) = compose.onNode(hasText(targetContext.getString(label)) and hasClickAction())

    private fun text(
        @StringRes label: Int,
    ) = compose.onNodeWithText(targetContext.getString(label))
}
