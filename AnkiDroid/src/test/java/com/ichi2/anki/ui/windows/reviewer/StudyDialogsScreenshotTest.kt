// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CommonString
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.preferences.reviewer.ReviewerMenuRepository
import com.ichi2.anki.preferences.reviewer.ViewerAction
import com.ichi2.anki.previewer.CardViewerActivity
import com.ichi2.anki.settings.Prefs
import com.ichi2.testutils.ext.clear
import com.ichi2.testutils.ext.reopenWithLanguage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.robolectric.shadows.ShadowLooper

@CheckAccessibility
class StudyDialogsScreenshotTest : ScreenshotTest() {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    override fun getCollectionStorageMode() = CollectionStorageMode.IN_MEMORY_WITH_MEDIA

    @After
    override fun tearDown() {
        super.tearDown()
        Prefs.clear()
    }

    @Test
    fun `cancel reset progress returns to the card being studied`() {
        runBlocking { col.reopenWithLanguage(language.option) }
        val deck = addDeck("Ginkgo", setAsSelected = true)
        val card = addNoteUsingNoteTypeName(col.notetypes.current().name, "What does a ginkgo leaf look like?", "Fan-shaped").firstCard()
        col.setDeck(listOf(card.id), deck)
        ReviewerMenuRepository(targetContext.sharedPrefs()).setDisplayTypeActions(
            alwaysShowActions = listOf(ViewerAction.RESET_PROGRESS),
            menuOnlyActions = emptyList(),
            disabledActions = emptyList(),
        )
        ActivityScenario.launch<CardViewerActivity>(ReviewerFragment.getIntent(targetContext)).use { scenario ->
            scenario.onActivity { (it.fragment as ReviewerFragment).viewModel.onPageFinished(false) }
            composeRule.waitUntil(10_000) {
                ShadowLooper.idleMainLooper()
                composeRule.onAllNodesWithText("Ginkgo", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            onView(withContentDescription(ViewerAction.RESET_PROGRESS.title(targetContext))).perform(click())
            val title = targetContext.getString(CommonString.reset_card_dialog_title)
            composeRule.waitUntil(10_000) {
                ShadowLooper.idleMainLooper()
                composeRule.onNodeWithText(title).isDisplayed()
            }
            composeRule.onNodeWithText(title).assertIsDisplayed()
            composeRule.onNodeWithText(targetContext.getString(CommonString.reset)).assertIsDisplayed()
            captureScreen("reset_progress")
            composeRule.onNodeWithText(targetContext.getString(CommonString.dialog_cancel)).performClick()
            composeRule.onNodeWithText(title).assertDoesNotExist()
            composeRule.onNodeWithText(targetContext.getString(CommonString.show_answer)).assertIsDisplayed()
        }
    }
}
