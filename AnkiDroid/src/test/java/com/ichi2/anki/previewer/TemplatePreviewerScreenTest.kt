// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.previewer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.NotetypeFile
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.testutils.createTransientDirectory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class TemplatePreviewerScreenTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    override fun getCollectionStorageMode() = CollectionStorageMode.IN_MEMORY_WITH_MEDIA

    @Test
    fun `editor preview shows the template tabs and the question action`() {
        val notetype = col.notetypes.basicOptionalReversed
        val args =
            TemplatePreviewerArguments(
                notetypeFile = NotetypeFile(createTransientDirectory(), notetype),
                fields = listOf("熟悉", "Familiar with", ""),
                tags = emptyList(),
            )
        val intent = TemplatePreviewerPage.getIntent(targetContext, args)

        ActivityScenario.launch<CardViewerActivity>(intent).use {
            composeRule.onNodeWithText(targetContext.getString(CommonString.card_editor_preview_card)).assertIsDisplayed()
            val first = notetype.templatesNames.first()
            val second = targetContext.getString(CommonString.card_previewer_empty_front_indicator, notetype.templatesNames[1])
            waitForText(second)
            composeRule.onNodeWithText(first).assertIsSelected()
            val emptyTemplate = composeRule.onNodeWithText(second)
            emptyTemplate.assertIsDisplayed().performClick()
            composeRule.waitUntil(10_000) { runCatching { emptyTemplate.assertIsSelected() }.isSuccess }
            val showAnswer = composeRule.onNodeWithText(targetContext.getString(CommonString.show_answer))
            showAnswer.assertIsDisplayed().performClick()
            val showQuestion = targetContext.getString(R.string.previewer_show_question)
            waitForText(showQuestion)
            composeRule.onNodeWithText(showQuestion).assertIsDisplayed().performClick()
            waitForText(targetContext.getString(CommonString.show_answer))
            showAnswer.assertIsDisplayed()
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(10_000) {
            ShadowLooper.idleMainLooper()
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
