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

class SyncDestinationTest : JvmTest() {
    @get:Rule(order = 0)
    val preferences = JvmPreferencesRule()

    @Test
    fun `AnkiWeb uses the engine default endpoint`() {
        Prefs.hkey = "test"

        assertNull(assertNotNull(syncAuth()).endpoint)
        assertEquals(true, isLoggedIn())
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
    fun `AnkiWeb sync follows its own HTTPS shard`() {
        Prefs.hkey = "test"
        Prefs.currentSyncUri = "https://sync9.ankiweb.net/"

        assertEquals("https://sync9.ankiweb.net/", assertNotNull(syncAuth()).endpoint)
    }

    @Test
    fun `a hosted account never follows an old custom redirect`() {
        Prefs.hkey = "cloud-key"
        Prefs.currentSyncUri = TEST_SYNC_SERVER
        assertNull(assertNotNull(syncAuth()).endpoint)
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
