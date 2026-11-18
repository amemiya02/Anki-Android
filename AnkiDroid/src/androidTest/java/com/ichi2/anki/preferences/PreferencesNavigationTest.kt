// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.IntentHandler
import com.ichi2.anki.R
import com.ichi2.anki.testutil.GrantStoragePermission
import com.ichi2.anki.testutil.awaitResumedActivity
import com.ichi2.anki.testutil.closeGetStartedScreenIfExists
import com.ichi2.anki.testutil.grantPermissions
import com.ichi2.anki.utils.isWindowCompact
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
            onView(withId(R.id.nav_more)).perform(click())
            composeRule.onNodeWithText(context.getString(CommonString.settings)).performClick()

            val settings = awaitResumedActivity<PreferencesActivity>()
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
    fun tabletSettingsOpensFromTheDrawerAndKeepsBothPanes() {
        assumeTrue(!context.resources.isWindowCompact())
        ActivityScenario.launch(IntentHandler::class.java).use {
            closeGetStartedScreenIfExists()
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            onView(withId(R.id.nav_settings)).perform(click())

            awaitResumedActivity<PreferencesActivity>()
            val search = composeRule.onNodeWithContentDescription(context.getString(R.string.settings_search))
            composeRule.onNodeWithText(context.getString(R.string.settings_appearance)).performClick()
            composeRule.onNodeWithText(context.getString(R.string.settings_theme_light)).assertIsDisplayed()
            search.assertIsDisplayed()
        }
    }
}
