// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.permissions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.google.testing.junit.testparameterinjector.TestParameter
import com.ichi2.anki.CheckAccessibility
import com.ichi2.anki.CommonString
import com.ichi2.anki.FontSizeConfig.PERCENT_200
import com.ichi2.anki.IncludeFontSizes
import com.ichi2.anki.IncludeLanguages
import com.ichi2.anki.LanguageConfig.ARABIC
import com.ichi2.anki.LanguageConfig.JAPANESE
import com.ichi2.anki.LanguageConfig.SIMPLIFIED_CHINESE
import com.ichi2.anki.ScreenshotTest
import com.ichi2.anki.StoragePermissionSet
import com.ichi2.anki.common.utils.dp
import com.ichi2.testutils.dispatchInsets
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse

@CheckAccessibility
@IncludeLanguages(SIMPLIFIED_CHINESE, JAPANESE, ARABIC)
@IncludeFontSizes(PERCENT_200)
class PermissionsActivityScreenshotTest : ScreenshotTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun permissions(
        @TestParameter("LEGACY_ACCESS", "EXTERNAL_MANAGER") permissionSet: StoragePermissionSet,
    ) {
        setPhoneQualifiers()
        val activity =
            startActivityNormallyOpenCollectionWithIntent(
                PermissionsActivity::class.java,
                PermissionsActivity.getIntent(targetContext, permissionSet),
            )
        assertFalse(activity.isFinishing)
        activity.dispatchInsets(navBarBottom = 24.dp)
        val permissionTitle =
            if (permissionSet == StoragePermissionSet.LEGACY_ACCESS) {
                CommonString.storage_access_title
            } else {
                CommonString.all_files_access_title
            }
        compose.onNodeWithText(getResourceString(permissionTitle)).assertIsDisplayed()
        compose.onNodeWithText(getResourceString(CommonString.dialog_continue)).assertIsDisplayed()
        captureScreen("permissions_${permissionSet.name.lowercase()}")
    }
}
