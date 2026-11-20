// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser.search

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.browser.BrowserColumnKey
import com.ichi2.anki.getBrowserWithNotes
import com.ichi2.anki.model.SortType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SortOrderBottomSheetFragmentTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun sortSheetShowsTheCurrentColumnAndDirection() {
        val browser = getBrowserWithNotes(noteCount = 0)
        browser.viewModel.setSortType(SortType.CollectionOrdering(BrowserColumnKey("noteFld"), reverse = true))
        advanceRobolectricLooper()
        compose.onNodeWithContentDescription(getResourceString(R.string.browse_sort)).performClick()
        compose.onNodeWithText(getResourceString(CommonString.card_browser_order_no_sorting_title)).assertIsDisplayed()
        val option = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        compose.onNode(hasText("Sort Field") and option).assertIsSelected()
        val descending = getResourceString(CommonString.sort_pill_a11y_descending)
        // the sheet's list is itself a radio group; the browser behind it scrolls too, with a radio group inside
        val radioList = hasScrollToIndexAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)
        compose.onNode(radioList).performScrollToNode(hasText(descending))
        compose.onNodeWithText(descending).assertIsSelected()
    }
}
