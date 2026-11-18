// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ichi2.anki.CommonString
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.cardviewer.SingleCardSide
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.model.SpecialFields
import com.ichi2.anki.withCardTemplateEditor
import com.ichi2.testutils.simulateSystemBars
import com.ichi2.testutils.windowInsetsOf
import org.junit.Rule
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class InsertFieldDialogScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun portrait() =
        captureInsertField(
            "portrait",
            with(targetContext) { windowInsetsOf(navBarBottom = 48.dp) },
        )

    @Test
    fun gestureNavigation() =
        captureInsertField(
            "gesture_navigation",
            with(targetContext) { windowInsetsOf(navBarBottom = 24.dp) },
        )

    @Test
    fun portraitCutout() =
        captureInsertField(
            "portrait_cutout",
            with(targetContext) { windowInsetsOf(navBarBottom = 48.dp, cutoutTop = 40.dp) },
        )

    @Test
    fun landscape() {
        RuntimeEnvironment.setQualifiers("+land")
        captureInsertField(
            "landscape",
            with(targetContext) { windowInsetsOf(navBarRight = 48.dp, cutoutLeft = 32.dp) },
        )
    }

    private fun captureInsertField(
        name: String,
        insets: WindowInsetsCompat,
    ) = withCardTemplateEditor {
        currentFragment!!.showInsertFieldDialog()
        advanceRobolectricLooper()
        val dialog = supportFragmentManager.fragments.filterIsInstance<InsertFieldDialog>().single()
        try {
            val window = dialog.requireDialog().window!!
            window.simulateSystemBars(insets)
            captureScreen(name)

            compose.onNodeWithText(getString(CommonString.special_fields_tab_header)).performClick()
            compose.waitForIdle()
            ViewCompat.dispatchApplyWindowInsets(window.decorView, insets)
            val list = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            list.performScrollToIndex(SpecialFields.all(side = SingleCardSide.FRONT).lastIndex)
            captureScreen("${name}_special_scrolled_to_bottom")
        } finally {
            dialog.dismissNow()
        }
    }
}
