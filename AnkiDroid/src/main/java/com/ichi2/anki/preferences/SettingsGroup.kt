// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.android.AdaptionUtil
import com.ichi2.anki.preferences.profiles.SwitchProfilesFragment
import com.ichi2.anki.preferences.reviewer.ReviewerMenuSettingsFragment
import com.ichi2.anki.reviewreminders.ScheduleRemindersFragment
import com.ichi2.anki.settings.Prefs
import kotlin.reflect.KClass

internal enum class SettingsGroup(
    @StringRes val title: Int,
    @StringRes val summary: Int,
    @DrawableRes val icon: Int,
    val destination: KClass<out Fragment>,
) {
    GENERAL(CommonString.pref_cat_general, R.string.settings_general_summary, R.drawable.symbol_tune, GeneralSettingsFragment::class),
    APPEARANCE(
        R.string.settings_appearance,
        R.string.settings_appearance_summary,
        R.drawable.symbol_palette,
        AppearanceSettingsFragment::class,
    ),
    STUDYING(
        R.string.settings_studying,
        R.string.settings_studying_summary,
        R.drawable.symbol_school,
        StudyingSettingsFragment::class,
    ),
    CONTROLS(
        CommonString.pref_cat_controls,
        R.string.settings_controls_summary,
        R.drawable.symbol_touch_app,
        ControlsSettingsFragment::class,
    ),
    ACCESSIBILITY(
        CommonString.accessibility,
        R.string.settings_accessibility_summary,
        R.drawable.symbol_accessibility_new,
        AccessibilitySettingsFragment::class,
    ),
    SYNC(CommonString.pref_cat_sync, R.string.settings_sync_summary, R.drawable.symbol_sync, SyncSettingsFragment::class),
    REVIEW_REMINDERS(
        CommonString.review_reminders_title,
        R.string.review_reminders_settings_summary,
        R.drawable.symbol_notifications,
        ScheduleRemindersFragment::class,
    ),
    BACKUPS(
        R.string.settings_backups,
        R.string.settings_backups_summary,
        R.drawable.symbol_settings_backup_restore,
        BackupLimitsSettingsFragment::class,
    ),
    ADVANCED(
        CommonString.pref_cat_advanced,
        R.string.settings_advanced_summary,
        R.drawable.symbol_construction,
        AdvancedSettingsFragment::class,
    ),
    ABOUT(CommonString.pref_cat_about_title, R.string.settings_about_summary, R.drawable.symbol_info, AboutFragment::class),
    DEVELOPER(
        CommonString.pref_cat_dev_options,
        R.string.settings_developer_summary,
        R.drawable.symbol_code,
        DeveloperOptionsFragment::class,
    ),
    SWITCH_PROFILE(
        CommonString.switch_profile,
        R.string.settings_switch_profile_summary,
        R.drawable.symbol_switch_account,
        SwitchProfilesFragment::class,
    ),
    ;

    companion object {
        fun visibleGroups(): List<SettingsGroup> =
            entries.filter {
                when (it) {
                    ADVANCED -> !AdaptionUtil.isXiaomiRestrictedLearningDevice
                    DEVELOPER -> Prefs.isDeveloperOptionsEnabled
                    SWITCH_PROFILE -> Prefs.switchProfileEnabled
                    else -> true
                }
            }

        fun forFragment(fragment: Fragment): SettingsGroup? =
            when (fragment) {
                is TimeboxSettingsFragment, is ReviewerMenuSettingsFragment -> STUDYING
                is CustomSyncServerSettingsFragment -> SYNC
                else -> entries.firstOrNull { it.destination.isInstance(fragment) }
            }
    }
}
