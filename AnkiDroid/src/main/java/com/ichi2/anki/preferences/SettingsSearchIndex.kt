// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.content.Context
import android.content.res.Configuration
import android.content.res.XmlResourceParser
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import androidx.fragment.app.Fragment
import com.ichi2.anki.BuildConfig
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.android.AdaptionUtil
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.preferences.reviewer.ReviewerMenuSettingsFragment
import com.ichi2.anki.preferences.reviewer.ViewerAction
import com.ichi2.anki.ui.internationalization.sentenceCase
import org.xmlpull.v1.XmlPullParser
import java.text.Normalizer
import java.util.Locale
import kotlin.reflect.KClass

internal data class SettingsSearchEntry(
    val title: String,
    val path: String,
    val destination: KClass<out Fragment>,
    val key: String? = null,
    val details: List<String> = emptyList(),
) {
    val id get() = "${destination.qualifiedName}/${key ?: title}"

    fun matchedDetails(query: String): String? =
        if (title.matches(query.searchTerms())) null else details.firstOrNull { it.matches(query.searchTerms()) }
}

internal fun List<SettingsSearchEntry>.matchingSettings(query: String): List<SettingsSearchEntry> {
    val terms = query.searchTerms()
    if (terms.isEmpty()) return emptyList()
    return filter { entry -> (listOf(entry.title) + entry.details).joinToString(" ").matches(terms) }
        .sortedByDescending { it.title.matches(terms) }
}

private val marks = Regex("\\p{M}+")
private val whitespace = Regex("\\s+")

internal fun String.searchTerms(): List<String> = trim().split(whitespace).filter(String::isNotEmpty)

private fun String.matches(terms: List<String>): Boolean {
    val text = searchable()
    return terms.all { it.searchable() in text }
}

private fun String.searchable(): String = Normalizer.normalize(lowercase(Locale.ROOT), Normalizer.Form.NFKD).replace(marks, "")

/** Indexes the localized metadata of settings that still use Preference XML, without opening their screens. */
internal fun buildSettingsSearchIndex(
    fragment: Fragment,
    groups: List<SettingsGroup>,
): List<SettingsSearchEntry> =
    with(fragment.requireContext()) {
        val hidden = hiddenSettings()
        groups
            .flatMap { group ->
                val title = getString(group.title)
                val groupEntry = SettingsSearchEntry(title, getString(CommonString.settings), group.destination)
                val entries =
                    when (group) {
                        SettingsGroup.GENERAL -> readSettings(R.xml.preferences_general, title, group.destination)
                        SettingsGroup.APPEARANCE -> appearanceSearchEntries(title)
                        SettingsGroup.STUDYING -> studyingSearchEntries(title)
                        SettingsGroup.CONTROLS -> controlsSearchEntries(title, fragment::controlPreferenceTitle)
                        SettingsGroup.ACCESSIBILITY -> readSettings(R.xml.preferences_accessibility, title, group.destination)
                        SettingsGroup.SYNC -> syncSearchEntries(title)
                        SettingsGroup.NOTIFICATIONS -> readSettings(R.xml.preferences_notifications, title, group.destination)
                        SettingsGroup.REVIEW_REMINDERS -> emptyList()
                        SettingsGroup.BACKUPS -> readSettings(R.xml.preferences_backup_limits, title, group.destination)
                        SettingsGroup.ADVANCED -> readSettings(R.xml.preferences_advanced, title, group.destination)
                        SettingsGroup.ABOUT -> aboutSearchEntries(title)
                        SettingsGroup.DEVELOPER -> readSettings(R.xml.preferences_developer_options, title, group.destination)
                        SettingsGroup.SWITCH_PROFILE -> emptyList()
                    }
                listOf(groupEntry) + entries
            }.filterNot { it.key in hidden }
            .distinctBy { it.id }
    }

