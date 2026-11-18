// SPDX-License-Identifier: GPL-3.0-or-later
package com.ichi2.anki.preferences

import android.os.Build
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.testing.junit.testparameterinjector.TestParameter
import com.ichi2.anki.R
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.common.storage.CollectionHelper
import com.ichi2.anki.common.utils.dp
import com.ichi2.anki.preferences.reviewer.ReviewerMenuSettingsFragment
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.utils.CollectionPreferences
import com.ichi2.testutils.dispatchInsets
import com.ichi2.testutils.ext.clear
import com.ichi2.testutils.launchForFullHeightScreenshot
import com.ichi2.testutils.simulateSystemBars
import com.ichi2.testutils.withWritePermissions
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PreferencesScreenshotTest : ScreenshotTest() {
    @After
    override fun tearDown() {
        super.tearDown()
        Prefs.clear()
    }

    @Test
    fun settingsPanelSystemBarsChange() =
        withWritePermissions {
            withPreferencesActivity(GeneralSettingsFragment::class) { activity ->
                val name = "GeneralSettingsFragment_sdk${Build.VERSION.SDK_INT}"
                val appbar = activity.settingsFragment!!.requireView().findViewById<AppBarLayout>(R.id.appbar)
                appbar.findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbarLayout).scrimAnimationDuration = 0
                activity.dispatchInsets(navBarBottom = 24.dp)
                appbar.setExpanded(false, false)
                advanceRobolectricLooper()
                captureScreen("${name}_collapsed")

                activity.dispatchInsets(barsVisible = false, navBarStableBottom = 24.dp)
                advanceRobolectricLooper()
                captureScreen("${name}_fullscreen_collapsed")

                appbar.setExpanded(true, false)
                advanceRobolectricLooper()
                captureScreen("${name}_fullscreen_expanded")

                activity.dispatchInsets(barsVisible = false, cutoutTop = 48.dp, navBarStableBottom = 24.dp)
                advanceRobolectricLooper()
                captureScreen("${name}_fullscreen_cutout")

                appbar.setExpanded(false, false)
                advanceRobolectricLooper()
                captureScreen("${name}_fullscreen_cutout_collapsed")

                appbar.setExpanded(true, false)
                activity.dispatchInsets(navBarBottom = 24.dp)
                advanceRobolectricLooper()
                captureScreen("${name}_system_bars_restored")
            }
        }

    @Test
    fun timeboxSettingsFragmentDisabled() =
        runTest {
            CollectionPreferences.setTimeboxTimeLimit(0.toDuration(DurationUnit.MINUTES))
            withPreferencesActivity(TimeboxSettingsFragment::class) { activity ->
                activity.simulateSystemBars()
                captureScreen("TimeboxSettingsFragment_disabled")
            }
        }

    @Test
    fun timeboxSettingsFragmentEnabled() =
        runTest {
            CollectionPreferences.setTimeboxTimeLimit(20.toDuration(DurationUnit.MINUTES))
            withPreferencesActivity(TimeboxSettingsFragment::class) { activity ->
                activity.simulateSystemBars()
                captureScreen("TimeboxSettingsFragment_enabled")
            }
        }

    /** One test per fragment: capturing all of them in a single test risks a timeout */
    @Test
    fun capturePreferenceFragment(
        @TestParameter(valuesProvider = PreferenceFragmentsProvider::class) fragmentClass: KClass<out Fragment>,
    ) {
        if (fragmentClass in setOf(HeaderFragment::class, AppearanceSettingsFragment::class, StudyingSettingsIndexFragment::class)) return
        withPreferencesActivity(
            fragmentClass,
            fullHeightList = fragmentClass.fullHeightList(),
            setup = { activity -> activity.stabilizeContent() },
        ) {
            captureScreen(fragmentClass.simpleName!!)
        }
    }

    /** Replaces content which changes between runs, so it does not appear in diffs */
    private fun PreferencesActivity.stabilizeContent() {
        val settingsFragment = settingsFragment
        // Robolectric generates a different temporary path every time,
        // so avoid creating an unnecessary diff in the collection path pref summary
        (settingsFragment as? AdvancedSettingsFragment)?.apply {
            requirePreference<Preference>(CollectionHelper.PREF_COLLECTION_PATH).summaryProvider = {
                "/storage/emulated/0/AnkiDroid"
            }
        }
        (settingsFragment as? AboutFragment)?.apply {
            binding.buildDate.text = "May 18, 2026"
        }
    }

    /**
     * @param fullHeightList if set, the screenshot is full height: the screen is sized to show the whole list
     * @param setup run after each launch of the screen, before [block]
     */
    private fun withPreferencesActivity(
        fragmentClass: KClass<out Fragment>,
        fullHeightList: ((PreferencesActivity) -> RecyclerView)? = null,
        setup: (PreferencesActivity) -> Unit = {},
        block: (PreferencesActivity) -> Unit,
    ) {
        val intent = PreferencesActivity.getIntent(targetContext, fragmentClass)
        if (fullHeightList != null) {
            launchForFullHeightScreenshot(intent, fullHeightList, setup, block)
            return
        }
        ActivityScenario.launch<PreferencesActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                setup(activity)
                block(activity)
            }
        }
    }

    /** The fragment displayed in the settings pane */
    private val PreferencesActivity.settingsFragment: Fragment?
        get() = (fragment as PreferencesFragment).childFragmentManager.findFragmentById(R.id.settings_container)

    /** @return the list which makes the screenshot full height, or null to capture at the device height */
    private fun KClass<out Fragment>.fullHeightList(): ((PreferencesActivity) -> RecyclerView)? =
        when {
            isSubclassOf(PreferenceFragmentCompat::class) -> { activity ->
                (activity.settingsFragment as PreferenceFragmentCompat).listView
            }
            isSubclassOf(ReviewerMenuSettingsFragment::class) -> { activity ->
                activity.settingsFragment!!.requireView().findViewById(R.id.recycler_view)
            }
            else -> null
        }
}
