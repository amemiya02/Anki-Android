// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.testutils.ext.triggerDeckNotFoundInLimitsMap
import com.ichi2.testutils.launchFragmentInContainer
import com.ichi2.testutils.withFragment
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowDialog
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class StudyOptionsRecoveryTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    /** Issue 21981: a refresh must offer recovery when the deck hierarchy is corrupt. */
    @Test
    fun `corrupt deck hierarchy offers Check database instead of crashing`() =
        runTest {
            triggerDeckNotFoundInLimitsMap()
            throwOnShowError = false

            launchFragmentInContainer<StudyOptionsFragment>().use { scenario ->
                advanceUntilIdle()
                advanceRobolectricLooper()
                scenario.withFragment {
                    compose.onNodeWithText("deck not found in limits map").assertIsDisplayed()
                    compose.onNodeWithText(TR.sentenceCase.checkDatabase).assertIsDisplayed()
                    val dialog = ShadowDialog.getLatestDialog()
                    assertTrue(dialog.isShowing)

                    dialog.dismiss()
                    col.fixIntegrity()
                    refreshInterface()
                    advanceUntilIdle()
                    advanceRobolectricLooper()
                    val state = assertIs<StudyOptionsState.StudyOptions>(viewModel.state)
                    assertEquals(1, state.data.newCardsToday)
                }
            }
        }
}
