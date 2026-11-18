// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.content.Context
import androidx.annotation.XmlRes
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.Fragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import com.ichi2.anki.BuildConfig
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonPreferenceKey
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.android.AdaptionUtil
import com.ichi2.utils.LanguageUtil
import com.ichi2.utils.LanguageUtil.getStringByLocale
import kotlin.reflect.KClass

/** Localized and device-specific row metadata shared by the screens and the search index. */
internal fun PreferenceScreen.applySettingsMetadata(
    @XmlRes resource: Int,
) {
    fun preference(id: Int): Preference = checkNotNull(findPreference(context.getString(id)))

    fun contextMenu(
        key: Int,
        label: Int,
    ) {
        val name = context.getString(label)
        preference(key).apply {
            title = context.getString(CommonString.card_browser_enable_external_context_menu, name)
            summary = context.getString(CommonString.card_browser_enable_external_context_menu_summary, name)
        }
    }

    when (resource) {
        R.xml.preferences_general -> {
            val language = preference(R.string.pref_language_key) as ListPreference
            val languages = LanguageUtil.APP_LANGUAGES.toSortedMap(String.CASE_INSENSITIVE_ORDER)
            val systemLanguage = context.getStringByLocale(CommonString.language_system, LanguageUtil.getSystemLocale())
            language.entries = arrayOf(systemLanguage, *languages.keys.toTypedArray())
            language.entryValues = arrayOf(LanguageUtil.SYSTEM_LANGUAGE_TAG, *languages.values.toTypedArray())
            preference(R.string.nav_drawer_gesture_key).isVisible = !context.resources.getBoolean(R.bool.bottom_navigation_available)
            preference(R.string.paste_png_key).title = TR.preferencesPasteClipboardImagesAsPng()
            contextMenu(R.string.anki_card_external_context_menu_key, CommonString.context_menu_anki_card_label)
            contextMenu(R.string.card_browser_external_context_menu_key, CommonString.card_browser_context_menu)
        }
        R.xml.preferences_sync -> {
            preference(R.string.sync_io_timeout_secs_key).title = TR.preferencesNetworkTimeout()
            preference(R.string.one_way_sync_key).summary = TR.preferencesOnNextSyncForceChangesIn()
        }
        R.xml.preferences_notifications -> {
            val minimum = preference(CommonPreferenceKey.pref_notifications_minimum_cards_due_key) as ListPreference
            val locale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
            minimum.entries =
                minimum.entries
                    .mapIndexed { index, entry ->
                        String.format(locale, entry.toString(), minimum.entryValues[index].toString().toInt())
                    }.toTypedArray()
            preference(R.string.pref_notifications_vibrate_key).isVisible = !AdaptionUtil.isXiaomiRestrictedLearningDevice
            preference(R.string.pref_notifications_blink_key).isVisible = !AdaptionUtil.isXiaomiRestrictedLearningDevice
        }
        R.xml.preferences_developer_options -> {
            preference(R.string.developer_options_enabled_by_user_key).isVisible = !BuildConfig.DEBUG
            preference(R.string.html_javascript_debugging_key).isVisible = !BuildConfig.DEBUG
        }
    }
}

/** Builds the same preference models without committing XML defaults or reading personal values. */
internal fun Context.remainingSettingsSearchEntries(
    @XmlRes resource: Int,
    path: String,
    destination: KClass<out Fragment>,
): List<SettingsSearchEntry> {
    val manager = PreferenceManager(this).apply { preferenceDataStore = SearchMetadataOnly }
    val screen = manager.inflateFromResource(this, resource, null)
    screen.applySettingsMetadata(resource)
    return buildList {
        fun append(
            group: PreferenceGroup,
            breadcrumb: String,
        ) {
            for (index in 0 until group.preferenceCount) {
                val row = group.getPreference(index)
                if (!row.isVisible) continue
                val title = row.title?.toString() ?: continue
                if (row is PreferenceCategory) {
                    append(row, "$breadcrumb › $title")
                } else if (row.isSelectable && row.key != null) {
                    val opensServer = row.key == getString(R.string.custom_sync_server_key)
                    val choices = (row as? ListPreference)?.entries.orEmpty().map(CharSequence::toString)
                    add(
                        SettingsSearchEntry(
                            title = title,
                            path = breadcrumb,
                            destination = if (opensServer) CustomSyncServerSettingsFragment::class else destination,
                            key = if (opensServer) null else row.key,
                            details = listOfNotNull(row.summary?.toString()?.takeIf { row !is ListPreference }) + choices,
                        ),
                    )
                }
            }
        }
        append(screen, path)
    }
}

private object SearchMetadataOnly : PreferenceDataStore() {
    override fun putBoolean(
        key: String,
        value: Boolean,
    ) = Unit

    override fun putInt(
        key: String,
        value: Int,
    ) = Unit

    override fun putString(
        key: String,
        value: String?,
    ) = Unit
}
