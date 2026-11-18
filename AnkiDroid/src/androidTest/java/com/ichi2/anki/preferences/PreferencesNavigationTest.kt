// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Amit Bisht <iamitsbisht07@gmail.com>

package com.ichi2.anki.preferences

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.IntentHandler
import com.ichi2.anki.R
import com.ichi2.anki.testutil.GrantStoragePermission
import com.ichi2.anki.testutil.awaitResumedActivity
import com.ichi2.anki.testutil.closeGetStartedScreenIfExists
import com.ichi2.anki.testutil.grantPermissions
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.isWindowCompact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesNavigationTest {
    @get:Rule
    val runtimePermissionRule = grantPermissions(GrantStoragePermission.storagePermission)

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun compactSettingsOpensFromMoreAndReturnsFromSearch() {
        assumeTrue(context.resources.isWindowCompact())
        ActivityScenario.launch(IntentHandler::class.java).use {
            closeGetStartedScreenIfExists()
            composeRule.onNodeWithTag("tab-more").performClick()
            composeRule.onNodeWithText(context.getString(CommonString.settings)).performClick()

            val settings = awaitSettingsHost()
            val appearance = context.getString(R.string.settings_appearance)
            val search = composeRule.onNodeWithContentDescription(context.getString(R.string.settings_search))
            search.performTextInput(appearance)
            composeRule.onNode(hasText(appearance) and hasClickAction() and !hasSetTextAction()).assertIsDisplayed().performClick()
            composeRule.onNodeWithText(context.getString(R.string.settings_theme_light)).assertIsDisplayed()
            composeRule.runOnIdle { settings.onBackPressedDispatcher.onBackPressed() }
            search.assertIsDisplayed()
        }
    }

    @Test
    fun addButtonClearsTheBottomBarAfterChangingThemeFromMore() {
        assumeTrue(context.resources.isWindowCompact())
        ActivityScenario.launch(IntentHandler::class.java).use {
            closeGetStartedScreenIfExists()
            composeRule.onNodeWithTag("tab-home").performClick()
            val originalClearance = addButtonClearance()
            assertTrue("The Add button clears the bottom bar on opening Decks", originalClearance > 0)
            composeRule.onNodeWithTag("tab-more").performClick()
            composeRule.onNodeWithText(context.getString(CommonString.settings)).performClick()
            composeRule.onNodeWithText(context.getString(R.string.settings_appearance)).performClick()

            for (theme in listOf(R.string.settings_theme_light, R.string.settings_theme_dark)) {
                val tile = composeRule.onAllNodes(hasText(context.getString(theme)) and hasClickAction())[0]
                tile.performClick().assertIsSelected()
            }

            repeat(2) {
                val settings = awaitSettingsHost()
                composeRule.runOnIdle { settings.onBackPressedDispatcher.onBackPressed() }
            }
            awaitResumedActivity<DeckPicker>()
            composeRule.onNodeWithTag("tab-home").performClick()
            assertEquals("Changing themes preserves the Add button clearance", originalClearance, addButtonClearance())
            composeRule.onNodeWithTag("deck-add").performClick()
            composeRule.onNodeWithText(with(context) { TR.sentenceCase.createDeck }).performClick()
            val label = context.getString(CommonString.paper_dialog_deck_name)
            composeRule.onNodeWithTag("text_input_label").assertTextEquals(label).assertIsDisplayed()
            composeRule.onNode(hasText(label) and hasSetTextAction()).assertIsDisplayed()
        }
    }

    private fun addButtonClearance(): Int {
        val add = composeRule.onNodeWithTag("deck-add").assertIsDisplayed()
        val tab = composeRule.onNodeWithTag("tab-home").assertIsDisplayed()
        return (tab.fetchSemanticsNode().boundsInWindow.top - add.fetchSemanticsNode().boundsInWindow.bottom).toInt()
    }

    @Test
    fun tabletSettingsOpensFromTheDrawerAndKeepsBothPanes() {
        assumeTrue(!context.resources.isWindowCompact())
        ActivityScenario.launch(IntentHandler::class.java).use {
            closeGetStartedScreenIfExists()
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            composeRule.onNodeWithText(context.getString(CommonString.settings)).performClick()

            val host = awaitSettingsHost()
            val search = composeRule.onNodeWithContentDescription(context.getString(R.string.settings_search))
            composeRule.onNodeWithText(context.getString(R.string.settings_appearance)).performClick()
            composeRule.onNodeWithText(context.getString(R.string.settings_theme_light)).assertIsDisplayed()

            val timeLimit = context.getString(CommonString.time_limit)
            search.performTextInput(timeLimit)
            composeRule.onNode(hasText(timeLimit) and hasClickAction() and !hasSetTextAction()).performClick()
            val timebox = composeRule.onNodeWithText(context.getString(CommonString.use_timebox))
            timebox.assertIsDisplayed()
            search.assertIsDisplayed()
            val studying = composeRule.onNode(hasText(context.getString(R.string.settings_studying)) and hasClickAction())
            studying.assertIsSelected()

            search.performTextInput("distinct search query")
            search.assertTextContains("distinct search query")
            composeRule.runOnIdle { host.onBackPressedDispatcher.onBackPressed() }
            timebox.assertIsDisplayed()
            studying.assertIsSelected()

            composeRule.runOnIdle { host.onBackPressedDispatcher.onBackPressed() }
            composeRule.waitUntil(10_000) {
                (host.currentScreenFragment as? PreferencesFragment)?.currentPage is StudyingSettingsFragment
            }
            studying.assertIsSelected()
            search.assertIsDisplayed()
        }
    }

    private fun awaitSettingsHost(): DeckPicker {
        val host = awaitResumedActivity<DeckPicker>()
        composeRule.waitUntil(10_000) { host.currentScreenFragment is PreferencesFragment }
        return host
    }
}
