// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.export

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.fragment.app.testing.launchFragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.browser.IdsFile
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.requireParcelable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowToast
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class BrowserSelectionExportTest : RobolectricTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun `browser export opens the deck package with the selection as its source`() {
        val args = ExportDialogFragment.newInstance(targetContext.cacheDir, ExportDialogFragment.ExportType.Cards, listOf(1L)).arguments
        launchFragment<ExportDialogFragment>(fragmentArgs = args, themeResId = CommonR.style.Theme_Light).use {
            composeRule.onNodeWithText("(.apkg)", substring = true).assertIsDisplayed()
            composeRule.onNodeWithText(TR.exportingSelectedNotes()).assertIsDisplayed()
        }
    }

    @Test
    fun `missing selection cancels export`() = assertUnavailableSelection { assertTrue(delete()) }

    @Test
    fun `truncated selection cancels export`() = assertUnavailableSelection { writeBytes(readBytes().dropLast(1).toByteArray()) }

    private fun assertUnavailableSelection(changeFile: IdsFile.() -> Unit) {
        for (type in ExportDialogFragment.ExportType.entries) {
            for (formatIndex in 0..2) {
                val args = ExportDialogFragment.newInstance(targetContext.cacheDir, type, listOf(1L)).requireArguments()
                launchFragment<ExportDialogFragment>(fragmentArgs = args, themeResId = CommonR.style.Theme_Light).use { scenario ->
                    lateinit var format: String
                    scenario.onFragment { fragment ->
                        format =
                            with(
                                fragment,
                            ) {
                                listOf(
                                    TR.sentenceCase.ankiDeckPackage,
                                    TR.sentenceCase.notesInPlainText,
                                    TR.sentenceCase.cardsInPlainText,
                                )[formatIndex]
                            }
                    }
                    composeRule.onNodeWithText("(.apkg)", substring = true).performClick()
                    composeRule.onNodeWithText(format, substring = true).performClick()
                    args.requireParcelable<IdsFile>("arg_ids_file").changeFile()
                    ShadowToast.reset()
                    composeRule.onNode(hasText(TR.actionsExport()) and hasClickAction()).performClick()
                    advanceRobolectricLooper()
                    assertEquals(targetContext.getString(CommonString.something_wrong), ShadowToast.getTextOfLatestToast())
                }
            }
        }
    }
}
