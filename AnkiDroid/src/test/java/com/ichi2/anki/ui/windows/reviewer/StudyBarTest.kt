// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.libanki.sched.Counts
import com.ichi2.compose.theme.AnkiDroidTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse

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

    private fun show(state: StudyBarState) {
        composeRule.setContent {
            AnkiDroidTheme {
                StudyBarTitles(state)
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
    fun `the progress line says how much of the session is done`() {
        show(studying)

        composeRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.25f, 0f..1f)))
            .assertIsDisplayed()
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
