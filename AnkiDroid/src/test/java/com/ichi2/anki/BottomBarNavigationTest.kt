// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.graphics.drawable.LayerDrawable
import android.view.View
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.view.isVisible
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ichi2.anki.BottomNavController.NavigationItem
import com.ichi2.anki.browser.CardBrowserFragment
import com.ichi2.anki.dialogs.help.HelpDialog
import com.ichi2.anki.pages.Statistics
import com.ichi2.anki.preferences.PreferencesFragment
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.nullValue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import com.ichi2.anki.common.android.R as CommonR

/**
 * On a phone, Decks, Browse, Stats and More are one tap away in the bottom bar, and the side
 * drawer is gone (ticket 15).
 */
@RunWith(AndroidJUnit4::class)
class BottomBarNavigationTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `a phone shows the bottom bar without any setting`() =
        withDeckPicker(deckCount = 1) { deckPicker ->
            assertThat(deckPicker.bottomBar.isVisible, equalTo(true))
            assertThat(deckPicker.bottomBar.selectedItemId, equalTo(NavigationItem.HOME.id))
        }

    @Test
    fun `the phone bar draws the Paper background and hairline`() =
        withDeckPicker(deckCount = 1) { deckPicker ->
            val bar = deckPicker.bottomBar
            assertThat(bar.background, instanceOf(LayerDrawable::class.java))
            assertThat(bar.minimumHeight, equalTo(deckPicker.resources.getDimensionPixelSize(R.dimen.bottom_bar_height)))
        }

    @Test
    fun `a phone has no side drawer`() =
        withDeckPicker(deckCount = 1) { deckPicker ->
            assertThat(deckPicker.findViewById<View>(R.id.navdrawer_items_container), nullValue())
            assertThat("no drawer button on Decks", deckPicker.toolbarNavigationIcon(), nullValue())
        }

    @Test
    fun `each tab opens its existing screen`() =
        withDeckPicker(deckCount = 1) { deckPicker ->
            mapOf(
                NavigationItem.BROWSER to CardBrowserFragment::class.java,
                NavigationItem.STATS to Statistics::class.java,
                NavigationItem.MORE to MoreFragment::class.java,
            ).forEach { (tab, screen) ->
                deckPicker.select(tab)
                val shown = deckPicker.currentScreenFragment
                assertThat("$tab opens its screen", shown, instanceOf(screen))
                assertThat("$tab is on screen", shown!!.isVisible, equalTo(true))
                assertThat("$tab hides Decks", deckPicker.binding.deckPickerContentWrapper!!.isShown, equalTo(false))
            }
        }

    @Test
    fun `back returns to Decks from another tab`() =
        withDeckPicker(deckCount = 1) { deckPicker ->
            listOf(NavigationItem.BROWSER, NavigationItem.STATS, NavigationItem.MORE).forEach { tab ->
                deckPicker.select(tab)
                deckPicker.title = "Edit note"

                deckPicker.onBackPressedDispatcher.onBackPressed()
                composeRule.waitUntil(10_000) {
                    deckPicker.binding.deckPickerContentWrapper?.isShown == true && deckPicker.bottomBar.selectedItemId == NavigationItem.HOME.id
                }

                assertThat("$tab: back selects Decks", deckPicker.bottomBar.selectedItemId, equalTo(NavigationItem.HOME.id))
                assertThat("$tab: back shows Decks", deckPicker.binding.deckPickerContentWrapper!!.isShown, equalTo(true))
                assertThat("$tab: back restores the Decks host title", deckPicker.title.toString(), equalTo(deckPicker.getString(R.string.app_name)))
                assertThat("$tab: back stays in the app", deckPicker.isFinishing, equalTo(false))
            }
        }

    @Test
    fun `back on Decks leaves the app`() =
        withDeckPicker(deckCount = 1) { deckPicker ->
            deckPicker.select(NavigationItem.STATS)
            deckPicker.select(NavigationItem.HOME)

            deckPicker.onBackPressedDispatcher.onBackPressed()
            advanceRobolectricLooperUntil { deckPicker.isFinishing }

            assertThat(deckPicker.isFinishing, equalTo(true))
        }

    @Test
    fun `More lists Settings, Help and Support, each opening what the drawer opened`() =
        withDeckPicker(deckCount = 1) { deckPicker ->
            deckPicker.select(NavigationItem.MORE)

            composeRule.onNodeWithText("Settings").performClick()
            composeRule.waitUntil(10_000) { deckPicker.currentScreenFragment is PreferencesFragment }
            assertThat("Settings opens in the existing activity", shadowOf(deckPicker).nextStartedActivity, nullValue())
            assertThat(deckPicker.currentScreenFragment, instanceOf(PreferencesFragment::class.java))
            deckPicker.onBackPressedDispatcher.onBackPressed()
            composeRule.waitUntil(10_000) { deckPicker.currentScreenFragment is MoreFragment }

            composeRule.onNodeWithText("Help").performClick()
            val restoredHost =
                composeRule.runOnIdle {
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).filterIsInstance<DeckPicker>().single()
                }
            assertThat("Help opens the help menu", restoredHost.shownHelpMenuTitle(), equalTo(CommonR.string.help))
            restoredHost.dismissDialogs()

            composeRule.onNodeWithText("Support AnkiDroid").performClick()
            assertThat(
                "Support opens the support menu",
                restoredHost.shownHelpMenuTitle(),
                equalTo(CommonR.string.help_title_support_ankidroid),
            )
        }

    private fun DeckPicker.shownHelpMenuTitle(): Int? {
        composeRule.waitUntil(10_000) { supportFragmentManager.fragments.any { it is HelpDialog } }
        val dialog = supportFragmentManager.fragments.filterIsInstance<HelpDialog>().singleOrNull()
        return dialog?.requireArguments()?.getInt(HelpDialog.ARG_MENU_TITLE)
    }

    private fun DeckPicker.dismissDialogs() {
        supportFragmentManager.fragments.filterIsInstance<HelpDialog>().forEach { it.dismissNow() }
        advanceRobolectricLooper()
    }

    private val DeckPicker.bottomBar: BottomNavigationView
        get() = binding.bottomNavigation!!

    private fun DeckPicker.toolbarNavigationIcon() = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).navigationIcon

    private fun DeckPicker.select(tab: NavigationItem) {
        bottomBar.selectedItemId = tab.id
        composeRule.waitUntil(10_000) {
            when (tab) {
                NavigationItem.HOME -> currentScreenFragment == null && binding.deckPickerContentWrapper?.isShown == true
                else -> currentScreenFragment?.isVisible == true
            }
        }
    }
}
