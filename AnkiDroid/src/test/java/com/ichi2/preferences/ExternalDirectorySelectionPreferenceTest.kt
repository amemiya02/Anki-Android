// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: 2026 Shaan Narendran <shaannaren06@gmail.com>

package com.ichi2.preferences

import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.EmptyApplicationCategory
import com.ichi2.anki.R
import com.ichi2.anki.common.preferences.sharedPrefs
import com.ichi2.anki.common.storage.CollectionHelper
import com.ichi2.anki.startup.getDefaultAnkiDroidDirectory
import com.ichi2.testutils.AndroidTest
import com.ichi2.testutils.EmptyApplication
import com.ichi2.testutils.targetContext
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tests that [ExternalDirectorySelectionPreference] offers the directories a user
 * would expect to pick from.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = EmptyApplication::class)
@Category(EmptyApplicationCategory::class)
class ExternalDirectorySelectionPreferenceTest : AndroidTest {
    @Test
    fun `the default directory is offered`() {
        val pref = buildPreference()

        assertThat(pref.loadDirectories(), hasItem(getDefaultAnkiDroidDirectory(targetContext).absolutePath))
    }

    @Test
    fun `the current directory is offered`() {
        val current = File(targetContext.filesDir, "AnkiDroidTest").absolutePath
        targetContext.sharedPrefs().edit(commit = true) {
            putString(CollectionHelper.PREF_COLLECTION_PATH, current)
        }

        val pref = buildPreference()

        assertThat(pref.loadDirectories(), hasItem(current))
    }

    private fun buildPreference(): ExternalDirectorySelectionPreference =
        checkNotNull(targetContext.loadSettings(R.xml.preferences_advanced).findPreference(CollectionHelper.PREF_COLLECTION_PATH))
}
