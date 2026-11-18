// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.managespace

import android.content.Intent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.common.utils.dp
import com.ichi2.testutils.dispatchInsets
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ManageSpaceInsetsTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `the rows clear a side navigation bar and the opposite cutout`() =
        withManageSpace { activity ->
            activity.dispatchInsets(navBarRight = 48.dp, cutoutLeft = 32.dp)

            val row = row(CommonString.pref__delete_unused_media_files__title).bounds()
            assertTrue(row.left >= 32.dp.toPx(targetContext), "row starts at ${row.left}")
            assertTrue(row.right <= activity.composeWidth - 48.dp.toPx(targetContext), "row ends at ${row.right}")
        }

    @Test
    fun `the last row scrolls above the bottom navigation bar`() =
        withManageSpace { activity ->
            activity.dispatchInsets(navBarBottom = 48.dp)

            val row = row(CommonString.pref__delete_everything__title).performScrollTo().bounds()
            assertTrue(row.bottom <= activity.composeHeight - 48.dp.toPx(targetContext), "row ends at ${row.bottom}")
        }

    @Test
    fun `a side navigation bar leaves the full height available to the list`() =
        withManageSpace { activity ->
            val before = row(CommonString.pref__delete_everything__title).performScrollTo().bounds()
            activity.dispatchInsets(navBarRight = 48.dp, bottomCornerRadius = 34.dp)
            val after = row(CommonString.pref__delete_everything__title).performScrollTo().bounds()
            assertTrue(after.top >= before.top, "last row moved up from ${before.top} to ${after.top}")
        }

    private fun row(title: Int) = compose.onNodeWithText(targetContext.getString(title))

    private fun SemanticsNodeInteraction.bounds(): Rect = fetchSemanticsNode().boundsInRoot

    private val ManageSpaceActivity.composeWidth get() = findViewById<android.view.View>(R.id.settings_manage_space).width

    private val ManageSpaceActivity.composeHeight get() = findViewById<android.view.View>(R.id.settings_manage_space).height

    private fun withManageSpace(block: (ManageSpaceActivity) -> Unit) {
        val activity =
            startActivityNormallyOpenCollectionWithIntent(
                ManageSpaceActivity::class.java,
                Intent(targetContext, ManageSpaceActivity::class.java),
            )
        advanceRobolectricLooper()
        block(activity)
    }
}