private fun Context.appearanceSearchEntries(path: String): List<SettingsSearchEntry> {
    fun appearance(
        @StringRes title: Int,
        @StringRes key: Int,
        vararg details: Int,
    ) = SettingsSearchEntry(
        getString(title),
        path,
        AppearanceSettingsFragment::class,
        getString(key),
        details.map(::getString),
    )

    return buildList {
        add(
            appearance(
                CommonString.app_theme,
                R.string.app_theme_key,
                R.string.settings_theme_system,
                R.string.settings_theme_light,
                R.string.settings_theme_dark,
                R.string.settings_theme_black,
            ),
        )
        add(appearance(R.string.settings_night_theme, R.string.night_theme_key, R.string.settings_theme_note))
        add(
            appearance(
                R.string.settings_choose_image,
                R.string.pref_deck_picker_background_key,
                R.string.settings_deck_list_background,
                R.string.settings_choose_image_summary,
            ),
        )
        add(
            appearance(
                R.string.settings_remove_background,
                R.string.pref_remove_wallpaper_key,
                R.string.settings_deck_list_background,
            ),
        )
    }
}

private fun Context.studyingSearchEntries(studying: String): List<SettingsSearchEntry> =
    readSettings(R.xml.preferences_reviewing, studying, ReviewingSettingsFragment::class) +
        readSettings(R.xml.preferences_reviewer, studying, ReviewerOptionsFragment::class) +
        ViewerAction.entries.filter { it.defaultDisplayType != null }.map {
            SettingsSearchEntry(
                it.title(this),
                breadcrumb(studying, getString(CommonString.toolbar_actions)),
                ReviewerMenuSettingsFragment::class,
                it.name,
            )
        }

private fun Context.controlsSearchEntries(
    controls: String,
    controlTitle: (String) -> String?,
): List<SettingsSearchEntry> {
    val prefs = sharedPrefs()
    return readSettings(R.xml.preferences_controls, controls, ControlsSettingsFragment::class) +
        ControlPreferenceScreen.entries.flatMap { screen ->
            val tab =
                getString(
                    when (screen) {
                        ControlPreferenceScreen.REVIEWER -> CommonString.pref_controls_reviews_tab
                        ControlPreferenceScreen.PREVIEWER -> CommonString.pref_controls_previews_tab
                    },
                )
            val actions = screen.searchableActions().associateBy { it.preferenceKey }
            readSettings(screen.xmlRes, breadcrumb(controls, tab), ControlsSettingsFragment::class, controlTitle).map { entry ->
                val bindings = actions[entry.key]?.getBindings(prefs).orEmpty().map { it.toDisplayString(this) }
                entry.copy(details = entry.details + bindings)
            }
        }
}

private fun Context.syncSearchEntries(sync: String): List<SettingsSearchEntry> =
    readSettings(R.xml.preferences_sync, sync, SyncSettingsFragment::class) +
        readSettings(
            R.xml.preferences_custom_sync_server,
            breadcrumb(sync, getString(CommonString.sync_server_title)),
            CustomSyncServerSettingsFragment::class,
        )

private fun Context.aboutSearchEntries(path: String): List<SettingsSearchEntry> =
    listOf(
        SettingsSearchEntry(getString(R.string.settings_version), path, AboutFragment::class, "version"),
        SettingsSearchEntry(getString(R.string.settings_build_date), path, AboutFragment::class, "build_date"),
        SettingsSearchEntry(getString(R.string.settings_engine_version), path, AboutFragment::class, "backend_version"),
        SettingsSearchEntry(getString(R.string.settings_fsrs_version), path, AboutFragment::class, "fsrs_version"),
        SettingsSearchEntry(getString(CommonString.about_fork_title), path, AboutFragment::class, "about_contributors_title"),
        SettingsSearchEntry(getString(CommonString.license), path, AboutFragment::class, "about_license_title"),
        SettingsSearchEntry(getString(R.string.settings_source_code), path, AboutFragment::class, "license_description"),
        SettingsSearchEntry(getString(CommonString.open_changelog), path, AboutFragment::class, "open_changelog"),
        SettingsSearchEntry(TR.sentenceCase.copyDebugInfo, path, AboutFragment::class, "copy_debug_info"),
    )

private fun Context.hiddenSettings(): Set<String> =
    buildSet {
        if (resources.getBoolean(R.bool.bottom_navigation_available)) add(getString(R.string.nav_drawer_gesture_key))
        if (resources.configuration.keyboard != Configuration.KEYBOARD_QWERTY) add(getString(R.string.pref_keyboard_shortcuts_key))
        if (BuildConfig.DEBUG) add(getString(R.string.developer_options_enabled_by_user_key))
        if (AdaptionUtil.isXiaomiRestrictedLearningDevice) {
            add(getString(R.string.pref_notifications_vibrate_key))
            add(getString(R.string.pref_notifications_blink_key))
        }
    }

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

