// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.settings

import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals

class NotificationPreferenceKeysTest {
    @Test
    fun `the legacy notification migration key is unchanged`() {
        // test working directory is the module directory: `AnkiDroid/`
        val keys = stringResources(File("../common/android/src/main/res/values/preferences_notifications.xml"))
        assertEquals("minimumCardsDueForNotification", keys["pref_notifications_minimum_cards_due_key"])
    }

    /** The name and value of each `<string>` in [file] */
    private fun stringResources(file: File): Map<String, String> {
        val strings =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(file)
                .getElementsByTagName("string")
        return (0 until strings.length)
            .map { strings.item(it) }
            .associate { it.attributes.getNamedItem("name").nodeValue to it.textContent.trim() }
    }
}
