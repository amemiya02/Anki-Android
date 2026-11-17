// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.R
import com.ichi2.anki.Reviewer
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.previewer.CardViewerActivity
import com.ichi2.anki.settings.Prefs
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser
import kotlin.test.assertEquals

/** The redesigned study screen is the Fork's default; the old one stays selectable (ADR-0004). */
@RunWith(AndroidJUnit4::class)
class StudyScreenDefaultTest : RobolectricTest() {
    @Test
    fun `studying opens the redesigned study screen when nothing was chosen`() {
        val intent = Reviewer.getIntent(targetContext)

        assertEquals(CardViewerActivity::class.java.name, intent.component?.className)
    }

    @Test
    fun `the old study screen can still be chosen in settings`() {
        Prefs.isNewStudyScreenEnabled = false

        val intent = Reviewer.getIntent(targetContext)

        assertEquals(Reviewer::class.java.name, intent.component?.className)
    }

    @Test
    fun `the settings switch shows the redesigned study screen as the default`() {
        assertEquals("true", switchDefaultValue(R.xml.preferences_reviewer))
    }

    private fun switchDefaultValue(xmlRes: Int): String? {
        val key = targetContext.getString(R.string.new_reviewer_options_key)
        val parser = targetContext.resources.getXml(xmlRes)
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            val attributes = (0 until parser.attributeCount).associate { parser.getAttributeName(it) to parser.getAttributeValue(it) }
            val keyAttribute = attributes["key"] ?: continue
            if (keyAttribute != key && keyAttribute != "@${R.string.new_reviewer_options_key}") continue
            return attributes["defaultValue"]
        }
        error("no preference with key $key")
    }
}
