// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.export

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performSemanticsAction
import androidx.fragment.app.testing.launchFragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.libanki.Consts.DEFAULT_DECK_ID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import com.ichi2.anki.common.android.R as CommonR

@RunWith(AndroidJUnit4::class)
class ExportDialogFragmentKeyboardTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    @Config(sdk = [24, 25, 36])
    fun `deck selector can receive keyboard focus`() {
        ensureCollectionLoadIsSynchronous()
        val args = ExportDialogFragment.forDeckList(DEFAULT_DECK_ID).arguments
        launchFragment<ExportDialogFragment>(fragmentArgs = args, themeResId = CommonR.style.Theme_Light).use {
            advanceRobolectricLooper()
            val label = TR.exportingInclude().replace(Regex("<[^>]+>"), "")
            val selector =
                compose.onNode(
                    hasText(label) and hasAnyAncestor(isDialog()) and
                        SemanticsMatcher.keyIsDefined(SemanticsActions.RequestFocus),
                )
            selector.performSemanticsAction(SemanticsActions.RequestFocus)
            selector.assertIsFocused()
        }
    }
}
