// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2026 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.core.view.isVisible
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.BottomNavController.NavigationItem
import com.ichi2.testutils.BackupManagerTestUtilities
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class BottomNavigationRestoreTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `a configuration change keeps the open tab on screen`() =
        forEachTab { tab ->
            launchOn(tab).use { controller ->
                controller.recreate()
                assertShowsTab(controller.get(), tab)
            }
        }

    @Test
    fun `reselecting the restored Browse tab opens search`() =
        withBottomNavigation {
            val controller = launchOn(NavigationItem.BROWSER)
            controller.recreate()
            advanceRobolectricLooper()

            val deckPicker = controller.get()
            assertShowsTab(deckPicker, NavigationItem.BROWSER)
            composeRule.onNode(hasSetTextAction()).assertDoesNotExist()

            val shortcut = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_2, 0, KeyEvent.META_ALT_ON)
            assertThat(deckPicker.dispatchKeyEvent(shortcut), equalTo(true))
            composeRule.onNode(hasSetTextAction()).assertDoesNotExist()

            composeRule.onNodeWithTag("tab-browser").performClick()
            composeRule.waitUntil(10_000) { !deckPicker.bottomNav.isVisible }
            composeRule.onNode(hasSetTextAction()).assertIsDisplayed()
        }

    @Test
    fun `restoring after process death keeps the open tab on screen`() =
        forEachTab { tab ->
            restoreAfterProcessDeath(launchOn(tab)).use { restored ->
                assertShowsTab(restored.get(), tab)
            }
        }

    @Test
    fun `back returns to the deck list after a restore`() =
        forEachTab { tab ->
            restoreAfterProcessDeath(launchOn(tab)).use { restored ->
                val deckPicker = restored.get()
                assertShowsTab(deckPicker, tab)

                deckPicker.onBackPressedDispatcher.onBackPressed()
                composeRule.waitUntil(10_000) {
                    deckPicker.deckList.isShown && deckPicker.bottomNav.selectedItemId == NavigationItem.HOME.id
                }

                assertThat("$tab: back selects Home", deckPicker.bottomNav.selectedItemId, equalTo(NavigationItem.HOME.id))
                assertThat("$tab: back shows the deck list", deckPicker.deckList.isShown, equalTo(true))
            }
        }

    @Test
    fun `reloading decks while another tab is open clears the home progress indicator`() =
        withBottomNavigation {
            val controller = launchOn(NavigationItem.MORE)
            controller.recreate()
            val deckPicker = controller.get()
            assertShowsTab(deckPicker, NavigationItem.MORE)
            composeRule.waitForIdle()
            val progress = deckPicker.deckPickerBinding.root.findViewById<View>(R.id.progress_bar)
            assertThat(progress.isAttachedToWindow, equalTo(false))

            val reload = deckPicker.viewModel.updateDeckList()
            advanceRobolectricLooperUntil { reload.isCompleted }
            val counts = requireNotNull(deckPicker.viewModel.loadDeckCounts)
            advanceRobolectricLooperUntil { counts.isCompleted }
            advanceRobolectricLooper()
            assertThat(progress.isVisible, equalTo(false))

            deckPicker.select(NavigationItem.HOME)
            assertThat(progress.isVisible, equalTo(false))
        }

    @Test
    fun `restoring on Home after the browser was opened`() =
        withBottomNavigation {
            val controller = launchOn(NavigationItem.BROWSER)
            controller.get().select(NavigationItem.HOME)

            val deckPicker = restoreAfterProcessDeath(controller).get()

            composeRule.waitUntil(10_000) { deckPicker.deckList.isShown }
            assertThat(deckPicker.bottomNav.selectedItemId, equalTo(NavigationItem.HOME.id))
            assertThat(deckPicker.deckList.isShown, equalTo(true))
        }

    @Test
    fun `restoring a browser on a screen with the side drawer`() =
        withBottomNavigation {
            val controller = launchOn(NavigationItem.BROWSER)

            // e.g. a foldable opened: the split-pane layout has the drawer and no bottom bar
            RuntimeEnvironment.setQualifiers("w1280dp-h800dp-xlarge")
            val deckPicker = restoreAfterProcessDeath(controller).get()

            assertThat(deckPicker.binding.bottomNavigation, nullValue())
            assertThat(deckPicker.hasNavigationDrawer, equalTo(true))
        }

    private fun withBottomNavigation(test: () -> Unit) {
        setIntroductionSlidesShown(true)
        BackupManagerTestUtilities.setupSpaceForBackup(targetContext)
        test()
    }

    private fun forEachTab(test: (NavigationItem) -> Unit) =
        withBottomNavigation {
            listOf(NavigationItem.BROWSER, NavigationItem.STATS, NavigationItem.MORE).forEach(test)
        }

    private fun launchOn(tab: NavigationItem): ActivityController<DeckPicker> =
        startActivityControllerNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent()).also {
            it.get().select(tab)
        }

    private fun DeckPicker.select(tab: NavigationItem) {
        composeRule.onNodeWithTag("tab-${tab.tag}").performClick()
        composeRule.waitUntil(10_000) {
            when (tab) {
                NavigationItem.HOME -> currentScreenFragment == null && deckList.isShown
                else -> currentScreenFragment?.isVisible == true
            }
        }
    }

    private fun restoreAfterProcessDeath(controller: ActivityController<DeckPicker>): ActivityController<DeckPicker> {
        composeRule.waitForIdle()
        val savedState = Bundle()
        controller
            .pause()
            .stop()
            .saveInstanceState(savedState)
            .destroy()
        val restored =
            Robolectric
                .buildActivity(DeckPicker::class.java, Intent())
                .setup(savedState)
        saveControllerForCleanup(restored)
        advanceRobolectricLooper()
        return restored
    }

    private fun assertShowsTab(
        deckPicker: DeckPicker,
        tab: NavigationItem,
    ) {
        composeRule.waitUntil(10_000) {
            advanceRobolectricLooper()
            deckPicker.currentScreenFragment?.isVisible == true && !deckPicker.deckList.isShown &&
                deckPicker.bottomNav.selectedTab == tab &&
                deckPicker.bottomNavContentClearance.intValue == deckPicker.bottomNav.height
        }
        val screen = deckPicker.currentScreenFragment!!
        assertThat("$tab is selected", deckPicker.bottomNav.selectedItemId, equalTo(tab.id))
        assertThat("$tab content is shown", screen.isVisible, equalTo(true))
        assertThat("$tab hides the deck list", deckPicker.deckList.isShown, equalTo(false))
        assertThat("$tab fragment is not hidden", screen.isHidden, equalTo(false))
        assertThat(
            "$tab content clears the bar",
            deckPicker.bottomNavContentClearance.intValue,
            equalTo(deckPicker.bottomNav.height),
        )
    }

    private val DeckPicker.bottomNav: DeckBottomBar
        get() = binding.bottomNavigation!!

    private val DeckPicker.deckList: View
        get() = binding.deckPickerContentWrapper!!
}
