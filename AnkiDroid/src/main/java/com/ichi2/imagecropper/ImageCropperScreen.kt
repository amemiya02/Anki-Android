// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.imagecropper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.canhub.cropper.CropImageView
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.multimedia.AttachmentIconButton
import com.ichi2.anki.multimedia.MediaDoneButton
import com.ichi2.anki.preferences.SettingsPage
import com.ichi2.compose.theme.foundation

@Composable
internal fun ImageCropperScreen(
    preview: CropImageView,
    tooLarge: Boolean,
    canCrop: Boolean,
    onBack: () -> Unit,
    onRotate: () -> Unit,
    onFlipHorizontal: () -> Unit,
    onFlipVertical: () -> Unit,
    onDone: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    var flipMenu by rememberSaveable { mutableStateOf(false) }
    SettingsPage(stringResource(CommonString.crop_button), onBack) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            // Keep the bitmap and interactive crop handles in CanHub's rendering view.
            if (tooLarge) {
                Text(
                    stringResource(CommonString.crop_image_size_notice),
                    style = foundation.type.body,
                    color = foundation.colors.onBackground,
                )
            } else {
                AndroidView(factory = { preview }, modifier = Modifier.fillMaxSize())
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AttachmentIconButton(R.drawable.symbol_rotate_right, CommonString.image_cropper_action_rotate, canCrop, onRotate)
            Box {
                AttachmentIconButton(R.drawable.symbol_flip, CommonString.image_cropper_action_flip, canCrop) { flipMenu = true }
                DropdownMenu(expanded = flipMenu, onDismissRequest = { flipMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(CommonString.image_cropper_action_flip_horizontally)) },
                        onClick = {
                            flipMenu = false
                            onFlipHorizontal()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(CommonString.image_cropper_action_flip_vertically)) },
                        onClick = {
                            flipMenu = false
                            onFlipVertical()
                        },
                    )
                }
            }
        }
        Box(Modifier.padding(bottom = 8.dp)) {
            MediaDoneButton(canCrop, onDone)
        }
    }
}
