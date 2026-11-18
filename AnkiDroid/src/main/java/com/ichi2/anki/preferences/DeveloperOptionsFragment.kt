// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.BuildConfig
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.storage.CollectionHelper
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.dialogs.TtsVoicesDialogFragment
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.utils.ext.defaultConfig
import com.ichi2.anki.utils.ext.showDialogFragment
import com.ichi2.anki.withProgress
import com.ichi2.preferences.IncrementerNumberRangePreferenceCompat
import com.ichi2.utils.setWebContentsDebuggingEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import timber.log.Timber
import java.io.File

/**
 * Fragment exclusive to DEBUG builds which can be used
 * to add options useful for developers or WIP features.
 */
class DeveloperOptionsFragment : RemainingSettingsFragment(R.id.settings_developer) {
    override val preferenceResource: Int
        get() = R.xml.preferences_developer_options
    override val analyticsScreenNameConstant: String
        get() = "prefs.dev_options"

    override fun initSubscreen() {
        setupEnableDeveloperOptions()
        // Make it possible to test crash reporting
        requirePreference<Preference>(R.string.pref_trigger_crash_key).setOnPreferenceClickListener {
            Timber.w("Crash triggered on purpose from advanced preferences in debug mode")
            throw RuntimeException("This is a test crash")
        }
        // Lock database
        requirePreference<Preference>(R.string.pref_lock_database_key).setOnPreferenceClickListener {
            Timber.w("Toggling database lock")
            launchCatchingTask { withCol { Thread.sleep(1000 * 86400) } }
            false
        }
        // Apply invalid FSRS Parameters (DeckConfigId 1)
        requirePreference<Preference>(R.string.pref_corrupt_fsrs_params).setOnPreferenceClickListener {
            Timber.w("Corrupting FSRS parameters for default deck config")
            launchCatchingTask {
                withCol {
                    val defaultConfig = decks.defaultConfig
                    val invalidFsrsConfig =
                        JSONArray().apply {
                            put(0.4197)
                        }
                    defaultConfig.jsonObject.put("fsrsWeights", invalidFsrsConfig)
                    defaultConfig.jsonObject.put("fsrsParams5", invalidFsrsConfig)
                    defaultConfig.jsonObject.put("fsrsParams6", invalidFsrsConfig)
                    decks.save(defaultConfig)
                }
                showThemedToast(requireContext(), getString(R.string.settings_dev_fsrs_corrupted), false)
            }
            false
        }

        // Make it possible to test crash reporting
        requirePreference<Preference>(R.string.pref_set_database_path_debug_key).setOnPreferenceClickListener {
            confirmSettingChange(
                title = getString(R.string.settings_dev_set_database_path),
                message = getString(R.string.settings_dev_set_database_path_warning),
                confirmText = getString(R.string.settings_dev_set_database_path_action),
                destructive = true,
            ) {
                Timber.w("Setting collection path to /storage/emulated/0/AnkiDroid")
                AnkiDroidApp.sharedPrefs().edit {
                    putString(CollectionHelper.PREF_COLLECTION_PATH, "/storage/emulated/0/AnkiDroid")
                }
            }
            false
        }

        // Open TTS voice selection ({{tts-voices:}})
        requirePreference<Preference>(R.string.dev_open_tts_voices).setOnPreferenceClickListener {
            showDialogFragment(TtsVoicesDialogFragment())
            false
        }

        val numberOfNotesPreference =
            requirePreference<IncrementerNumberRangePreferenceCompat>(getString(R.string.pref_fill_default_deck_number_key))

        /*
         * Create fake media section
         */
        requirePreference<Preference>(R.string.pref_fill_default_deck_key).setOnPreferenceClickListener {
            val numberOfNotes = numberOfNotesPreference.getValue()
            confirmSettingChange(
                title = getString(R.string.settings_dev_create_notes),
                message = getString(R.string.settings_dev_create_notes_warning, numberOfNotes),
                confirmText = getString(R.string.settings_dev_create_notes),
            ) { generateNotes(numberOfNotes) }
            true
        }

        val sizePreference =
            requirePreference<IncrementerNumberRangePreferenceCompat>(getString(R.string.pref_fill_collection_size_file_key))
        val numberOfFilePreference =
            requirePreference<IncrementerNumberRangePreferenceCompat>(getString(R.string.pref_fill_collection_number_file_key))

        /*
         * Create fake media section
         */
        requirePreference<Preference>(R.string.pref_fill_collection_key).setOnPreferenceClickListener {
            val sizeOfFiles = sizePreference.getValue()
            val numberOfFiles = numberOfFilePreference.getValue()
            confirmSettingChange(
                title = getString(R.string.settings_dev_create_files),
                message = getString(R.string.settings_dev_create_files_warning, numberOfFiles),
                confirmText = getString(R.string.settings_dev_create_files),
                destructive = true,
            ) { generateFiles(sizeOfFiles, numberOfFiles) }
            true
        }

        requirePreference<Preference>(R.string.pref_enable_switch_profile_key).setOnPreferenceChangeListener { _, _ ->
            ActivityCompat.recreate(requireActivity())
            true
        }

        setupWebDebugPreference()
    }

