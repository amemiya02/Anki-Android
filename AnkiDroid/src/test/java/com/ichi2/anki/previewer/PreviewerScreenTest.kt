// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.previewer

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.browser.IdsFile
import com.ichi2.testutils.createTransientDirectory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.appcompat.R as AppCompatR

@RunWith(AndroidJUnit4::class)
class PreviewerScreenTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    override fun getCollectionStorageMode() = CollectionStorageMode.IN_MEMORY_WITH_MEDIA

    @Test
    fun `browser preview shows the current position and the question action`() {
        val ids = addBasicAndReversedNote().cardIds(col)
        val selection = IdsFile(createTransientDirectory(), ids, IdsFile.Purpose.PREVIEW)
        val intent = PreviewerFragment.getIntent(targetContext, selection, currentIndex = 1)

        ActivityScenario.launch<CardViewerActivity>(intent).use {
            composeRule.onNodeWithText("2 of 2").assertIsDisplayed()
            composeRule.onNodeWithText(targetContext.getString(CommonString.show_answer)).assertIsDisplayed()
        }
    }

    @Test
    fun `answer then next navigates the selection and the slider can jump back`() {
        val ids = addBasicAndReversedNote().cardIds(col)
        val selection = IdsFile(createTransientDirectory(), ids, IdsFile.Purpose.PREVIEW)
        val intent = PreviewerFragment.getIntent(targetContext, selection, currentIndex = 0)

        ActivityScenario.launch<CardViewerActivity>(intent).use {
            val showAnswer = composeRule.onNodeWithText(targetContext.getString(CommonString.show_answer))
            val next = composeRule.onNodeWithText(targetContext.getString(CommonString.previewer_next))
            showAnswer.performClick()
            composeRule.waitUntil(10_000) { runCatching { next.assertIsDisplayed() }.isSuccess }
            next.performClick()
            composeRule.waitUntil(10_000) { runCatching { showAnswer.assertIsDisplayed() }.isSuccess }
            composeRule.onNodeWithText("2 of 2").assertIsDisplayed()

            composeRule.onNode(slider).performSemanticsAction(SemanticsActions.SetProgress) { setPosition ->
                setPosition(1f)
            }
            composeRule.waitUntil(10_000) {
                runCatching { composeRule.onNodeWithText("1 of 2").assertIsDisplayed() }.isSuccess
            }
        }
    }

    @Test
    fun `single card has no slider and back side only has no next card`() {
        val ids = addBasicNote().cardIds(col)
        val selection = IdsFile(createTransientDirectory(), ids, IdsFile.Purpose.PREVIEW)
        val intent = PreviewerFragment.getIntent(targetContext, selection, currentIndex = 0)

        ActivityScenario.launch<CardViewerActivity>(intent).use {
            composeRule.onNode(slider).assertDoesNotExist()
            composeRule.onNodeWithText("1 of 1").assertDoesNotExist()
            composeRule
                .onNodeWithContentDescription(targetContext.getString(AppCompatR.string.abc_action_menu_overflow_description))
                .performClick()
            composeRule.onNodeWithText(targetContext.getString(R.string.previewer_show_back_only)).performClick()
            val next = composeRule.onNodeWithText(targetContext.getString(CommonString.previewer_next))
            composeRule.waitUntil(10_000) { runCatching { next.assertIsDisplayed() }.isSuccess }
            next.assertIsNotEnabled()
        }
    }

    private val slider get() = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)
}
