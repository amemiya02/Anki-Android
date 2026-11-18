// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.R
import com.ichi2.anki.common.utils.ext.AddingDefaultsMode
import com.ichi2.anki.common.utils.ext.addingDefaultsMode
import com.ichi2.anki.common.utils.ext.pasteImagesAsPng
import com.ichi2.anki.contextmenu.AnkiCardContextMenu
import com.ichi2.anki.contextmenu.CardBrowserContextMenu
import com.ichi2.anki.launchCatchingTask
import com.ichi2.utils.LanguageUtil
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class GeneralSettingsFragment : RemainingSettingsFragment(R.id.settings_general) {
    override val preferenceResource: Int
        get() = R.xml.preferences_general
    override val analyticsScreenNameConstant: String
        get() = "prefs.general"

    override fun initSubscreen() {
        initializeLanguagePref()

        // Deck for new cards
        requirePreference<ListPreference>(R.string.deck_for_new_cards_key).apply {
            launchCatchingTask { value = withCol { config.addingDefaultsMode }.toPreferenceValue() }
            setOnPreferenceChangeListener { newValue ->
                launchCatchingTask { withCol { config.addingDefaultsMode = newValue.toAddingDefaultsMode() } }
            }
        }
        // Paste PNG
        // Represents in the collection's pref "pastePNG" , i.e.
        // whether to convert clipboard uri to png format or not.
        requirePreference<SwitchPreferenceCompat>(R.string.paste_png_key).apply {
            launchCatchingTask { isChecked = withCol { config.pasteImagesAsPng } }
            setOnPreferenceChangeListener { newValue ->
                launchCatchingTask { withCol { config.pasteImagesAsPng = newValue } }
            }
        }
        // Anki card context menu
        requirePreference<SwitchPreferenceCompat>(R.string.anki_card_external_context_menu_key).apply {
            setOnPreferenceChangeListener { newValue ->
                AnkiCardContextMenu.ensureConsistentStateWithPreferenceStatus(requireContext(), newValue)
            }
        }
        // Card browser context menu
        requirePreference<SwitchPreferenceCompat>(R.string.card_browser_external_context_menu_key).apply {
            setOnPreferenceChangeListener { newValue ->
                CardBrowserContextMenu.ensureConsistentStateWithPreferenceStatus(requireContext(), newValue)
            }
        }
    }

    private fun initializeLanguagePref() {
        requirePreference<ListPreference>(R.string.pref_language_key).apply {
            setOnPreferenceChangeListener { selectedLanguage ->
                LanguageUtil.setDefaultBackendLanguages(selectedLanguage)
                runBlocking { CollectionManager.discardBackend() }

                val localeCode =
                    if (selectedLanguage != LanguageUtil.SYSTEM_LANGUAGE_TAG) {
                        selectedLanguage
                    } else {
                        null
                    }
                val localeList = LocaleListCompat.forLanguageTags(localeCode)
                AppCompatDelegate.setApplicationLocales(localeList)
                Timber.w("Known bug: some strings may be stale after a language change until the app is restarted")
            }
        }
    }
}

private fun AddingDefaultsMode.toPreferenceValue(): String =
    when (this) {
        AddingDefaultsMode.USE_CURRENT_DECK -> "0"
        AddingDefaultsMode.DECIDE_BY_NOTE_TYPE -> "1"
    }

private fun String.toAddingDefaultsMode(): AddingDefaultsMode =
    if (this == "0") AddingDefaultsMode.USE_CURRENT_DECK else AddingDefaultsMode.DECIDE_BY_NOTE_TYPE
