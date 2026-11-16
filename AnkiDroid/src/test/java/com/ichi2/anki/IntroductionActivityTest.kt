// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2026 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.introduction.SetupCollectionFragment
import com.ichi2.anki.introduction.SetupCollectionFragment.CollectionSetupOption.DeckPickerWithNewCollection
import com.ichi2.anki.ui.windows.permissions.PermissionsActivity
import com.ichi2.testutils.withDeniedPermissions
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Tests [IntroductionActivity] */
@RunWith(AndroidJUnit4::class)
class IntroductionActivityTest : RobolectricTest() {
    @Test
    fun `the first-run screen offers import in place of AnkiWeb sync`() =
        runTest {
            val activity = startRegularActivity<IntroductionActivity>(Intent())

            assertTrue(activity.hasVisibleText(CommonString.intro_import), "import is offered")
            assertFalse(activity.hasVisibleText("AnkiWeb"), "AnkiWeb sync is not offered")
        }

    // At API 29, selectStoragePermissions returns StoragePermissionSet.LEGACY_ACCESS, which can be denied.
    // StoragePermissionSet.APP_PRIVATE requires no permission
    @Config(sdk = [Build.VERSION_CODES.Q])
    @Test
    fun `Import without storage permission opens PermissionsActivity`() =
        runTest {
            withDeniedPermissions(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE) {
                val activity = startRegularActivity<IntroductionActivity>(Intent())

                activity.clickText(CommonString.intro_import)

                val intent = assertNotNull(shadowOf(activity).nextStartedActivity)
                assertThat(
                    intent.component?.shortClassName,
                    equalTo(PermissionsActivity::class.java.name),
                )
            }
        }

    @Test
    fun `Import opens the deck list ready to import`() =
        runTest {
            val activity = startRegularActivity<IntroductionActivity>(Intent())

            activity.clickText(CommonString.intro_import)

            val intent = assertNotNull(shadowOf(activity).nextStartedActivity)
            assertThat(intent.component?.className, equalTo(DeckPicker::class.java.name))
            assertTrue(intent.getBooleanExtra(DeckPicker.EXTRA_IMPORT, false), "the deck list is asked to import")
            assertFalse(intent.getBooleanExtra(DeckPicker.EXTRA_SYNC_FROM_LOGIN, false), "the deck list is not asked to sync")
        }

    @Test
    fun `Get Started opens DeckPicker without the sync-from-login flag`() =
        runTest {
            val activity = startRegularActivity<IntroductionActivity>(Intent())

            activity.supportFragmentManager.setFragmentResult(
                SetupCollectionFragment.REQUEST_COLLECTION_SETUP,
                Bundle().apply { putParcelable(SetupCollectionFragment.RESULT_KEY, DeckPickerWithNewCollection) },
            )

            val intent = assertNotNull(shadowOf(activity).nextStartedActivity)
            assertThat(
                intent.component?.className,
                equalTo(DeckPicker::class.java.name),
            )
            assertThat(
                "DeckPicker isn't asked to sync (Get Started, not Sync)",
                intent.getBooleanExtra(DeckPicker.EXTRA_SYNC_FROM_LOGIN, false),
                equalTo(false),
            )
            assertThat(
                "intro is marked as shown so it doesn't reappear",
                getPreferences().getBoolean(IntroductionActivity.PREF_INTRODUCTION_SLIDES_SHOWN, false),
                equalTo(true),
            )
        }

    private fun IntroductionActivity.hasVisibleText(
        @StringRes text: Int,
    ) = hasVisibleText(getString(text))

    /** Whether a shown view's text contains [text], ignoring case */
    private fun IntroductionActivity.hasVisibleText(text: String): Boolean {
        val views = ArrayList<View>()
        window.decorView.findViewsWithText(views, text, View.FIND_VIEWS_WITH_TEXT)
        return views.any { it.isShown }
    }

    private fun IntroductionActivity.clickText(
        @StringRes text: Int,
    ) {
        val views = ArrayList<View>()
        window.decorView.findViewsWithText(views, getString(text), View.FIND_VIEWS_WITH_TEXT)
        views.single { it.isShown }.performClick()
        advanceRobolectricLooper()
    }
}
