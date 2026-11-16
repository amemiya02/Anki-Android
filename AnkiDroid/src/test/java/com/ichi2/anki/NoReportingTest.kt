// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.preferences.PreferenceTestUtils
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import kotlin.test.assertFailsWith
import com.ichi2.anki.common.android.R as CommonR

/**
 * The Fork never sends a crash report to Upstream's server and never sends usage
 * statistics anywhere (spec: "Sync and reporting").
 */
@RunWith(AndroidJUnit4::class)
class NoReportingTest : RobolectricTest() {
    @Test
    fun `the app carries no crash report sender`() {
        assertFailsWith<ClassNotFoundException>("ACRA would send crash reports to Upstream's server") {
            Class.forName("org.acra.ACRA")
        }
    }

    @Test
    fun `the app has no crash report server address`() {
        val fieldNames = BuildConfig::class.java.fields.map { it.name }
        assertThat(fieldNames, not(hasItem("ACRA_URL")))
    }

    @Test
    fun `settings have no entry for crash reports`() {
        val keys = PreferenceTestUtils.getAllPreferenceKeys(targetContext)

        assertThat("error reporting mode", keys, not(hasItem("reportErrorMode")))
    }

    @Test
    fun `a failing task still shows the app's own error dialog`() {
        throwOnShowError = false
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).also(::saveControllerForCleanup)
        val activity = controller.create().get()
        activity.setTheme(CommonR.style.Theme_Light)

        activity.launchCatchingTask { throw IllegalStateException("the task failed") }
        advanceRobolectricLooper()

        assertThat(getAlertDialogText(true), containsString("the task failed"))
    }
}
