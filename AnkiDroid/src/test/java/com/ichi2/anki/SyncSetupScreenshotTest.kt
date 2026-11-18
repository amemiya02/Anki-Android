// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import org.junit.Rule
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import kotlin.reflect.jvm.jvmName
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Sync setup asks for the owner's sync server before sign-in and explains moving decks from AnkiWeb. */
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE)
class SyncSetupScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @After
    override fun tearDown() {
        super.tearDown()
        Prefs.clear()
    }

    @Test
    fun withoutServerAddress() {
        setSyncServer(null)
        openSyncSetup().use { scenario ->
            val account = compose.onNodeWithText(targetContext.getString(CommonString.menu_my_account))
            compose.onNodeWithText("AnkiWeb", substring = true).performScrollTo().assertIsDisplayed()
            account.performScrollTo().assertIsNotEnabled()
            compose.onNodeWithText(targetContext.getString(CommonString.sync_server_required)).assertIsDisplayed()
            captureScreen("no_server")
            scenario.onActivity { assertNull(shadowOf(it).nextStartedActivity, "sign-in does not open") }
        }
    }

    @Test
    fun withServerAddress() {
        setSyncServer(TEST_SYNC_SERVER)
        openSyncSetup().use { scenario ->
            val signInTo = targetContext.getString(CommonString.sync_server_sign_in_to, TEST_SYNC_SERVER)
            val account = compose.onNodeWithText(targetContext.getString(CommonString.menu_my_account))
            account.performScrollTo().assertIsEnabled()
            compose.onNodeWithText(signInTo).assertIsDisplayed()
            captureScreen("with_server")

            account.performClick()
            scenario.onActivity { activity ->
                val intent = assertNotNull(shadowOf(activity).nextStartedActivity)
                assertEquals(AccountActivity::class.java.name, intent.component?.className)
                assertEquals(LoginFragment::class.jvmName, intent.getStringExtra(EXTRA_FRAGMENT_NAME))
            }
        }
    }

    private fun openSyncSetup(): ActivityScenario<PreferencesActivity> =
        ActivityScenario.launch(PreferencesActivity.getIntent(targetContext, CustomSyncServerSettingsFragment::class))
}
