// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.R
import com.ichi2.anki.common.storage.CollectionHelper
import com.ichi2.anki.exception.StorageAccessException
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.provider.CardContentProvider
import com.ichi2.anki.startup.getDefaultAnkiDroidDirectory
import com.ichi2.anki.utils.openUrl
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.preferences.ExternalDirectorySelectionPreference
import com.ichi2.utils.Permissions
import com.ichi2.utils.Permissions.openAppSettingsScreen
import timber.log.Timber
import java.io.File

class AdvancedSettingsFragment : RemainingSettingsFragment(R.id.settings_advanced) {
    override val preferenceResource: Int
        get() = R.xml.preferences_advanced
    override val analyticsScreenNameConstant: String
        get() = "prefs.advanced"

    private val microphonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                requirePreference<SwitchPreferenceCompat>(R.string.pref_allow_template_audio_recording).isChecked = true
                return@registerForActivityResult
            }

            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.RECORD_AUDIO)) {
                return@registerForActivityResult
            }

            confirmSettingChange(
                title = getString(CommonString.permission_denied),
                message = getString(CommonString.microphone_permission_denied_message),
            ) { openAppSettingsScreen() }
        }

    override fun initSubscreen() {
        // Check that input is valid before committing change in the collection path
        requirePreference<ExternalDirectorySelectionPreference>(CollectionHelper.PREF_COLLECTION_PATH).apply {
            setOnPreferenceChangeListener { _, newValue: Any? ->
                val newPath = newValue as String
                try {
                    CollectionHelper.initializeAnkiDroidDirectory(File(newPath))
                    launchCatchingTask {
                        CollectionManager.discardBackend()
                        val deckPicker = Intent(requireContext(), DeckPicker::class.java)
                        deckPicker.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(deckPicker)
                    }
                    true
                } catch (e: StorageAccessException) {
                    // TODO: Request MANAGE_EXTERNAL_STORAGE
                    Timber.e(e, "Could not initialize directory: %s", newPath)
                    showSettingsDialog { dismiss ->
                        ErrorDialog(
                            title = getString(CommonString.dialog_collection_path_not_dir),
                            message = "",
                            onDismissRequest = dismiss,
                            dismissText = getString(CommonString.dialog_ok),
                            primaryAction =
                                DialogAction(getString(CommonString.reset_custom_buttons), {
                                    value = getDefaultAnkiDroidDirectory(requireContext()).absolutePath
                                    dismiss()
                                }),
                        )
                    }
                    false
                }
            }
        }

        requirePreference<SwitchPreferenceCompat>(R.string.pref_allow_template_audio_recording).apply {
            isChecked = isChecked && Permissions.canRecordAudio(requireContext())
            setOnPreferenceChangeListener { _, newValue ->
                if (newValue !is Boolean) return@setOnPreferenceChangeListener false
                if (!newValue || Permissions.canRecordAudio(requireContext())) {
                    return@setOnPreferenceChangeListener true
                }
                // veto the opt-in until the permission is granted
                microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                false
            }
        }

        /*
         * Plugins section
         */

        // Third party apps
        requirePreference<Preference>(R.string.thirdparty_apps_key).setOnPreferenceClickListener {
            requireContext().openUrl(R.string.link_third_party_api_apps)
            false
        }

        // Enable API
        requirePreference<SwitchPreferenceCompat>(R.string.enable_api_key).setOnPreferenceChangeListener { newValue ->
            val providerName = ComponentName(requireContext(), CardContentProvider::class.java.name)
            val state =
                if (newValue) {
                    Timber.i("AnkiDroid ContentProvider enabled by user")
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    Timber.i("AnkiDroid ContentProvider disabled by user")
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
            requireActivity().packageManager.setComponentEnabledSetting(providerName, state, PackageManager.DONT_KILL_APP)
        }
    }
}
