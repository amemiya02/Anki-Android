// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2022 Ali Ahnaf <aliahnaf327@gmail.com>

package com.ichi2.anki

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.account.AccountActivity
import com.ichi2.testutils.TEST_SYNC_SERVER
import com.ichi2.testutils.setSyncServer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyAccountTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun setup() {
        setSyncServer(TEST_SYNC_SERVER)
    }

    @Test
    fun `the sign-in screen offers no AnkiWeb links or privacy button`() =
        withLogin {
            field("Username").assertIsDisplayed()
            for (text in listOf("AnkiWeb", "Sign up", "Reset password", "Forgot", "Privacy")) {
                compose.onNodeWithText(text, substring = true).assertDoesNotExist()
            }
        }

    @Test
    fun `sign-in accepts a username without an email address`() =
        withLogin {
            field("Username").performTextInput("paper")
            field("Password").performTextInput("randomStrongPassword")
            signIn.assertIsEnabled()
        }

    @Test
    fun `sign-in requires a username`() =
        withLogin {
            field("Password").performTextInput("randomStrongPassword")
            signIn.assertIsNotEnabled()
        }

    @Test
    fun `sign-in requires a password`() =
        withLogin {
            field("Username").performTextInput("paper")
            signIn.assertIsNotEnabled()
        }

    private fun withLogin(assertions: () -> Unit) {
        ActivityScenario.launch<AccountActivity>(AccountActivity.getIntent(targetContext)).use { assertions() }
    }

    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasText(label))

    private val signIn get() = compose.onNodeWithText("Sign in")
}
