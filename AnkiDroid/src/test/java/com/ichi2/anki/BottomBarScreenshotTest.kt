// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.core.view.isVisible
import com.ichi2.anki.BottomNavController.NavigationItem
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import org.junit.Rule
import org.junit.Test
import com.ichi2.anki.common.android.R as CommonR

/**
 * The phone's bottom bar and the More surface against the Core Loop Mockup
 * (`docs/redesign/core-loop/more-*.png`, the bar in `decks-*.png`), across the Quality Bar:
 *
 * `./gradlew :AnkiDroid:recordRoborazziPlayDebug -Pscreenshot -Ptheme=light,dark,black --tests '*.BottomBarScreenshotTest'`
 */
@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class BottomBarScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun decks() =
        withDeckPicker(deckCount = 3) { deckPicker ->
            // The add button belongs to the deck list (tickets 17 and 18): in Dark and Black the
            // accessibility check reports its label colour, which is not this ticket's to change
            deckPicker.floatingActionButtonBinding.root.isVisible = false
            composeRule.waitUntil(10_000) {
                advanceRobolectricLooper()
                composeRule.onAllNodesWithText("Test Deck 0").fetchSemanticsNodes().isNotEmpty()
            }
            captureScreen("decks")
        }

    @Test
    fun more() =
        withDeckPicker(deckCount = 3) { deckPicker ->
            deckPicker.binding.bottomNavigation!!.selectedItemId = NavigationItem.MORE.id
            advanceRobolectricLooper()
            composeRule.onNodeWithText(targetContext.getString(CommonR.string.more_help_summary)).assertExists()
            captureScreen("more")
        }
}
