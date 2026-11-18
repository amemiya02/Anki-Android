// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.core.view.WindowInsetsCompat
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.model.CardsOrNotes
import com.ichi2.anki.withCardBrowser
import com.ichi2.testutils.simulateSystemBars
import com.ichi2.testutils.windowInsetsOf
import org.junit.Rule
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowLooper

/** Covers the status bar, cutout, and last column on the full-screen Columns page. */
class BrowserColumnSelectionFragmentScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun portrait() =
        captureColumns(
            "portrait",
            with(targetContext) { windowInsetsOf(navBarBottom = 48.dp) },
        )

    @Test
    fun gestureNavigation() =
        captureColumns(
            "gesture_navigation",
            with(targetContext) { windowInsetsOf(navBarBottom = 24.dp) },
        )

    @Test
    fun portraitCutout() =
        captureColumns(
            "portrait_cutout",
            with(targetContext) { windowInsetsOf(navBarBottom = 48.dp, cutoutTop = 40.dp) },
        )

    @Test
    fun landscape() {
        RuntimeEnvironment.setQualifiers("+land")
        captureColumns(
            "landscape",
            with(targetContext) { windowInsetsOf(navBarRight = 48.dp, cutoutLeft = 32.dp) },
        )
    }

    private fun captureColumns(
        name: String,
        insets: WindowInsetsCompat,
    ) = withCardBrowser(noteCount = 1) { browser ->
        val fragment = BrowserColumnSelectionFragment.createInstance(CardsOrNotes.CARDS)
        fragment.showNow(browser.supportFragmentManager, "columns")
        try {
            fragment.requireDialog().window!!.simulateSystemBars(insets)
            composeRule.waitUntil(10_000) {
                ShadowLooper.idleMainLooper()
                composeRule.onAllNodesWithText("Shown").fetchSemanticsNodes().isNotEmpty()
            }
            captureScreen(name)

            val inColumns = hasAnyAncestor(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Columns"))
            composeRule.onNode(inColumns and hasScrollToIndexAction()).performScrollToNode(hasText("Position"))
            composeRule.onNodeWithText("Position").assertIsDisplayed()
            captureScreen("${name}_scrolled_to_bottom")
        } finally {
            fragment.dismissNow()
        }
    }
}
