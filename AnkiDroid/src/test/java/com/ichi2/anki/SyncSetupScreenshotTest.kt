// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.app.Activity
import android.view.View
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.SingleFragmentActivity.Companion.EXTRA_FRAGMENT_NAME
import com.ichi2.anki.account.AccountActivity
import com.ichi2.anki.account.LoginFragment
import com.ichi2.anki.preferences.CustomSyncServerSettingsFragment
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.anki.settings.Prefs
import com.ichi2.testutils.TEST_SYNC_SERVER
import com.ichi2.testutils.ext.clear
import com.ichi2.testutils.setSyncServer
import org.junit.After
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import kotlin.reflect.jvm.jvmName
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Sync setup: the Fork asks for the owner's own sync server before any sign-in, and explains
 * how to bring decks in from AnkiWeb, which it cannot sign in to (ADR-0001).
 *
 * `./gradlew :AnkiDroid:recordRoborazziPlayDebug -Pscreenshot --tests "com.ichi2.anki.SyncSetupScreenshotTest"`
 */
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE)
class SyncSetupScreenshotTest : ScreenshotTest() {
    @After
    override fun tearDown() {
        super.tearDown()
        Prefs.clear()
    }

    @Test
    fun withoutServerAddress() {
        setSyncServer(null)
        withSyncSetup { activity ->
            assertTrue(activity.isTextShown(CommonString.sync_server_required), "asks for a server address")
            assertFalse(activity.rowWithText(CommonString.sync_account_title).isEnabled, "sign-in is not possible")
            assertTrue(activity.isTextShown("AnkiWeb"), "explains the route from AnkiWeb")
            captureScreen("no_server")

            activity.rowWithText(CommonString.sync_account_title).performClick()
            assertNull(shadowOf(activity).nextStartedActivity, "sign-in does not open")
        }
    }

    @Test
    fun withServerAddress() {
        setSyncServer(TEST_SYNC_SERVER)
        withSyncSetup { activity ->
            val signInTo = activity.getString(CommonString.sync_server_sign_in_to, TEST_SYNC_SERVER)
            assertTrue(activity.isTextShown(signInTo), "offers to sign in to the server")
            assertTrue(activity.rowWithText(CommonString.sync_account_title).isEnabled, "sign-in is possible")
            captureScreen("with_server")

            activity.rowWithText(CommonString.sync_account_title).performClick()
            val intent = assertNotNull(shadowOf(activity).nextStartedActivity)
            assertEquals(AccountActivity::class.java.name, intent.component?.className)
            assertEquals(LoginFragment::class.jvmName, intent.getStringExtra(EXTRA_FRAGMENT_NAME))
        }
    }

    private fun withSyncSetup(block: (PreferencesActivity) -> Unit) {
        val intent = PreferencesActivity.getIntent(targetContext, CustomSyncServerSettingsFragment::class)
        ActivityScenario.launch<PreferencesActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                advanceRobolectricLooper()
                block(activity)
            }
        }
    }

    /** Views showing [text], or text containing it, ignoring case */
    private fun Activity.shownViewsWithText(text: String): List<View> {
        val views = ArrayList<View>()
        window.decorView.findViewsWithText(views, text, View.FIND_VIEWS_WITH_TEXT)
        return views.filter { it.isShown }
    }

    private fun Activity.isTextShown(
        @StringRes text: Int,
    ) = isTextShown(getString(text))

    private fun Activity.isTextShown(text: String) = shownViewsWithText(text).isNotEmpty()

    /** The list row of the preference titled [title] */
    private fun Activity.rowWithText(
        @StringRes title: Int,
    ): View {
        var view = shownViewsWithText(getString(title)).single()
        while (view.parent !is RecyclerView) view = view.parent as View
        return view
    }
}
