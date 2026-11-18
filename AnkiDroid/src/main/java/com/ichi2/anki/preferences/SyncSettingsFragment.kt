// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.DeckPicker
import com.ichi2.anki.R
import com.ichi2.anki.account.AccountActivity
import com.ichi2.anki.account.LoggedInViewModel
import com.ichi2.anki.account.SyncCertificateEditor
import com.ichi2.anki.account.SyncPrimaryButton
import com.ichi2.anki.common.crashreporting.runCatchingWithReport
import com.ichi2.anki.isLoggedIn
import com.ichi2.anki.launchCatchingTask
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.syncServerAddress
import com.ichi2.compose.theme.foundation
import com.ichi2.preferences.NumberRangePreferenceCompat
import com.ichi2.preferences.Preference
import com.ichi2.utils.LanguageUtil.withUnicodeIsolation
import com.ichi2.utils.Permissions
import java.text.DateFormat
import java.util.Date

/** The single page for sync account status, actions and settings. */
class SyncSettingsFragment : RemainingSettingsFragment(R.id.settings_sync) {
    private val accountViewModel by viewModels<LoggedInViewModel>()
    private var account by mutableStateOf(SyncPageAccount(server = null, signedIn = false, username = ""))
    private var lastSync by mutableStateOf(CollectionSyncStatus())

    override val preferenceResource = R.xml.preferences_sync
    override val analyticsScreenNameConstant = "prefs.sync"

    override fun initSubscreen() {
        updateAccount()

        requirePreference<NumberRangePreferenceCompat>(R.string.sync_io_timeout_secs_key).apply {
            setSummaryProvider {
                runCatchingWithReport("network_timeout") {
                    TR.qtMiscSecond(getValue())
                }.getOrNull() ?: getString(CommonString.pref__etc__summary__error)
            }
        }

        requirePreference<Preference>(R.string.one_way_sync_key).setOnPreferenceClickListener {
            confirmSettingChange(
                title = getString(CommonString.one_way_sync_title),
                message = TR.preferencesOnNextSyncForceChangesIn(),
            ) {
                launchCatchingTask {
                    withCol { modSchema(check = false) }
                    updateLastSync()
                    showSnackbar(CommonString.one_way_sync_confirmation, Snackbar.LENGTH_SHORT)
                }
            }
            true
        }

        requirePreference<Preference>(R.string.custom_sync_certificate_key).apply {
            setOnPreferenceClickListener {
                showSettingsDialog { dismiss ->
                    SyncCertificateEditor(Prefs.customSyncCertificate.orEmpty(), dismiss) {
                        updateCertificateSummary()
                        dismiss()
                        showSnackbar(CommonString.dialog_updated_custom_certificate)
                    }
                }
                true
            }
        }
    }

    private fun updateCertificateSummary() {
        requirePreference<Preference>(R.string.custom_sync_certificate_key).summary =
            getString(
                if (Prefs.customSyncCertificate.isNullOrBlank()) CommonString.paper_sync_optional else CommonString.paper_sync_configured,
            )
    }

    override fun onResume() {
        super.onResume()
        val wasSignedIn = account.signedIn
        updateAccount()
        updateCertificateSummary()
        if (!account.signedIn) return
        if (!wasSignedIn) Permissions.requestNotificationPermissionsForSyncing(requireActivity())
        launchCatchingTask { updateLastSync() }
    }

    private suspend fun updateLastSync() {
        lastSync =
            withCol {
                CollectionSyncStatus(
                    time = Prefs.lastSuccessfulSyncTime,
                    pending = db.queryLongScalar("select max(mod, scm) > ls from col") == 1L,
                )
            }
    }

    private fun updateAccount() {
        account = readAccount()
        requirePreference<Preference>(R.string.one_way_sync_key).isEnabled = account.signedIn
    }

    @Composable
    override fun SettingsHeader() {
        val foundation = MaterialTheme.foundation
        val server = account.server
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                Box(
                    Modifier.size(44.dp).background(foundation.colors.progressTrack, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(if (account.signedIn) R.drawable.symbol_cloud_done else R.drawable.symbol_cloud_off),
                        contentDescription = null,
                        tint = foundation.colors.emphasis,
                    )
                }
                Column(Modifier.weight(1f)) {
                    val status = if (account.signedIn) CommonString.paper_sync_signed_in_as else CommonString.sync_account_summ_logged_out
                    Text(stringResource(status), style = foundation.type.caption, color = foundation.colors.onBackgroundMuted)
                    if (account.signedIn) {
                        Text(
                            withUnicodeIsolation(account.username),
                            style = foundation.type.lead,
                            color = foundation.colors.onBackground,
                        )
                    }
                    if (server != null) {
                        Text(
                            withUnicodeIsolation(server),
                            style = foundation.type.caption,
                            color = foundation.colors.onBackgroundMuted,
                        )
                    }
                }
            }
            if (account.signedIn) {
                if (lastSync.time > 0) {
                    val whenSynced = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(lastSync.time))
                    Text(
                        stringResource(
                            if (lastSync.pending) CommonString.paper_sync_last_pending else CommonString.paper_sync_last_clean,
                            whenSynced,
                        ),
                        style = foundation.type.caption,
                        color = foundation.colors.onBackgroundMuted,
                    )
                }
                SyncPrimaryButton(
                    stringResource(CommonString.paper_sync_now),
                    ::syncNow,
                    icon = R.drawable.symbol_sync,
                )
            } else if (server != null) {
                SyncPrimaryButton(
                    TR.syncSignIn(),
                    { startActivity(AccountActivity.getIntent(requireContext())) },
                )
            }
        }
        SettingsSearchTarget(settingsSearchTarget == getString(R.string.custom_sync_server_key)) { modifier, highlighted ->
            SettingsRow(
                title =
                    stringResource(
                        if (server == null) CommonString.paper_sync_server_title_action else CommonString.paper_sync_change_server,
                    ),
                icon = R.drawable.symbol_dns,
                onClick = { startActivity(AccountActivity.getSetupIntent(requireContext())) },
                modifier = modifier,
                highlighted = highlighted,
                trailing = { SettingsChevron() },
            )
        }
        if (account.signedIn) {
            SettingsRow(
                title = stringResource(CommonString.paper_sync_sign_out),
                summary = stringResource(CommonString.paper_sync_sign_out_hint),
                icon = R.drawable.symbol_logout,
                onClick = {
                    confirmSettingChange(
                        title = getString(CommonString.paper_sync_sign_out),
                        message = getString(CommonString.paper_sync_sign_out_hint),
                        confirmText = getString(CommonString.paper_sync_sign_out),
                    ) {
                        accountViewModel.onLogout(requireContext())
                        lastSync = CollectionSyncStatus()
                        updateAccount()
                    }
                },
            )
        }
    }

    private fun syncNow() {
        startActivity(
            DeckPicker
                .getIntent(requireContext(), autoSync = true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
    }

    private fun readAccount() = SyncPageAccount(syncServerAddress(), isLoggedIn(), Prefs.username.orEmpty())

    private data class SyncPageAccount(
        val server: String?,
        val signedIn: Boolean,
        val username: String,
    )

    private data class CollectionSyncStatus(
        val time: Long = 0,
        val pending: Boolean = false,
    )
}
