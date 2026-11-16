// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.testutils

import com.ichi2.anki.settings.Prefs

/** A sync server address for tests. Nothing listens there: tests replace the Engine's network calls. */
const val TEST_SYNC_SERVER = "https://sync.example.com/"

/** Sets the owner's own sync server address, as sync setup does, or removes it when [address] is null */
fun setSyncServer(address: String?) {
    Prefs.customSyncUri = address
    Prefs.isCustomSyncEnabled = address != null
}

/** Records a sign-in to [TEST_SYNC_SERVER] without contacting it */
fun signInToSyncServer(hkey: String = "test") {
    setSyncServer(TEST_SYNC_SERVER)
    Prefs.hkey = hkey
}
