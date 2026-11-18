// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.account

import android.content.Context
import android.content.SharedPreferences
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
import com.ichi2.anki.worker.UniqueWorkNames
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LoggedInViewModel : ViewModel() {
    internal val server = MutableStateFlow(syncServerAddress())

    internal fun setServer(
        context: Context,
        address: String?,
    ) {
        if (address == syncServerAddress() && (address != null || Prefs.customSyncUri.isNullOrBlank())) return
        val wasAuthenticated = !Prefs.hkey.isNullOrEmpty()
        Prefs.sharedPrefs.edit {
            putString(SyncPreferences.CUSTOM_SYNC_URI, address)
            putBoolean(SyncPreferences.CUSTOM_SYNC_ENABLED, address != null)
            clearAccount()
        }
        server.value = address
        if (wasAuthenticated) stopSync(context)
    }

    internal fun onLogout(context: Context) {
        if (!isLoggedIn()) return
        Prefs.sharedPrefs.edit { clearAccount() }
        stopSync(context)
    }

    private fun stopSync(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(UniqueWorkNames.SYNC)
            cancelUniqueWork(UniqueWorkNames.SYNC_MEDIA)
        }
        forceMediaResync()
    }

    private fun forceMediaResync() {
        viewModelScope.launch { withCol { media.forceResync() } }
    }

    private fun SharedPreferences.Editor.clearAccount() {
        remove(Prefs.key(R.string.hkey_key))
        remove(Prefs.key(R.string.username_key))
        remove(SyncPreferences.PREF_CURRENT_SYNC_URI)
        remove(Prefs.key(R.string.last_sync_time_key))
        remove(Prefs.key(R.string.last_successful_sync_time_key))
    }
}
