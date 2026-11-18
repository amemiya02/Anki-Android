// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.provider.MediaStore
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ichi2.anki.common.destinations.NoteEditorDestination
import com.ichi2.anki.common.destinations.toBundle
import com.ichi2.anki.noteeditor.openNoteEditorWithArgs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class NoteEditorOcclusionActionsTest : RobolectricTest() {
    @get:Rule
    val compose = createEmptyComposeRule()

    private fun openOcclusion() {
        ensureCollectionLoadIsSynchronous()
        val editor = openNoteEditorWithArgs(NoteEditorDestination.AddNote().toBundle())
        editor.setCurrentlySelectedNoteType(requireNotNull(col.notetypes.byName("Image Occlusion")).id)
        compose.onNodeWithText(targetContext.getString(CommonString.paste_from_clipboard)).assertExists()
    }

    private fun cameraButton() = compose.onNodeWithText(targetContext.getString(CommonString.multimedia_editor_image_field_editing_photo))

    @Test
    fun `image occlusion camera button is hidden without a camera app`() {
        openOcclusion()
        cameraButton().assertDoesNotExist()
    }

    @Test
    fun `image occlusion camera button is shown with a camera app`() {
        installCameraApp()
        openOcclusion()
        cameraButton().assertExists()
    }

    private fun installCameraApp() {
        val camera = ComponentName("com.android.camera2", "com.android.camera.CaptureActivity")
        shadowOf(targetContext.packageManager).apply {
            addActivityIfNotPresent(camera)
            addIntentFilterForActivity(camera, IntentFilter(MediaStore.ACTION_IMAGE_CAPTURE).apply { addCategory(Intent.CATEGORY_DEFAULT) })
        }
    }
}
