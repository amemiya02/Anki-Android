// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.reviewreminders

import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.ichi2.anki.CommonString
import com.ichi2.anki.InitialActivity
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.utils.ConfigAwareSingleFragmentActivity
import com.ichi2.testutils.dispatchInsets
import com.ichi2.testutils.withWritePermissions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.ui.unit.dp as composeDp

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ReviewRemindersInsetsTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `the reminder heading and add button clear the cutout and system bars`() =
        withStandaloneScheduleReminders { activity ->
            val title = compose.onNodeWithText(activity.getString(CommonString.review_reminders_title))
            val add = compose.onNodeWithContentDescription(activity.getString(CommonString.add_reminder))
            val initialTitle = title.getUnclippedBoundsInRoot()
            val initialAdd = add.getUnclippedBoundsInRoot()

            activity.dispatchInsets(navBarBottom = 48.dp, cutoutLeft = 32.dp)
            advanceRobolectricLooper()

            val titleWithInsets = title.getUnclippedBoundsInRoot()
            val addWithInsets = add.getUnclippedBoundsInRoot()
            assertTrue(titleWithInsets.left >= initialTitle.left + 32.composeDp)
            assertTrue(addWithInsets.bottom <= initialAdd.bottom - 48.composeDp)
        }

    @Test
    fun `the last list action clears a side navigation bar`() =
        withStandaloneScheduleReminders { activity ->
            val window = compose.onNode(isRoot()).getUnclippedBoundsInRoot()
            activity.dispatchInsets(navBarRight = 48.dp)
            advanceRobolectricLooper()
            val label = activity.getString(CommonString.schedule_reminders_notification_settings_menu)
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(label))
            val settings = compose.onNodeWithText(label).getUnclippedBoundsInRoot()
            assertTrue(settings.right <= window.right - 48.composeDp)
        }

    @Test
    fun `the last troubleshooting action clears the navigation bar`() {
        targetContext.setTroubleshootingChecks(batteryOptimization = CheckResult.Warning)
        withTroubleshootingFragment {
            val window = compose.onNode(isRoot()).getUnclippedBoundsInRoot()
            requireActivity().dispatchInsets(navBarBottom = 48.dp)
            advanceRobolectricLooper()
            val copy = compose.onNodeWithText(getString(R.string.review_reminder_check_copy)).performScrollTo().getUnclippedBoundsInRoot()
            assertTrue(copy.bottom <= window.bottom - 48.composeDp)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun `the standalone window renders into a display cutout`() =
        withStandaloneScheduleReminders { activity ->
            assertEquals(
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS,
                activity.window.attributes.layoutInDisplayCutoutMode,
            )
        }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    @Suppress("DEPRECATION")
    fun `the standalone window stays edge to edge after pausing`() =
        withWritePermissions {
            InitialActivity.decideStorageIfUndecided(targetContext)
            val intent = ScheduleRemindersFragment.getIntent(targetContext, ReviewReminderScope.Global)
            ActivityScenario.launch<ConfigAwareSingleFragmentActivity>(intent).use { scenario ->
                advanceRobolectricLooper()
                scenario.onActivity { activity ->
                    assertEquals(DECOR_FITS_FLAGS, activity.window.decorView.systemUiVisibility and DECOR_FITS_FLAGS)
                }
                scenario.moveToState(Lifecycle.State.STARTED)
                advanceRobolectricLooper()
                scenario.onActivity { activity ->
                    assertEquals(DECOR_FITS_FLAGS, activity.window.decorView.systemUiVisibility and DECOR_FITS_FLAGS)
                }
            }
        }

    companion object {
        @Suppress("DEPRECATION")
        private const val DECOR_FITS_FLAGS =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
}
