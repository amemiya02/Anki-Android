// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.account

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.MainThread
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.R
import com.ichi2.anki.SyncPreferences
import com.ichi2.anki.isLoggedIn
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.syncServerAddress
import com.ichi2.anki.usesAnkiWeb
import com.ichi2.anki.worker.UniqueWorkNames
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class LoggedInViewModel : ViewModel() {
    internal val server = MutableStateFlow(syncServerAddress())

    internal fun setServer(
        context: Context,
        address: String?,
        certificate: String = "",
    ) {
        val certificateKey = Prefs.key(R.string.custom_sync_certificate_key)
        if (address == syncServerAddress() && (address != null || usesAnkiWeb())) {
            Prefs.sharedPrefs.edit { putString(certificateKey, certificate) }
            return
        }
        val wasAuthenticated = !Prefs.hkey.isNullOrEmpty()
        cancelQueuedSync(context)
        Prefs.sharedPrefs.edit {
            putString(SyncPreferences.CUSTOM_SYNC_URI, address)
            putBoolean(SyncPreferences.CUSTOM_SYNC_ENABLED, address != null)
            putString(certificateKey, certificate)
            replaceSyncAccount()
        }
        server.value = address
        if (wasAuthenticated) forceMediaResync()
    }

    internal fun onLogout(context: Context) {
        if (!isLoggedIn()) return
        updateSyncAccount(context)
        forceMediaResync()
    }

    private fun forceMediaResync() {
        viewModelScope.launch { withCol { media.forceResync() } }
    }
}

@MainThread
internal fun updateSyncAccount(
    context: Context,
    username: String? = null,
    hkey: String? = null,
) {
    // Submit cancellation before publishing credentials that can enqueue new work.
    cancelQueuedSync(context)
    Prefs.sharedPrefs.edit { replaceSyncAccount(username, hkey) }
}

private fun cancelQueuedSync(context: Context) {
    WorkManager.getInstance(context).apply {
        cancelUniqueWork(UniqueWorkNames.SYNC)
        cancelUniqueWork(UniqueWorkNames.SYNC_MEDIA)
    }
}

private fun SharedPreferences.Editor.replaceSyncAccount(
    username: String? = null,
    hkey: String? = null,
) {
    putString(Prefs.key(R.string.sync_session_key), UUID.randomUUID().toString())
    putString(Prefs.key(R.string.hkey_key), hkey)
    putString(Prefs.key(R.string.username_key), username)
    remove(Prefs.key(R.string.media_sync_paused_key))
    remove(SyncPreferences.PREF_CURRENT_SYNC_URI)
    remove(Prefs.key(R.string.last_sync_time_key))
    remove(Prefs.key(R.string.last_successful_sync_time_key))
}
