// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog
import com.ichi2.anki.dialogs.customstudy.CustomStudyDialog.ContextMenuOption.STUDY_AHEAD
import com.ichi2.anki.libanki.Consts.DEFAULT_DECK_ID
import com.ichi2.testutils.launchFragment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomStudyInputRegressionTest : RobolectricTest() {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun zeroDaysShowsTheMinimumInsteadOfKeepingThePreviousValue() {
        val arguments = CustomStudyDialog.createSubDialog(DEFAULT_DECK_ID, STUDY_AHEAD).arguments
        launchFragment<CustomStudyDialog>(arguments).use {
            compose.onNode(hasSetTextAction()).performTextReplacement("0")
            compose.onNodeWithText("Minimum value is 1").assertIsDisplayed()
        }
    }
}
