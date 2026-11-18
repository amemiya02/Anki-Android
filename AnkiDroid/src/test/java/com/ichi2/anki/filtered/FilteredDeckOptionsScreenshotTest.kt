// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.filtered

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import anki.decks.copy
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.utils.ConfigAwareSingleFragmentActivity
import com.ichi2.testutils.dispatchInsets
import com.ichi2.testutils.ext.reopenWithLanguage
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.Rule
import org.junit.Test
import org.robolectric.RuntimeEnvironment

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class FilteredDeckOptionsScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun newFilteredDeckAndItsOptions() =
        withOptions {
            compose.onNodeWithText(TR.decksBuild()).assertIsDisplayed()
            captureScreen("new_deck")
            compose.onNodeWithText(TR.decksUnmovableCards()).performScrollTo().assertIsDisplayed()
            captureScreen("new_deck_options")
        }

    @Test
    fun existingFilteredDeckWithLandscapeCutout() {
        setPhoneQualifiers()
        RuntimeEnvironment.setQualifiers("+land")
        withOptions(existingDeck = true, usePhoneQualifiers = false) { activity ->
            activity.dispatchInsets(cutoutTop = 32.dp, navBarBottom = 24.dp)
            compose.onNodeWithText(TR.actionsRebuild()).assertIsDisplayed()
            compose.onNodeWithText(TR.decksUnmovableCards()).performScrollTo().assertIsDisplayed()
            captureScreen("existing_deck_landscape_options")
        }
    }

    @Test
    fun secondFilterAndPreviewDelays() =
        withOptions {
            compose.onNodeWithText(TR.decksEnableSecondFilter()).performScrollTo().performClick()
            compose.onAllNodes(hasText(TR.actionsSearch()))[1].performScrollTo().performTextReplacement("is:new")
            compose.onNodeWithText("is:new").assertIsDisplayed()
            captureScreen("second_filter")

            compose.onNodeWithText(TR.decksRescheduleCardsBasedOnMyAnswers()).performScrollTo().performClick()
            compose.onNodeWithText(getResourceString(CommonString.filtered_option_delay_again)).performScrollTo().assertIsDisplayed()
            captureScreen("preview_delays")
        }

    private fun withOptions(
        existingDeck: Boolean = false,
        usePhoneQualifiers: Boolean = true,
        block: (ConfigAwareSingleFragmentActivity) -> Unit,
    ) {
        if (usePhoneQualifiers) setPhoneQualifiers()
        runTest { col.reopenWithLanguage(language.option) }
        val deckName = "Filtered deck"
        val deckId = if (existingDeck) col.decks.newFiltered(deckName) else 0L
        val scheduler = col.sched
        val options = scheduler.getOrCreateFilteredDeck(deckId).copy { name = deckName }
        mockkObject(scheduler)
        try {
            every { scheduler.getOrCreateFilteredDeck(deckId) } returns options
            val activity =
                startActivityNormallyOpenCollectionWithIntent(
                    ConfigAwareSingleFragmentActivity::class.java,
                    FilteredDeckOptionsFragment.getIntent(targetContext, did = deckId),
                )
            activity.dispatchInsets(navBarBottom = 24.dp)
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasText(TR.deckConfigNamePrompt())).fetchSemanticsNodes().isNotEmpty()
            }
            block(activity)
        } finally {
            unmockkObject(scheduler)
        }
    }
}
