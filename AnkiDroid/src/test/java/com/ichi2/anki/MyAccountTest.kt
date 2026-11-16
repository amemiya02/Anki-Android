// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2022 Ali Ahnaf <aliahnaf327@gmail.com>

package com.ichi2.anki

import android.view.View
import android.widget.Button
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.textfield.TextInputEditText
import com.ichi2.anki.account.LoginFragment
import com.ichi2.anki.settings.Prefs
import com.ichi2.testutils.TEST_SYNC_SERVER
import com.ichi2.testutils.launchFragmentInContainer
import com.ichi2.testutils.setSyncServer
import junit.framework.TestCase.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class MyAccountTest : RobolectricTest() {
    @Before
    fun setup() {
        Prefs.username = ""
        Prefs.hkey = ""
        setSyncServer(TEST_SYNC_SERVER)
    }

    @Test
    fun `the sign-in screen offers no AnkiWeb sign up or password reset`() {
        launchFragmentInContainer<LoginFragment>().use { scenario ->
            scenario.onFragment { fragment ->
                for (text in listOf("AnkiWeb", "Sign up", "Reset password", "Forgot")) {
                    val views = ArrayList<View>()
                    fragment.requireView().findViewsWithText(views, text, View.FIND_VIEWS_WITH_TEXT)
                    assertTrue(views.none { it.isShown }, "'$text' is not offered")
                }
            }
        }
    }

    @Test
    fun testLoginEmailPasswordProvided() {
        launchFragmentInContainer<LoginFragment>().use { scenario ->
            scenario.onFragment { fragment ->
                val testPassword = "randomStrongPassword"
                val testEmail = "random.email@example.com"

                fragment.view?.findViewById<TextInputEditText>(R.id.username)?.setText(testEmail)
                fragment.view?.findViewById<TextInputEditText>(R.id.password)?.setText(testPassword)

                val loginButton = fragment.view?.findViewById<Button>(R.id.login_button)
                assertEquals(loginButton?.isEnabled, true)
            }
        }
    }

    @Test
    fun testLoginFailsNoEmailProvided() {
        launchFragmentInContainer<LoginFragment>().use { scenario ->
            scenario.onFragment { fragment ->
                val testPassword = "randomStrongPassword"

                fragment.view?.findViewById<TextInputEditText>(R.id.password)?.setText(testPassword)
                val loginButton = fragment.view?.findViewById<Button>(R.id.login_button)
                assertFalse(loginButton?.isEnabled == true)
            }
        }
    }

    @Test
    fun testLoginFailsNoPasswordProvided() {
        launchFragmentInContainer<LoginFragment>().use { scenario ->
            scenario.onFragment { fragment ->
                val testEmail = "random.email@example.com"

                fragment.view?.findViewById<TextInputEditText>(R.id.username)?.setText(testEmail)
                val loginButton = fragment.view?.findViewById<Button>(R.id.login_button)
                assertFalse(loginButton?.isEnabled == true)
            }
        }
    }
}
