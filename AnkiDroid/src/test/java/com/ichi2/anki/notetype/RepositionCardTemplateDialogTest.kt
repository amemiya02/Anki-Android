// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notetype

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.AnkiActivity
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositionCardTemplateDialogTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `only positions in the displayed range are accepted`() {
        var selectedPosition: Int? = null
        RepositionCardTemplateDialog.showInstance(startRegularActivity<AnkiActivity>(), 3) { selectedPosition = it }
        val inputField = compose.onNode(hasSetTextAction())
        val confirm = compose.onNodeWithText(getResourceString(CommonString.dialog_ok))
        confirm.assertIsNotEnabled()
        for (input in listOf("0", "4", "2147483648", "")) {
            inputField.performTextReplacement(input)
            confirm.assertIsNotEnabled()
        }
        for (input in listOf("1", "3")) {
            inputField.performTextReplacement(input)
            confirm.assertIsEnabled()
        }
        confirm.performClick()
        assertEquals(3, selectedPosition)
    }

    @Test
    fun `cancel does not reposition the template`() {
        RepositionCardTemplateDialog.showInstance(startRegularActivity<AnkiActivity>(), 3) {
            error("Cancel must not reposition the template")
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("2")
        val cancel = compose.onNodeWithText(getResourceString(CommonString.dialog_cancel))
        cancel.performClick().assertDoesNotExist()
    }
}
