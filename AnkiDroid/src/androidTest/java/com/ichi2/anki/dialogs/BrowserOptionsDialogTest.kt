// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CardBrowser
import com.ichi2.anki.CommonString
import com.ichi2.anki.model.CardsOrNotes
import com.ichi2.anki.tests.InstrumentedTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests [BrowserOptionsDialog] */
@RunWith(AndroidJUnit4::class)
class BrowserOptionsDialogTest : InstrumentedTest() {
    @get:Rule
    val activityRule = ActivityScenarioRule(CardBrowser::class.java)

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun dialogLoads() {
        activityRule.scenario.onActivity { activity ->
            BrowserOptionsDialog
                .newInstance(
                    CardsOrNotes.CARDS,
                    isTruncated = true,
                ).show(activity.supportFragmentManager, "BrowserOptionsDialog")
        }
        val choice = testContext.getString(CommonString.show_cards)
        val truncate = testContext.getString(CommonString.card_browser_truncate)
        val options = hasAnyAncestor(hasAnyDescendant(hasText(truncate)))
        composeRule.onNode(hasText(choice) and options).assertIsDisplayed().assertIsSelected()
    }
}
