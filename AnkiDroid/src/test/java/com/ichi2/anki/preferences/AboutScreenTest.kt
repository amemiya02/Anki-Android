// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.text.Spanned
import android.text.style.URLSpan
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.R
import com.ichi2.anki.RobolectricTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.hasItem
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

/**
 * The about screen tells a person that the app is Hayaki, an unofficial fork of AnkiDroid,
 * credits Upstream, states the licence and links to the Fork's public source (spec: "Fork and identity").
 */
@RunWith(AndroidJUnit4::class)
class AboutScreenTest : RobolectricTest() {
    @Test
    fun `the about screen names the app Hayaki`() =
        withAboutScreen { screen ->
            assertEquals("Hayaki", screen.text(R.id.about_app_name).toString())
        }

    @Test
    fun `the about screen says the app is an unofficial fork of AnkiDroid and credits Upstream`() =
        withAboutScreen { screen ->
            val credit = screen.text(R.id.contributors_description)
            assertThat(credit.toString(), containsString("Hayaki is an unofficial fork of AnkiDroid"))
            assertThat(credit.links(), hasItem(UPSTREAM_SOURCE))
            assertThat(credit.links(), hasItem(UPSTREAM_CONTRIBUTORS))
        }

    @Test
    fun `the about screen states the licence and links to the Fork's source`() =
        withAboutScreen { screen ->
            val licence = screen.text(R.id.license_description)
            assertThat(licence.toString(), containsString("Hayaki is released under the GPL-3.0"))
            assertThat(licence.links(), hasItem(FORK_SOURCE))
        }

    private fun withAboutScreen(block: (PreferencesActivity) -> Unit) {
        val intent = PreferencesActivity.getIntent(targetContext, AboutFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                advanceRobolectricLooper()
                block(activity)
            }
        }
    }

    private fun PreferencesActivity.text(
        @IdRes id: Int,
    ): CharSequence = findViewById<TextView>(id).text

    private fun CharSequence.links(): List<String> = (this as? Spanned)?.getSpans(0, length, URLSpan::class.java)?.map { it.url }.orEmpty()

    companion object {
        const val FORK_SOURCE = "https://github.com/amemiya02/Anki-Android"
        const val UPSTREAM_SOURCE = "https://github.com/ankidroid/Anki-Android"
        const val UPSTREAM_CONTRIBUTORS = "https://github.com/ankidroid/Anki-Android#credits"
    }
}
