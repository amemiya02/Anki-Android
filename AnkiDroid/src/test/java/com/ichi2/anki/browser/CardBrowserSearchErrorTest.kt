// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import android.app.Dialog
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import com.ichi2.anki.BottomNavController.NavigationItem
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.setIntroductionSlidesShown
import com.ichi2.anki.throwOnShowError
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.shadows.ShadowDialog
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(ParameterizedRobolectricTestRunner::class)
class CardBrowserSearchErrorTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @ParameterizedRobolectricTestRunner.Parameter
    @JvmField
    var embedded: Boolean = false

    @Test
    fun `search errors are displayed in each browser host - Issue 23301`() =
        withBrowser {
            throwOnShowError = false
            repeat(2) {
                activityViewModel.setQuery("is:now").join()
                awaitSearch()

                assertSearchError("now").dismiss()
            }
        }

    private fun assertSearchError(expected: String): Dialog {
        val dialog = assertNotNull(ShadowDialog.getLatestDialog(), "search error is shown")
        assertTrue(dialog.isShowing, "expected a new error dialog containing '$expected'")
        compose.onNode(hasText(expected, substring = true) and hasAnyAncestor(isDialog())).assertIsDisplayed()
        return dialog
    }

    private suspend fun CardBrowserFragment.awaitSearch() {
        activityViewModel.searchJob?.join()
        advanceRobolectricLooper()
    }

    private fun withBrowser(block: suspend CardBrowserFragment.() -> Unit) =
        if (embedded) {
            run {
                setIntroductionSlidesShown(true)
                runTest {
                    val activity = startActivityControllerNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent()).get()
                    activity.binding.bottomNavigation!!.selectedItemId = NavigationItem.BROWSER.id
                    advanceRobolectricLooper()
                    val browser = activity.currentScreenFragment as CardBrowserFragment
                    browser.activityViewModel.searchJob?.join()
                    advanceRobolectricLooper()
                    block(browser)
                }
            }
        } else {
            withCardBrowserFragment {
                activityViewModel.searchJob?.join()
                advanceRobolectricLooper()
                block()
            }
        }

    companion object {
        @ParameterizedRobolectricTestRunner.Parameters(name = "embedded={0}")
        @JvmStatic
        fun parameters() = listOf(arrayOf(false), arrayOf(true))
    }
}
