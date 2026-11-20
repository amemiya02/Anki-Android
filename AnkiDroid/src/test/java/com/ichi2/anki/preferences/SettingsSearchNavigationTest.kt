// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.core.content.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.previewer.PreviewerAction
import com.ichi2.anki.reviewer.Binding
import com.ichi2.anki.reviewer.MappableBinding
import com.ichi2.anki.reviewer.MappableBinding.Companion.toPreferenceString
import com.ichi2.anki.servicelayer.PreferenceUpgradeService
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.settings.enums.AppTheme
import com.ichi2.anki.settings.enums.NightTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertIs
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class SettingsSearchNavigationTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun aSearchFindsTheImagePickerByItsSummary() {
        openSettings().use {
            search(targetContext.getString(R.string.settings_choose_image_summary))
            result(targetContext.getString(R.string.settings_choose_image)).assertIsDisplayed()
        }
    }

    @Test
    fun searchingForReviewRemindersOpensTheScreen() {
        openSettings().use {
            val title = targetContext.getString(CommonString.review_reminders_title)
            search(title)
            result(title).assertIsDisplayed().performClick()
            advanceRobolectricLooper()
            compose.onNodeWithContentDescription(targetContext.getString(CommonString.add_reminder)).assertIsDisplayed()
        }
    }

    @Test
    fun searchingForTimeboxOpensTheTimeboxSubpage() {
        openSettings().use {
            val title = targetContext.getString(CommonString.time_limit)
            search(title)
            result(title).assertIsDisplayed().performClick()
            advanceRobolectricLooper()
            compose.onNodeWithText(targetContext.getString(CommonString.use_timebox)).assertIsDisplayed()
        }
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp")
    fun tabletSearchKeepsTheCategorySelectedAndBackReturnsToTheCategory() {
        openSettings().use { scenario ->
            scenario.onActivity { activity ->
                val settings = activity.fragment as PreferencesFragment
                assertIs<ComposeView>(settings.requireView().findViewById<View>(R.id.settings_container))
            }
            val timeLimit = targetContext.getString(CommonString.time_limit)
            search(timeLimit)
            result(timeLimit).performClick()
            compose.onNodeWithText(targetContext.getString(CommonString.use_timebox)).assertIsDisplayed()

            val studying = result(targetContext.getString(R.string.settings_studying))
            studying.assertIsSelected()

            // Back clears a search in the left pane before changing the right pane.
            search("distinct search query")
            compose
                .onNodeWithContentDescription(targetContext.getString(R.string.settings_search))
                .assertTextContains("distinct search query")
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            studying.assertIsSelected()
            compose.onNodeWithText(targetContext.getString(CommonString.use_timebox)).assertIsDisplayed()

            scenario.onActivity { activity ->
                val settings = activity.fragment as PreferencesFragment
                assertIs<TimeboxSettingsFragment>(settings.currentPage)
                assertEquals(0, settings.childFragmentManager.backStackEntryCount)
                activity.onBackPressedDispatcher.onBackPressed()
            }

            studying.assertIsSelected()
            scenario.onActivity { activity ->
                val settings = activity.fragment as PreferencesFragment
                assertIs<StudyingSettingsFragment>(settings.currentPage)
            }
        }
    }

    @Test
    fun aSearchByBindingFindsTheActionOnThePreviewerTab() {
        val binding = MappableBinding(Binding.KeyCode(KeyEvent.KEYCODE_F11))
        Prefs.sharedPrefs.edit { putString(PreviewerAction.NEXT.preferenceKey, listOf(binding).toPreferenceString()) }
        openSettings().use {
            search(binding.toDisplayString(targetContext))
            result(targetContext.getString(CommonString.previewer_next)).assertIsDisplayed().performClick()
            advanceRobolectricLooper()
            compose.onNodeWithText(targetContext.getString(CommonString.previewer_next)).assertIsDisplayed()
        }
    }

    @Test
    fun followingTheSystemAppliesTheSelectedNightThemeImmediately() {
        RuntimeEnvironment.setQualifiers("+night")
        Prefs.appTheme = AppTheme.FOLLOW_SYSTEM
        Prefs.nightTheme = NightTheme.DARK
        val intent = PreferencesActivity.getIntent(targetContext, AppearanceSettingsFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use { scenario ->
            compose.onNodeWithText(targetContext.getString(R.string.settings_theme_system)).assertIsSelected()
            val blackChoices = compose.onAllNodes(hasText(targetContext.getString(R.string.settings_theme_black)) and hasClickAction())
            blackChoices[1].performScrollTo().performClick()
            advanceRobolectricLooper()
            blackChoices[1].assertIsSelected()
            scenario.onActivity { activity ->
                val background = TypedValue()
                activity.theme.resolveAttribute(CommonR.attr.foundationBackground, background, true)
                assertEquals(activity.getColor(CommonR.color.foundation_black_background), background.data)
            }
        }
    }

    @Test
    fun historicalAppearanceCodesAreConvertedBeforeOpeningSettings() {
        // Before preference upgrade 26, "2" meant Plain. Later it meant forced night.
        // E-ink lived in the separate dayTheme field after that conversion.
        data class Installed(
            val upgradeVersion: Int,
            val appTheme: String,
            val dayTheme: String?,
            val expectedTile: Int,
        )
        val cases =
            listOf(
                Installed(25, "2", null, R.string.settings_theme_light),
                Installed(25, "3", null, R.string.settings_theme_black),
                Installed(25, "4", null, R.string.settings_theme_dark),
                Installed(26, "1", "3", R.string.settings_theme_light),
            )
        for (installed in cases) {
            Prefs.sharedPrefs.edit {
                putInt("preferenceUpgradeVersion", installed.upgradeVersion)
                putString("appTheme", installed.appTheme)
                putString("dayTheme", installed.dayTheme)
                remove("nightTheme")
                remove("hayaki.theme")
            }
            PreferenceUpgradeService.upgradePreferences(targetContext, 0L)
            val intent = PreferencesActivity.getIntent(targetContext, AppearanceSettingsFragment::class)
            ActivityScenario.launch<PreferencesActivity>(intent).use {
                val tile = hasText(targetContext.getString(installed.expectedTile)) and isSelectable()
                compose.onAllNodes(tile)[0].assertIsSelected()
                val themeRow = hasText(targetContext.getString(CommonString.app_theme)) and hasClickAction()
                compose.onNode(themeRow).assertTextContains("Hayaki")
            }
        }
    }

    @Test
    fun anUpgradeKeepsBlackAtNightAndTheSavedPaletteWhenLightIsSelected() {
        // Values written by the previous Android release, before the brand Theme picker existed.
        Prefs.sharedPrefs.edit {
            putString("appTheme", "0")
            putString("nightTheme", "3")
            remove("hayaki.theme")
        }
        RuntimeEnvironment.setQualifiers("+night")
        val intent = PreferencesActivity.getIntent(targetContext, AppearanceSettingsFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            val themeRow = hasText(targetContext.getString(CommonString.app_theme)) and hasClickAction()
            compose.onNode(themeRow).assertTextContains("Hayaki")
            compose.onNodeWithText(targetContext.getString(R.string.settings_theme_system)).assertIsSelected()
            val light = hasText(targetContext.getString(R.string.settings_theme_light)) and isSelectable()
            compose.onNode(light).performScrollTo().performClick()
            advanceRobolectricLooper()
            assertEquals(NightTheme.BLACK, Prefs.nightTheme)
        }
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            val system = hasText(targetContext.getString(R.string.settings_theme_system)) and isSelectable()
            compose.onNode(system).performScrollTo().performClick()
            advanceRobolectricLooper()
            assertEquals(AppTheme.FOLLOW_SYSTEM, Prefs.appTheme)
            assertEquals(NightTheme.BLACK, Prefs.nightTheme)
        }
    }

    @Test
    fun choosingAThemeKeepsItAcrossAppearanceChangesAndRelaunch() {
        val intent = PreferencesActivity.getIntent(targetContext, AppearanceSettingsFragment::class)
        val themeRow = hasText(targetContext.getString(CommonString.app_theme)) and hasClickAction()
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            compose.onNode(themeRow).performClick()
            for (label in listOf("Hayaki", "Sakura", "Matcha", "Ocean", "Indigo", "Wisteria", "Amber", "Sumi", "Paper", "Midnight")) {
                compose.onNode(hasText(label) and isSelectable()).assertExists()
            }
            compose.onNode(hasText("Sakura") and isSelectable()).performScrollTo().performClick()
            advanceRobolectricLooper()

            for (appearance in listOf(
                R.string.settings_theme_light,
                R.string.settings_theme_dark,
                R.string.settings_theme_black,
                R.string.settings_theme_system,
            )) {
                compose
                    .onAllNodes(hasText(targetContext.getString(appearance)) and hasClickAction())[0]
                    .performScrollTo()
                    .performClick()
                advanceRobolectricLooper()
                compose.onNode(themeRow).assertTextContains("Sakura")
            }
        }
        ActivityScenario.launch<PreferencesActivity>(intent).use {
            compose.onNode(themeRow).assertTextContains("Sakura").performClick()
            compose.onNode(hasText("Sakura") and isSelectable()).assertIsSelected()
        }
    }

    private fun openSettings(): ActivityScenario<PreferencesActivity> =
        ActivityScenario.launch(PreferencesActivity.getIntent(targetContext))

    private fun search(query: String) {
        compose.onNodeWithContentDescription(targetContext.getString(R.string.settings_search)).performTextInput(query)
    }

    private fun result(title: String) = compose.onNode(hasText(title) and hasClickAction() and !hasSetTextAction())
}
