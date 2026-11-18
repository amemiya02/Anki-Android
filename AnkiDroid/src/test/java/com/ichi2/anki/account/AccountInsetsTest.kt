// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.account

import android.app.Activity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.settings.Prefs
import com.ichi2.testutils.TEST_SYNC_SERVER
import com.ichi2.testutils.dispatchInsets
import com.ichi2.testutils.setSyncServer
import com.ichi2.testutils.signInToSyncServer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AccountInsetsTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `sign-in clears the status bar, side navigation, and cutout`() =
        withAccount {
            val before = submit.getUnclippedBoundsInRoot()
            it.onActivity { activity -> activity.dispatchInsets(navBarRight = 48.dp, cutoutLeft = 32.dp) }
            val after = submit.getUnclippedBoundsInRoot()
            assertTrue(title.getUnclippedBoundsInRoot().top.value >= 24f)
            assertTrue(after.left.value >= before.left.value + 31f)
            assertTrue(after.right.value <= before.right.value - 47f)
        }

    @Test
    fun `sign-in button clears the navigation bar and rounded corners`() =
        withAccount {
            it.onActivity { activity -> activity.dispatchInsets(navBarBottom = 24.dp) }
            advanceRobolectricLooper()
            val before = submit.getUnclippedBoundsInRoot()
            it.onActivity { activity -> activity.dispatchInsets(navBarBottom = 24.dp, bottomCornerRadius = 48.dp) }
            advanceRobolectricLooper()
            val after = submit.assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue(after.bottom.value <= before.bottom.value - 23f, "Before: $before, after: $after")
        }

    @Test
    fun `the keyboard leaves sign-in and the password field reachable`() =
        withAccount {
            val before = submit.getUnclippedBoundsInRoot()
            it.onActivity { activity -> activity.dispatchInsets(navBarBottom = 48.dp, imeBottom = 300.dp) }
            val after = submit.assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue(after.bottom.value <= before.bottom.value - 299f)
            compose.onNodeWithText(targetContext.getString(CommonString.password)).performScrollTo().assertIsDisplayed()
        }

    @Test
    fun `the account and settings page clears the navigation bar`() =
        withAccount(signedIn = true) {
            it.onActivity { activity -> activity.dispatchInsets(navBarBottom = 48.dp) }
            compose.onNodeWithText(targetContext.getString(CommonString.paper_sync_now)).assertIsDisplayed()
            compose.onNodeWithText(targetContext.getString(CommonString.one_way_sync_title)).performScrollTo().assertIsDisplayed()
        }

    private val submit get() = compose.onNodeWithText(targetContext.getString(CommonString.paper_sync_sign_in))
    private val title get() = compose.onNodeWithText(targetContext.getString(CommonString.paper_sync_sign_in_title))

    private fun withAccount(
        signedIn: Boolean = false,
        block: (ActivityScenario<Activity>) -> Unit,
    ) {
        setSyncServer(TEST_SYNC_SERVER)
        Prefs.hkey = ""
        if (signedIn) {
            signInToSyncServer()
            Prefs.username = "paper"
        }
        ActivityScenario.launch<Activity>(AccountActivity.getIntent(targetContext)).use(block)
    }
}
