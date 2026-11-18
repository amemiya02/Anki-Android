// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.account.AccountActivity
import com.ichi2.anki.common.crashreporting.runCatchingWithReport
import com.ichi2.anki.isLoggedIn
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.syncServerAddress
import com.ichi2.preferences.NumberRangePreferenceCompat

/**
 * Fragment with preferences related to syncing
 */
class SyncSettingsFragment : RemainingSettingsFragment(R.id.settings_sync) {
    override val preferenceResource: Int
        get() = R.xml.preferences_sync
    override val analyticsScreenNameConstant: String
        get() = "prefs.sync"

    override fun initSubscreen() {
        // Sync account: opens sync setup while no sync server is set up
        updateSyncAccountSummary()

        // Enable/disable one-way sync if the user is logged in
        updateOneWaySyncEnabledState()

        // Configure 'Network timeout'
        // TODO: add 'reset to default' functionality
        requirePreference<NumberRangePreferenceCompat>(R.string.sync_io_timeout_secs_key).apply {
            summaryProvider =
                Preference.SummaryProvider<EditTextPreference> {
                    runCatchingWithReport("network_timeout") {
                        TR.qtMiscSecond(this.getValue())
                    }.getOrNull() ?: getString(CommonString.pref__etc__summary__error)
                }
        }

        // Configure one-way sync option
        requirePreference<Preference>(R.string.one_way_sync_key).apply {
            setOnPreferenceClickListener {
                confirmSettingChange(
                    title = getString(CommonString.one_way_sync_title),
                    message = TR.preferencesOnNextSyncForceChangesIn(),
                ) {
                    launchCatchingTask {
                        withCol { modSchema(check = false) }
                        showSnackbar(CommonString.one_way_sync_confirmation, Snackbar.LENGTH_SHORT)
                    }
                }
                false
            }
        }

        requirePreference<Preference>(R.string.sync_account_key).apply {
            setOnPreferenceClickListener {
                val accountActivityIntent = AccountActivity.getIntent(requireContext())
                startActivity(accountActivityIntent)
                true
            }
        }

        // Custom sync server
        requirePreference<Preference>(R.string.custom_sync_server_key).setSummaryProvider {
            syncServerAddress() ?: getString(CommonString.sync_server_not_set)
        }
    }

    private fun updateSyncAccountSummary() {
        requirePreference<Preference>(R.string.sync_account_key)
            .summary =
            when {
                syncServerAddress() == null -> getString(CommonString.sync_server_required)
                isLoggedIn() -> Prefs.username
                else -> getString(CommonString.sync_account_summ_logged_out)
            }
    }

    private fun updateOneWaySyncEnabledState() {
        requirePreference<Preference>(R.string.one_way_sync_key).isEnabled = isLoggedIn()
    }

    // TODO trigger the summary change from MyAccount.kt once it is migrated to a fragment
    override fun onResume() {
        // Trigger a summary update in case the user logged in/out on MyAccount activity
        updateSyncAccountSummary()
        updateOneWaySyncEnabledState()
        super.onResume()
    }
}
