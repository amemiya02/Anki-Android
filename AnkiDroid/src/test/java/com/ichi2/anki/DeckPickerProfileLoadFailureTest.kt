// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: 2026 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.InitialActivity.StartupFailure
import com.ichi2.anki.RobolectricTest.Companion.advanceRobolectricLooper
import com.ichi2.anki.dialogs.DatabaseErrorDialog
import com.ichi2.anki.dialogs.DeckPickerNoSpaceLeftDialog
import com.ichi2.anki.dialogs.ProfileLoadFailureDialog
import com.ichi2.anki.multiprofile.ProfileId
import com.ichi2.anki.multiprofile.ProfileManager
import com.ichi2.anki.multiprofile.ProfileManager.Companion.PREF_LAST_ACTIVE_PROFILE_ID
import com.ichi2.anki.multiprofile.ProfileManager.Companion.PROFILE_REGISTRY_FILENAME
import com.ichi2.anki.multiprofile.ProfileName
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowToast
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class DeckPickerProfileLoadFailureTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val registry
        get() = targetContext.getSharedPreferences(PROFILE_REGISTRY_FILENAME, Context.MODE_PRIVATE)

    private val DeckPicker.profileFailureDialogs: List<Dialog>
        get() = supportFragmentManager.fragments.filterIsInstance<ProfileLoadFailureDialog>().showingDialogs()

    private val DeckPicker.storageFullDialogs: List<Dialog>
        get() =
            supportFragmentManager.fragments
                .filterIsInstance<DialogFragment>()
                .filter { it is DeckPickerNoSpaceLeftDialog || it is DatabaseErrorDialog }
                .showingDialogs()

    private fun List<DialogFragment>.showingDialogs() = mapNotNull { it.dialog?.takeIf { dialog -> dialog.isShowing } }

    @After
    fun resetProfileFailure() {
        ProfileManager.resetForTesting()
    }

    @Test
    fun `startup explains that a profile could not be opened`() {
        failToOpenProfile()

        deckPicker {
            assertEquals(1, profileFailureDialogs.size)
        }
    }

    @Test
    fun `a normal start shows no explanation`() {
        deckPicker {
            assertEquals(0, profileFailureDialogs.size)
        }
    }

    @Test
    fun `the explanation names the profile that opened instead`() {
        failToOpenProfile(defaultProfileName = "Home")

        deckPicker {
            compose.onNodeWithText(getString(CommonString.profile_load_failed_message, "Home", "Work")).assertIsDisplayed()
            compose.onNodeWithText(getString(CommonString.profile_load_failed_use_default, "Home")).assertIsDisplayed()
        }
    }

    @Test
    fun `the explanation is shown above the storage warning`() {
        failToOpenProfile()

        deckPicker {
            assertEquals(1, storageFullDialogs.size)
            assertEquals(profileFailureDialogs.single(), ShadowDialog.getLatestDialog())
        }
    }

    @Test
    fun `a startup error waits until OK is pressed`() {
        assertStartupErrorFollows { clickProfileFailureButton(useDefault = false) }
    }

    @Test
    fun `a startup error waits until the explanation is cancelled`() {
        assertStartupErrorFollows { profileFailureDialogs.single().cancel() }
    }

    @Test
    fun `a startup error waits until Default is chosen`() {
        assertStartupErrorFollows { clickProfileFailureButton(useDefault = true) }
    }

    @Test
    fun `an unmounted SD card closes the Deck Picker after the explanation`() {
        failToOpenProfile()

        withStartupFailure(StartupFailure.SDCardNotMounted) {
            deckPicker {
                assertFalse(isFinishing)

                clickProfileFailureButton(useDefault = false)

                assertTrue(isFinishing)
            }
        }
    }

    @Test
    fun `Use Default opens the Default profile from the next start`() {
        failToOpenProfile()

        deckPicker {
            clickProfileFailureButton(useDefault = true)

            assertEquals(ProfileId.DEFAULT.value, registry.getString(PREF_LAST_ACTIVE_PROFILE_ID, null))
        }
    }

    @Test
    fun `Use Default can be tried again when the choice is not saved`() {
        failToOpenProfile()
        mockkObject(ProfileManager.Companion)
        try {
            every { ProfileManager.selectDefaultProfile(any()) } returnsMany listOf(false, true)

            deckPicker {
                clickProfileFailureButton(useDefault = true)
                assertEquals(1, profileFailureDialogs.size)
                assertEquals(getString(CommonString.something_wrong), ShadowToast.getTextOfLatestToast())

                clickProfileFailureButton(useDefault = true)
                assertEquals(0, profileFailureDialogs.size)
            }
        } finally {
            unmockkObject(ProfileManager.Companion)
        }
    }

    @Test
    fun `OK tries the profile again on the next start`() {
        val work = failToOpenProfile()

        deckPicker {
            clickProfileFailureButton(useDefault = false)

            assertEquals(work.value, registry.getString(PREF_LAST_ACTIVE_PROFILE_ID, null))
        }
    }

    @Test
    fun `the explanation survives a configuration change`() {
        failToOpenProfile()
        val controller = startDeckPicker()

        val recreated = controller.recreateDeckPicker()

        assertEquals(1, recreated.profileFailureDialogs.size)
    }

    @Test
    fun `a dismissed explanation does not come back`() {
        failToOpenProfile()
        val controller = startDeckPicker()
        controller.get().clickProfileFailureButton(useDefault = false)

        val recreated = controller.recreateDeckPicker()

        assertEquals(0, recreated.profileFailureDialogs.size)
    }

    @Test
    fun `a closed explanation stays closed when the app returns`() {
        failToOpenProfile()

        withStartupFailure(StartupFailure.DiskFull) {
            val controller = startDeckPicker()
            controller.get().clickProfileFailureButton(useDefault = false)

            controller.leaveAndReturn()

            assertEquals(0, controller.get().profileFailureDialogs.size)
            assertEquals(1, controller.get().storageFullDialogs.size)
        }
    }

    @Test
    fun `the explanation is shown once after the process is restored`() {
        failToOpenProfile()
        val savedState = Bundle()
        startDeckPicker()
            .pause()
            .stop()
            .saveInstanceState(savedState)
            .destroy()
        assertNull(ProfileManager.createOrNull(targetContext))

        val restored =
            Robolectric
                .buildActivity(DeckPicker::class.java, Intent())
                .create(savedState)
                .start()
                .restoreInstanceState(savedState)
                .postCreate(savedState)
                .resume()
                .visible()
        saveControllerForCleanup(restored)
        restored.get().viewModel.awaitStartup()
        advanceRobolectricLooper()

        assertEquals(1, restored.get().profileFailureDialogs.size)
    }

    private fun assertStartupErrorFollows(close: DeckPicker.() -> Unit) {
        failToOpenProfile()

        withStartupFailure(StartupFailure.DiskFull) {
            deckPicker {
                assertEquals(0, storageFullDialogs.size)

                close()
                advanceRobolectricLooper()

                assertEquals(1, storageFullDialogs.size)
            }
        }
    }

    private fun startDeckPicker(): ActivityController<DeckPicker> {
        setIntroductionSlidesShown(true)
        return startActivityControllerNormallyOpenCollectionWithIntent(DeckPicker::class.java, Intent()).also {
            advanceRobolectricLooper()
            assertEquals(1, it.get().profileFailureDialogs.size)
        }
    }

    private fun DeckPicker.clickProfileFailureButton(useDefault: Boolean) {
        val label =
            if (useDefault) {
                getString(CommonString.profile_load_failed_use_default, ProfileManager.DEFAULT_PROFILE_DISPLAY_NAME)
            } else {
                getString(CommonString.dialog_ok)
            }
        compose.onNodeWithText(label).performClick()
        advanceRobolectricLooper()
    }
}

