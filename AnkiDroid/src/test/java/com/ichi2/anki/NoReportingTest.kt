// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.preferences.PreferenceTestUtils
import com.ichi2.testutils.BackupManagerTestUtilities
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.Rule
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
    @get:Rule
    val compose = createEmptyComposeRule()

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
    fun `the app carries no usage statistics client`() {
        assertFailsWith<ClassNotFoundException>("the Google Analytics client would send usage statistics") {
            Class.forName("com.criticalay.GoogleAnalytics")
        }
    }

    @Test
    fun `settings have no entries for crash reports or usage statistics`() {
        val keys = PreferenceTestUtils.getAllPreferenceKeys(targetContext)

        assertThat("error reporting mode", keys, not(hasItem("reportErrorMode")))
        assertThat("share feature usage", keys, not(hasItem("analytics_opt_in_v2")))
        assertThat("analytics dev mode", keys, not(hasItem("analytics_debug_preference")))
    }

    @Test
    fun `an upgrade does not ask the owner to share usage statistics`() {
        setIntroductionSlidesShown(true)
        BackupManagerTestUtilities.setupSpaceForBackup(targetContext)
        try {
            // An upgrade is when the opt-in prompt used to appear
            getPreferences().edit {
                putString("lastVersion", "0.1")
                putBoolean("backupPromptDisabled", true)
            }

            val deckPicker = startActivityNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent())
            advanceRobolectricLooper()

            assertThat(deckPicker.supportFragmentManager.fragments.filterIsInstance<DialogFragment>(), empty())
        } finally {
            BackupManagerTestUtilities.reset()
        }
    }

    @Test
    fun `a failing task still shows the app's own error dialog`() {
        throwOnShowError = false
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).also(::saveControllerForCleanup)
        controller.get().setTheme(CommonR.style.Theme_Light)
        val activity = controller.setup().get()

        activity.launchCatchingTask { throw IllegalStateException("the task failed") }
        advanceRobolectricLooper()

        compose.onNodeWithText("the task failed", substring = true).assertIsDisplayed()
    }
}
