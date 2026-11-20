// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.common.android.Animations
import com.ichi2.anki.libanki.sched.Counts
import com.ichi2.compose.theme.AnkiDroidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class StudyBarTest : RobolectricTest() {
    @get:Rule
    val composeRule = createComposeRule()

    private val studying =
        StudyBarState(
            deckPath = listOf("中文", "HSK 4"),
            counts = StudyCounts(newCount = 8, learnCount = 2, reviewCount = 6, activeQueue = Counts.Queue.NEW),
            minutesLeft = 6,
            progress = 0.25f,
        )

    private fun show(
        state: StudyBarState,
        titleModifier: Modifier = Modifier,
    ) {
        composeRule.setContent {
            AnkiDroidTheme {
                StudyBarTitles(state, modifier = titleModifier)
                StudyProgress(state.progress)
            }
        }
    }

    @Test
    fun `the Deck path, counts and estimate are shown`() {
        show(studying)

        composeRule.onNodeWithText("中文 › HSK 4").assertIsDisplayed()
        composeRule.onNodeWithText("8").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithText("6").assertIsDisplayed()
        composeRule.onNodeWithText("6 minutes left").assertIsDisplayed()
    }

    @Test
    fun `the study header names the three queue counts in reading order`() {
        show(studying)

        val labels = listOf("New: 8", "Learning: 2", "Due: 6")
        val bounds = labels.map { composeRule.onNodeWithContentDescription(it).assertIsDisplayed().getUnclippedBoundsInRoot() }
        assertTrue(bounds.zipWithNext().all { (first, second) -> first.right <= second.left })
        val deckBottom = composeRule.onNodeWithText("中文 › HSK 4").getUnclippedBoundsInRoot().bottom
        assertTrue(bounds.all { it.top >= deckBottom })
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `a narrow study header shows the current deck at the end of a wrapped path`() {
        val path = listOf("日语", "eggrolls-JLPT10000", "2-N4")
        val title = path.joinToString(" › ")
        show(studying.copy(deckPath = path), Modifier.width(180.dp))

        val node = composeRule.onNodeWithText(title).assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        checkNotNull(node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action)(layouts)
        val layout = layouts.single()
        val finalLine = layout.lineCount - 1
        assertFalse(layout.isLineEllipsized(finalLine), "The current deck is cut off from the end of the path")
        assertEquals(title.length, layout.getLineEnd(finalLine, visibleEnd = true))
        composeRule.onNodeWithText("8").assertIsDisplayed()
        composeRule.onNodeWithText("6 minutes left").assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `an unusually long path keeps the current deck visible and the full path accessible`() {
        val path = listOf("Japanese", "A long ancestor name ".repeat(8).trimEnd(), "2-N4")
        show(studying.copy(deckPath = path), Modifier.width(180.dp))

        composeRule.onNodeWithText("… › 2-N4").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(path.joinToString(" › ")).assertExists()
        composeRule.onNodeWithText("8").assertIsDisplayed()
    }

    @Test
    fun `the progress line says how much of the session is done`() {
        show(studying)

        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.25f, 0f..1f)))
            .assertIsDisplayed()
    }

    @Test
    fun `the progress line updates immediately with Remove animations enabled`() {
        editPreferences { putBoolean(targetContext.getString(R.string.safe_display_key), true) }
        var progress by mutableFloatStateOf(0.25f)
        composeRule.setContent {
            AnkiDroidTheme { StudyProgress(progress) }
        }
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.25f, 0f..1f))).assertIsDisplayed()
        composeRule.mainClock.autoAdvance = false
        assertFalse(Animations.areAnimationsEnabled(targetContext))
        composeRule.runOnUiThread {
            progress = 0.5f
            Snapshot.sendApplyNotifications()
        }
        composeRule.mainClock.advanceTimeBy(32)
        val indicator = composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
        assertEquals(0.5f, indicator.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current)
        indicator.assertIsDisplayed()
    }

    @Test
    fun `nothing about what is left is shown when the remaining count is hidden`() {
        show(StudyBarState(deckPath = listOf("Default")))

        composeRule.onNodeWithText("Default").assertIsDisplayed()
        val tree = composeRule.onRoot(useUnmergedTree = true).printToString()
        assertFalse("minutes left" in tree, tree)
        assertFalse("ProgressBarRangeInfo" in tree, tree)
    }
}
