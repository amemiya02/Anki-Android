// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2026 Bhaskar Patel <patel.bhaskar09@gmail.com>

package com.ichi2.anki.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ichi2.anki.CardBrowser
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.ui.internationalization.sentenceCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests [GradeNowDialog] */
@RunWith(RobolectricTestRunner::class)
class GradeNowDialogTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun dialogLoads() {
        val cardId = addBasicNote().firstCard().id
        val cardBrowser = super.startRegularActivity<CardBrowser>()

        val translatedTitle = with(cardBrowser) { TR.sentenceCase.gradeNow }

        GradeNowDialog.showDialog(cardBrowser, listOf(cardId))

        composeRule.onNodeWithText(translatedTitle).assertIsDisplayed()
    }

    @Test
    fun showsAllGradeOptions() {
        val cardId = addBasicNote().firstCard().id
        val cardBrowser = super.startRegularActivity<CardBrowser>()

        GradeNowDialog.showDialog(cardBrowser, listOf(cardId))

        composeRule.onNodeWithText(TR.studyingAgain()).assertIsDisplayed()

        composeRule.onNodeWithText(TR.studyingHard()).assertIsDisplayed()

        composeRule.onNodeWithText(TR.studyingGood()).assertIsDisplayed()

        composeRule.onNodeWithText(TR.studyingEasy()).assertIsDisplayed()
    }

    @Test
    fun clickingGradeDismissesDialog() {
        val cardId = addBasicNote().firstCard().id
        val cardBrowser = super.startRegularActivity<CardBrowser>()

        val translatedTitle = with(cardBrowser) { TR.sentenceCase.gradeNow }

        GradeNowDialog.showDialog(cardBrowser, listOf(cardId))

        composeRule.onNodeWithText(TR.studyingGood()).performClick()

        composeRule.onNodeWithText(translatedTitle).assertDoesNotExist()
    }

    @Test
    fun dialogNotShownIfNoCardsSelected() {
        val cardBrowser = super.startRegularActivity<CardBrowser>()
        val translatedTitle = with(cardBrowser) { TR.sentenceCase.gradeNow }

        GradeNowDialog.showDialog(cardBrowser, emptyList())

        composeRule.onNodeWithText(translatedTitle).assertDoesNotExist()
    }
}
