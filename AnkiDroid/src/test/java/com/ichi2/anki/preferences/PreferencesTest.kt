// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.RobolectricTest
import com.ichi2.anki.libanki.exception.ConfirmModSchemaException
import com.ichi2.anki.utils.CollectionPreferences
import com.ichi2.testutils.getJavaMethodAsAccessible
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class PreferencesTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var preferences: PreferencesActivity

    @Before
    override fun setUp() {
        super.setUp()
        preferences = PreferencesActivity()
        val attachBaseContext =
            getJavaMethodAsAccessible(
                AppCompatActivity::class.java,
                "attachBaseContext",
                Context::class.java,
            )
        attachBaseContext.invoke(preferences, targetContext)
    }

    @Test
    fun testDayOffsetExhaustive() {
        runTest {
            for (i in 0..23) {
                setDayOffset(preferences, i)
                assertThat(CollectionPreferences.getDayOffset(), equalTo(i))
            }
        }
    }

    @Test
    fun `PreferenceFragmentsProvider lists every preference fragment`() {
        val expected = PreferenceTestUtils.getAllPreferencesFragments(targetContext).map { it::class }.toSet()
        assertEquals(expected, PreferenceFragmentsProvider.FRAGMENTS.toSet())
    }

    /** checks if any of the Preferences fragments throws while being created */
    @Test
    fun fragmentsDoNotThrowOnCreation() {
        for (fragment in PreferenceTestUtils.getAllPreferencesFragments(targetContext)) {
            val intent = PreferencesActivity.getIntent(targetContext, fragment::class)
            ActivityScenario.launch<PreferencesActivity>(intent).use { activityScenario ->
                compose.waitForIdle()
                activityScenario.onActivity { activity ->
                    val preferences = activity.fragment as PreferencesFragment
                    assertEquals(fragment::class, preferences.currentPage!!::class)
                }
            }
        }
    }

    @Test
    @Throws(ConfirmModSchemaException::class)
    fun setDayOffsetSetsConfig() {
        runTest {
            setDayOffset(preferences, 2)
            assertThat("rollover config should be set to new value", col.config.get("rollover") ?: 4, equalTo(2))
        }
    }
}
