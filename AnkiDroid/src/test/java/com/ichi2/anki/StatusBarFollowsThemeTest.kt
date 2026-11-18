// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.os.Build
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.test.filters.SdkSuppress
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.instantnoteeditor.InstantNoteEditorActivity
import com.ichi2.anki.settings.PrefsRepository
import com.ichi2.anki.settings.enums.AppTheme
import com.ichi2.anki.settings.enums.NightTheme
import com.ichi2.testutils.ActivityList
import com.ichi2.testutils.ActivityList.ActivityLaunchParam
import com.ichi2.testutils.grantWritePermissions
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector

/**
 * Every screen's app bar is the page colour, so the status bar icons drawn over it must follow
 * the theme: dark on Light's paper, light on Dark and Black.
 *
 * Upstream's app bars were coloured, and several screens asked for light icons in every theme,
 * which left them white on paper in Light.
 *
 * @see ActivityList.allActivitiesAndIntents
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
@RunWith(RobolectricTestParameterInjector::class)
class StatusBarFollowsThemeTest : RobolectricTest() {
    @TestParameter(valuesProvider = ActivityLauncherProvider::class)
    lateinit var config: ActivityConfig

    @TestParameter
    lateinit var theme: ThemeConfig

    private val launcher: ActivityLaunchParam get() = config.launcher

    // rendering a card requires a media folder
    @Before
    fun startAsAnExistingUser() {
        grantWritePermissions()
        targetContext.sharedPrefs().edit { putBoolean(IntroductionActivity.PREF_INTRODUCTION_SLIDES_SHOWN, true) }
        PrefsRepository(targetContext).apply {
            appTheme = if (theme.isNight) AppTheme.NIGHT else AppTheme.DAY
            (theme.theme as? NightTheme)?.let { nightTheme = it }
        }
    }

    @Test
    fun `status bar icons follow the theme`() {
        val activity = startActivityNormallyOpenCollectionWithIntent(launcher.activity, launcher.buildIntent(this))
        advanceRobolectricLooper()
        assumeFalse("${launcher.simpleName} finished during startup", activity.isFinishing)

        val window = activity.window
        assertThat(
            "dark status bar icons on Light's paper, light icons on Dark and Black",
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars,
            equalTo(!theme.isNight),
        )
    }

    /** Wraps the launcher so JUnit formats the test name correctly */
    class ActivityConfig(
        val launcher: ActivityLaunchParam,
    ) {
        override fun toString(): String = launcher.simpleName
    }

    class ActivityLauncherProvider : TestParameterValuesProvider() {
        override fun provideValues(context: Context?): List<ActivityConfig> =
            ActivityList
                .allActivitiesAndIntents()
                .filterNot { it.activity in notScreens }
                .map { ActivityConfig(it) }
    }

    companion object {
        /** Activities which redirect, or draw over another app, rather than showing a screen */
        private val notScreens =
            setOf(
                IntentHandler::class.java,
                IntentHandler2::class.java,
                InstantNoteEditorActivity::class.java,
            )
    }
}
