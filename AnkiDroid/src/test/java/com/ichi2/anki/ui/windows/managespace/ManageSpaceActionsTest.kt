// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.managespace

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.CommonString
import com.ichi2.anki.RobolectricTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManageSpaceActionsTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun clearingAppDataRequiresConfirmationAndCanBeCancelled() {
        startActivityNormallyOpenCollectionWithIntent(
            ManageSpaceActivity::class.java,
            Intent(targetContext, ManageSpaceActivity::class.java),
        )
        advanceRobolectricLooper()

        val clearData = compose.onNodeWithText(targetContext.getString(CommonString.pref__delete_everything__title))
        clearData.performScrollTo().performClick()
        compose.onNodeWithText(targetContext.getString(CommonString.dialog__delete_everything__title)).assertIsDisplayed()
        compose.onNodeWithText(targetContext.getString(CommonString.dialog_cancel)).performClick()
        clearData.assertIsDisplayed()
    }
}