private fun Context.readSettings(
    @XmlRes xml: Int,
    groupPath: String,
    destination: KClass<out Fragment>,
    overrideTitle: (String) -> String? = { null },
): List<SettingsSearchEntry> =
    buildList {
        resources.getXml(xml).use { parser ->
            val sections = mutableListOf<Pair<Int, String>>()
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG || parser.depth == 1) continue
                sections.removeAll { (depth, _) -> depth >= parser.depth }
                val key = parser.androidText(this@readSettings, "key")
                val kind = parser.name.substringAfterLast('.')
                val title = key?.let(overrideTitle) ?: settingTitle(key) ?: parser.androidText(this@readSettings, "title")
                if (kind.endsWith("Category")) {
                    title?.let { sections.add(parser.depth to it) }
                    continue
                }
                val submenu =
                    when (key) {
                        getString(R.string.time_limit_preference) -> TimeboxSettingsFragment::class
                        getString(R.string.custom_sync_server_key) -> CustomSyncServerSettingsFragment::class
                        getString(R.string.reviewer_menu_settings_key) -> ReviewerMenuSettingsFragment::class
                        else -> null
                    }
                if (key == null || title == null || key == getString(R.string.pref_backups_help_key)) continue

                val sectionPath =
                    when {
                        key == getString(R.string.keep_screen_on_preference) -> listOf(getString(R.string.settings_study_screen))
                        destination == ReviewerOptionsFragment::class ->
                            sections.map {
                                if (it.second ==
                                    getString(CommonString.screen)
                                ) {
                                    getString(R.string.settings_study_screen)
                                } else {
                                    it.second
                                }
                            }
                        else -> sections.map { it.second }
                    }
                val path = breadcrumb(groupPath, *sectionPath.toTypedArray())
                val summary = settingSummary(key) ?: parser.androidText(this@readSettings, "summary")
                val choices = parser.getAttributeResourceValue(ANDROID_NS, "entries", 0)
                val details =
                    listOfNotNull(summary) + if (choices == 0) emptyList() else resources.getStringArray(choices).toList()

                add(SettingsSearchEntry(title, path, submenu ?: destination, key.takeIf { submenu == null }, details))
            }
        }
    }

private fun Context.settingTitle(key: String?): String? =
    when (key) {
        getString(R.string.paste_png_key) -> TR.preferencesPasteClipboardImagesAsPng()
        getString(R.string.anki_card_external_context_menu_key) ->
            getString(CommonString.card_browser_enable_external_context_menu, getString(CommonString.context_menu_anki_card_label))
        getString(R.string.card_browser_external_context_menu_key) ->
            getString(CommonString.card_browser_enable_external_context_menu, getString(CommonString.card_browser_context_menu))
        getString(R.string.pref_review_category_key) -> getString(R.string.settings_study_screen)
        getString(R.string.show_progress_preference) -> TR.preferencesShowRemainingCardCount()
        getString(R.string.show_audio_play_buttons_key) -> TR.preferencesShowPlayButtonsOnCardsWith()
        getString(R.string.show_estimates_preference) -> TR.preferencesShowNextReviewTimeAboveAnswer()
        getString(R.string.sync_io_timeout_secs_key) -> TR.preferencesNetworkTimeout()
        else -> null
    }

private fun Context.settingSummary(key: String): String? =
    when (key) {
        getString(R.string.one_way_sync_key) -> TR.preferencesOnNextSyncForceChangesIn()
        getString(R.string.anki_card_external_context_menu_key) ->
            getString(CommonString.card_browser_enable_external_context_menu_summary, getString(CommonString.context_menu_anki_card_label))
        getString(R.string.card_browser_external_context_menu_key) ->
            getString(CommonString.card_browser_enable_external_context_menu_summary, getString(CommonString.card_browser_context_menu))
        else -> null
    }

private fun XmlResourceParser.androidText(
    context: Context,
    attribute: String,
): String? {
    val resource = getAttributeResourceValue(ANDROID_NS, attribute, 0)
    return if (resource != 0) context.getText(resource).toString() else getAttributeValue(ANDROID_NS, attribute)
}

private fun breadcrumb(vararg parts: String): String = parts.joinToString(" › ")