    /**
     * In a DEBUG build, hide the preference to disable developer options
     * In a RELEASE build, configure the preference to disable dev options
     * Ensure that the preference is searchable or not
     * based on the same condition at the settings search index
     */
    private fun setupEnableDeveloperOptions() {
        val enableDeveloperOptionsPref =
            requirePreference<SwitchPreferenceCompat>(R.string.developer_options_enabled_by_user_key)

        if (!BuildConfig.DEBUG) {
            enableDeveloperOptionsPref.setOnPreferenceChangeListener { _, _ ->
                showDisableDeveloperOptionsDialog()
                false
            }
        }
    }

    private fun setupWebDebugPreference() {
        requirePreference<SwitchPreferenceCompat>(R.string.html_javascript_debugging_key).apply {
            setOnPreferenceChangeListener { isEnabled ->
                setWebContentsDebuggingEnabled(isEnabled)
            }
        }
    }

    /**
     * Generate [numberOfNotes]. Note n is in deck n, with :: between each digit.
     */
    private fun generateNotes(numberOfNotes: Int) {
        Timber.d("numberOf notes: $numberOfNotes")
        launchCatchingTask {
            withProgress(getString(R.string.settings_dev_notes_progress, numberOfNotes)) {
                for (i in 1..numberOfNotes) {
                    fun deckName(noteNumber: Int): String {
                        if (noteNumber < 10) {
                            return "$noteNumber"
                        }
                        return "${deckName(noteNumber / 10)}::${noteNumber % 10}"
                    }
                    withCol {
                        val deck = decks.addNormalDeckWithName(deckName(i))
                        addNote(
                            newNote(notetypes.current()).apply { setField(0, "$i") },
                            deck.id,
                        )
                    }
                    if (i % 1000 == 0) {
                        showThemedToast(requireContext(), getString(R.string.settings_dev_notes_progress_count, i), true)
                    }
                }
                showThemedToast(requireContext(), getString(R.string.settings_dev_notes_progress_count, numberOfNotes), false)
            }
        }
    }

    private fun generateFiles(
        size: Int,
        numberOfFiles: Int,
    ) {
        Timber.d("numberOf files: $numberOfFiles, size: $size")
        launchCatchingTask {
            withProgress(getString(R.string.settings_dev_files_progress, numberOfFiles, size)) {
                val suffix = ".$size"
                for (i in 1..numberOfFiles) {
                    val f =
                        withContext(Dispatchers.IO) {
                            File.createTempFile("00$i", suffix)
                        }
                    f.appendBytes(ByteArray(size))

                    withCol {
                        media.addFile(f)
                    }
                    if (i % 1000 == 0) {
                        showThemedToast(requireContext(), getString(R.string.settings_dev_files_progress_count, i), true)
                    }
                }
                showThemedToast(requireContext(), getString(R.string.settings_dev_files_progress_count, numberOfFiles), false)
            }
        }
    }

    /**
     * Shows dialog to confirm if developer options should be disabled
     */
    private fun showDisableDeveloperOptionsDialog() {
        confirmSettingChange(
            title = getString(CommonString.disable_dev_options),
        ) {
            Prefs.isDeveloperOptionsEnabled = false
            parentFragmentManager.popBackStack()
            ActivityCompat.recreate(requireActivity())
        }
    }
}
