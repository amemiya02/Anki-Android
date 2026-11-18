// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import com.ichi2.anki.settings.Prefs
import com.ichi2.testutils.JvmPreferencesRule
import com.ichi2.testutils.JvmTest
import com.ichi2.testutils.TEST_SYNC_SERVER
import com.ichi2.testutils.setSyncServer
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The Fork never syncs with AnkiWeb (ADR-0001). The Engine falls back to AnkiWeb when it is
 * handed no endpoint, so the app hands it sync credentials only together with the owner's own
 * server address.
 */
class NoAnkiWebSyncTest : JvmTest() {
    @get:Rule(order = 0)
    val preferences = JvmPreferencesRule()

    @Test
    fun `without a server address there are no sync credentials`() {
        Prefs.hkey = "test"

        assertNull(syncAuth())
        assertFalse(isLoggedIn())
    }

    @Test
    fun `a server address that is switched off counts as none`() {
        Prefs.hkey = "test"
        setSyncServer(TEST_SYNC_SERVER)
        Prefs.isCustomSyncEnabled = false

        assertNull(syncAuth())
    }

    @Test
    fun `a blank server address counts as none`() {
        Prefs.hkey = "test"
        setSyncServer("  ")

        assertNull(syncAuth())
    }

    @Test
    fun `an endpoint remembered from an earlier sync is not used without a server address`() {
        Prefs.hkey = "test"
        Prefs.currentSyncUri = "https://sync9.ankiweb.net/"

        assertNull(syncAuth())
    }

    @Test
    fun `AnkiWeb entered as the server cannot receive credentials`() {
        Prefs.hkey = "test"
        setSyncServer("https://sync9.ankiweb.net/")

        assertNull(getEndpoint())
        assertNull(syncAuth())
        assertFalse(isLoggedIn())
    }

    @Test
    fun `an AnkiWeb redirect is ignored when using a self-hosted server`() {
        setSyncServer(TEST_SYNC_SERVER)
        Prefs.currentSyncUri = "https://ANKIWEB.NET./"

        assertEquals(TEST_SYNC_SERVER, getEndpoint())
    }

    @Test
    fun `sync credentials name the server address`() {
        Prefs.hkey = "test"
        setSyncServer(TEST_SYNC_SERVER)

        val auth = assertNotNull(syncAuth())
        assertEquals(TEST_SYNC_SERVER, auth.endpoint)
        assertEquals("test", auth.hkey)
    }
}
