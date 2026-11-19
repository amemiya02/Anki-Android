// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.app.Activity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.account.AccountActivity
import com.ichi2.anki.preferences.PreferencesActivity
import com.ichi2.anki.settings.Prefs
import com.ichi2.testutils.TEST_SYNC_SERVER
import com.ichi2.testutils.ext.clear
import com.ichi2.testutils.ext.reopenWithLanguage
import com.ichi2.testutils.setSyncServer
import com.ichi2.testutils.signInToSyncServer
import org.junit.After
import org.junit.Rule
import org.junit.Test

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class SyncSetupScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    override fun getCollectionStorageMode() = CollectionStorageMode.ON_DISK

    @After
    override fun tearDown() {
        super.tearDown()
        Prefs.clear()
    }

    @Test
    fun addressIsTheFirstStepForASelfHostedServer() {
        openWithAppLanguage()
        setSyncServer(null)
        ActivityScenario.launch<AccountActivity>(AccountActivity.getSetupIntent(targetContext, forResult = true)).use {
            compose.onNodeWithText(targetContext.getString(CommonString.paper_sync_step, 1)).assertIsDisplayed()
            val next = button(CommonString.dialog_continue)
            next.assertIsNotEnabled()
            val address = field(CommonString.paper_sync_server_address)
            address.performTextReplacement("https://sync9.ankiweb.net/")
            next.assertIsNotEnabled()
            compose.onNodeWithText(label(CommonString.paper_sync_invalid_address)).assertIsDisplayed()

            address.performTextReplacement(TEST_SYNC_SERVER)
            next.assertIsEnabled()
            captureScreen("address")
            next.performClick()

            compose.onNodeWithText(targetContext.getString(CommonString.paper_sync_step, 2)).assertIsDisplayed()
            compose.onNodeWithText(TEST_SYNC_SERVER, substring = true).assertIsDisplayed()
            compose.onNode(hasText(TR.syncSignIn()) and hasClickAction()).assertIsNotEnabled()
            field(CommonString.paper_sync_username).performTextReplacement("paper")
            field(CommonString.password).performTextReplacement("sample-password")
            compose.onNode(hasText(TR.syncSignIn()) and hasClickAction()).assertIsEnabled()
            captureScreen("sign_in")

            button(CommonString.paper_sync_change_server).performClick()
            compose.onNodeWithText(targetContext.getString(CommonString.paper_sync_step, 1)).assertIsDisplayed()
            compose.onNodeWithText(TEST_SYNC_SERVER, substring = true).assertIsDisplayed()
        }
    }

    @Test
    fun settingsOpensAccountAndSettingsInOnePlace() {
        openWithAppLanguage()
        signInToSyncServer()
        Prefs.username = "paper"
        Prefs.syncNotifsRequestShown = true
        ActivityScenario.launch<PreferencesActivity>(PreferencesActivity.getIntent(targetContext)).use {
            button(CommonString.pref_cat_sync).performScrollTo().performClick()
            compose.onNodeWithText(label(CommonString.paper_sync_signed_in_as)).assertIsDisplayed()
            compose.onNodeWithText("paper", substring = true).assertIsDisplayed()
            compose.onNodeWithText(TEST_SYNC_SERVER, substring = true).assertIsDisplayed()
            button(CommonString.paper_sync_now).assertIsDisplayed()
            captureScreen("account")

            compose.onNodeWithText(label(CommonString.paper_sync_auto)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(label(CommonString.paper_sync_badge)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(label(CommonString.paper_sync_metered)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(label(CommonString.paper_sync_media)).performScrollTo().assertIsDisplayed()
            button(CommonString.custom_sync_certificate_title).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(label(CommonString.one_way_sync_title)).performScrollTo().assertIsDisplayed()
            captureScreen("account_advanced")

            button(CommonString.paper_sync_sign_out).performScrollTo().performClick()
            compose
                .onNode(hasText(label(CommonString.paper_sync_sign_out)) and hasClickAction() and hasAnyAncestor(isDialog()))
                .performClick()
            compose.onNodeWithText(label(CommonString.sync_account_summ_logged_out)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(TEST_SYNC_SERVER, substring = true).assertIsDisplayed()
            compose.onNode(hasText(TR.syncSignIn()) and hasClickAction()).assertIsEnabled()
            captureScreen("signed_out")
        }
    }

    @Test
    fun ankiWebOffersAnImportPath() {
        openWithAppLanguage()
        setSyncServer(null)
        ActivityScenario.launch<Activity>(AccountActivity.getSetupIntent(targetContext)).use {
            button(CommonString.paper_sync_from_ankiweb).performScrollTo().performClick()
            compose.onNodeWithText(label(CommonString.paper_sync_import_steps)).assertIsDisplayed()
            button(CommonString.paper_sync_choose_file).assertIsDisplayed()
            captureScreen("ankiweb_import")
        }
    }

    private fun openWithAppLanguage() {
        setPhoneQualifiers()
        runTest { col.reopenWithLanguage(language.option) }
    }

    private fun label(resource: Int) = targetContext.getString(resource)

    private fun button(resource: Int) = compose.onNode(hasText(label(resource)) and hasClickAction())

    private fun field(resource: Int) = compose.onNode(hasText(label(resource)) and hasSetTextAction())
}
