// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.edit
import com.ichi2.anki.CollectionManager
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.SyncPreferences
import com.ichi2.anki.account.AccountActivity
import com.ichi2.anki.isLoggedIn
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.syncServerAddress
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.preferences.Preference
import com.ichi2.preferences.VersatileTextPreference
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Sync setup: the address of the owner's own sync server, then sign-in to it.
 *
 * Sign-in is only possible once an address is set: the Fork never signs in to or syncs with
 * AnkiWeb (ADR-0001). The screen also explains how to bring decks in from AnkiWeb.
 */
class CustomSyncServerSettingsFragment : RemainingSettingsFragment(R.id.settings_sync_server) {
    override val preferenceResource = R.xml.preferences_custom_sync_server
    override val analyticsScreenNameConstant = "prefs.custom_sync_server"

    override fun initSubscreen() {
        listOf(
            R.string.custom_sync_server_collection_url_key,
        ).forEach {
            requirePreference<VersatileTextPreference>(it).continuousValidator =
                VersatileTextPreference.Validator { value ->
                    if (value.isNotEmpty()) value.toHttpUrl()
                }
        }

        requirePreference<VersatileTextPreference>(
            R.string.custom_sync_certificate_key,
        ).setOnPreferenceChangeListener { _, newValue: Any? ->
            val newCert = newValue as String

            // Empty string input causes the certificate to be unset in the backend, i.e., no error
            if (!CollectionManager.updateCustomCertificate(newCert)) {
                showSettingsDialog { dismiss ->
                    ErrorDialog(
                        title = getString(CommonString.dialog_invalid_custom_certificate_title),
                        message = getString(CommonString.dialog_invalid_custom_certificate),
                        onDismissRequest = dismiss,
                    )
                }
                return@setOnPreferenceChangeListener false
            }

            showSnackbar(CommonString.dialog_updated_custom_certificate)
            true
        }

        requirePreference<Preference>(R.string.sync_setup_account_key).setOnPreferenceClickListener {
            // AccountActivity opens this screen again while no server is set up
            if (syncServerAddress() != null) {
                startActivity(AccountActivity.getIntent(requireContext()))
            }
            true
        }
        updateAccount()
    }

    override fun onResume() {
        super.onResume()
        // the user may have signed in or out
        updateAccount()
    }

    /** Sign-in is offered only once a server address is set */
    private fun updateAccount() {
        val server = syncServerAddress()
        requirePreference<Preference>(R.string.sync_setup_account_key).apply {
            isEnabled = server != null
            summary =
                when {
                    server == null -> getString(CommonString.sync_server_required)
                    isLoggedIn() -> Prefs.username
                    else -> getString(CommonString.sync_server_sign_in_to, server)
                }
        }
    }

    // See discussion at https://github.com/ankidroid/Anki-Android/pull/12367#discussion_r967681337
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.sharedPrefs
            .registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        Prefs.sharedPrefs
            .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (
                key == SyncPreferences.CUSTOM_SYNC_URI ||
                key == SyncPreferences.CUSTOM_SYNC_ENABLED
            ) {
                prefs.edit {
                    remove(SyncPreferences.PREF_CURRENT_SYNC_URI)
                }
                updateAccount()
            }
        }
}
