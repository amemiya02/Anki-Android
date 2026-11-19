// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.worker

import androidx.work.Data
import androidx.work.workDataOf
import anki.sync.syncAuth
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.sync.SyncAuth

internal const val SYNC_SESSION_KEY = "syncSession"
private const val SYNC_AUTH_KEY = "syncAuth"

/** Preserve all sync settings, including the network timeout, when passing auth to either worker. */
internal fun SyncAuth.toWorkData(session: String = Prefs.syncSession): Data =
    workDataOf(
        SYNC_AUTH_KEY to toProto().toByteArray(),
        SYNC_SESSION_KEY to session,
    )

internal fun Data.isCurrentSyncSession(): Boolean = getString(SYNC_SESSION_KEY).orEmpty() == Prefs.syncSession

internal fun Data.toSyncAuth(): SyncAuth? {
    if (!isCurrentSyncSession()) return null
    getByteArray(SYNC_AUTH_KEY)?.let { return SyncAuth(anki.sync.SyncAuth.parseFrom(it)) }

    // Temporary workaround for an app update from <2.26.0
    val hkey = getString("hkey") ?: return null
    return SyncAuth(
        syncAuth {
            this.hkey = hkey
            getString("endpoint")?.let { endpoint = it }
            ioTimeoutSecs = Prefs.networkTimeoutSecs
        },
    )
}
