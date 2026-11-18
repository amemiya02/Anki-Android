// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.common.utils.dp
import com.ichi2.testutils.dispatchInsets
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class IntroductionInsetsTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `welcome controls clear a side navigation bar and the opposite cutout`() {
        ActivityScenario.launch(IntroductionActivity::class.java).use {
            val start = compose.onNodeWithText(targetContext.getString(CommonString.paper_welcome_start_empty))
            val before = start.getUnclippedBoundsInRoot()
            it.onActivity { activity -> activity.dispatchInsets(navBarRight = 48.dp, cutoutLeft = 32.dp) }
            val after = start.performScrollTo().assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue(after.left.value >= before.left.value + 31f)
            assertTrue(after.right.value <= before.right.value - 47f)
        }
    }

    @Test
    fun `the migration action clears larger bottom corners`() {
        ActivityScenario.launch(IntroductionActivity::class.java).use {
            compose.onNodeWithText(targetContext.getString(CommonString.paper_sync_from_ankiweb)).performScrollTo().performClick()
            val chooseFile = compose.onNodeWithText(targetContext.getString(CommonString.paper_sync_choose_file))
            it.onActivity { activity -> activity.dispatchInsets(navBarBottom = 24.dp) }
            val before = chooseFile.getUnclippedBoundsInRoot()
            it.onActivity { activity -> activity.dispatchInsets(navBarBottom = 24.dp, bottomCornerRadius = 48.dp) }
            val after = chooseFile.assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue(after.bottom.value <= before.bottom.value - 23f, "Before: $before, after: $after")
        }
    }
}