fun RobolectricTest.failToOpenProfile(
    profileName: String = "Work",
    defaultProfileName: String? = null,
): ProfileId {
    val manager = ProfileManager.create(targetContext)
    defaultProfileName?.let { manager.renameProfile(ProfileId.DEFAULT, ProfileName.fromTrustedSource(it)) }
    val profileId = manager.createNewProfile(ProfileName.fromTrustedSource(profileName))
    File(targetContext.filesDir.parentFile, profileId.value).writeText("a file where the profile folder belongs")
    targetContext.getSharedPreferences(PROFILE_REGISTRY_FILENAME, Context.MODE_PRIVATE).edit(commit = true) {
        putString(PREF_LAST_ACTIVE_PROFILE_ID, profileId.value)
    }
    assertNull(ProfileManager.createOrNull(targetContext))
    return profileId
}

fun withStartupFailure(
    failure: StartupFailure,
    block: () -> Unit,
) {
    mockkObject(InitialActivity)
    try {
        every { InitialActivity.getStartupFailureType(any<SharedPreferences>(), any<() -> Boolean>()) } returns failure
        block()
    } finally {
        unmockkObject(InitialActivity)
    }
}

val DeckPicker.profileLoadFailureDialog: ProfileLoadFailureDialog?
    get() = supportFragmentManager.findFragmentByTag(ProfileLoadFailureDialog.TAG) as? ProfileLoadFailureDialog

fun ActivityController<DeckPicker>.leaveAndReturn() {
    pause()
        .stop()
        .start()
        .resume()
        .visible()
    advanceRobolectricLooper()
}

fun ActivityController<DeckPicker>.recreateDeckPicker(): DeckPicker {
    recreate()
    get().viewModel.awaitStartup()
    advanceRobolectricLooper()
    return get()
}
