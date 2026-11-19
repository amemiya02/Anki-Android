// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.multimedia

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.multimedia.MultimediaBottomSheet.MultimediaAction.OPEN_CAMERA
import com.ichi2.anki.multimedia.MultimediaBottomSheet.MultimediaAction.OPEN_DRAWING
import com.ichi2.anki.multimedia.MultimediaBottomSheet.MultimediaAction.SELECT_AUDIO_FILE
import com.ichi2.anki.multimedia.MultimediaBottomSheet.MultimediaAction.SELECT_AUDIO_RECORDING
import com.ichi2.anki.multimedia.MultimediaBottomSheet.MultimediaAction.SELECT_IMAGE_FILE
import com.ichi2.anki.multimedia.MultimediaBottomSheet.MultimediaAction.SELECT_VIDEO_FILE
import com.ichi2.anki.multimedia.MultimediaUtils.canTakePicture
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation
import com.google.android.material.R as MaterialR
import com.ichi2.anki.common.android.R as CommonR

class MultimediaBottomSheet : BottomSheetDialogFragment() {
    private val viewModel: MultimediaViewModel by activityViewModels()

    val fieldIndex get() = requireArguments().getInt(ARG_FIELD_INDEX)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val field = requireArguments().getString(ARG_FIELD_NAME).orEmpty()
        val hasCamera = requireContext().canTakePicture()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AnkiDroidTheme {
                    AttachmentChoices(field, hasCamera) { action ->
                        viewModel.setMultimediaAction(action)
                        dismiss()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sheet = dialog?.findViewById<View>(MaterialR.id.design_bottom_sheet) ?: return
        sheet.setBackgroundColor(Color.TRANSPARENT)
        BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
    }

    enum class MultimediaAction {
        SELECT_IMAGE_FILE,
        SELECT_AUDIO_FILE,
        OPEN_DRAWING,
        SELECT_AUDIO_RECORDING,
        SELECT_VIDEO_FILE,
        OPEN_CAMERA,
    }

    companion object {
        const val TAG = "MultimediaBottomSheet"
        private const val ARG_FIELD_INDEX = "fieldIndex"
        private const val ARG_FIELD_NAME = "fieldName"

        fun forField(
            index: Int,
            name: String,
        ) = MultimediaBottomSheet().apply {
            arguments =
                Bundle().apply {
                    putInt(ARG_FIELD_INDEX, index)
                    putString(ARG_FIELD_NAME, name)
                }
        }
    }
}

@Composable
private fun AttachmentChoices(
    field: String,
    hasCamera: Boolean,
    onSelect: (MultimediaBottomSheet.MultimediaAction) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(foundation.shapes.button)
                .background(foundation.colors.background)
                .border(foundation.shapes.hairline, foundation.colors.outline, foundation.shapes.button)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = foundation.spacing.screenMargin)
                .padding(bottom = 12.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
                .size(width = 36.dp, height = 4.dp)
                .background(foundation.colors.onBackgroundMuted, foundation.shapes.button),
        )
        Text(
            text = stringResource(R.string.note_editor_attach_to_field, field),
            style = foundation.type.studyTitle,
            color = foundation.colors.onBackground,
            modifier = Modifier.padding(vertical = 12.dp).semantics { heading() },
        )
        HorizontalDivider(thickness = foundation.shapes.hairline, color = foundation.colors.outline)
        AttachmentOption(R.drawable.symbol_image, R.string.note_editor_attach_image, R.string.note_editor_attach_image_hint) {
            onSelect(SELECT_IMAGE_FILE)
        }
        if (hasCamera) {
            AttachmentOption(R.drawable.symbol_photo_camera, R.string.note_editor_attach_camera, R.string.note_editor_attach_camera_hint) {
                onSelect(OPEN_CAMERA)
            }
        }
        AttachmentOption(R.drawable.symbol_draw, CommonString.drawing, R.string.note_editor_attach_drawing_hint) {
            onSelect(OPEN_DRAWING)
        }
        AttachmentOption(CommonR.drawable.symbol_mic, CommonString.multimedia_editor_popup_audio) { onSelect(SELECT_AUDIO_RECORDING) }
        AttachmentOption(R.drawable.symbol_audio_file, R.string.note_editor_attach_audio_file) { onSelect(SELECT_AUDIO_FILE) }
        AttachmentOption(R.drawable.symbol_video_library, R.string.note_editor_attach_video_file) { onSelect(SELECT_VIDEO_FILE) }
    }
}

@Composable
private fun AttachmentOption(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    @StringRes hint: Int? = null,
    onClick: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = 56.dp)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = foundation.colors.onBackgroundMuted,
            modifier = Modifier.size(24.dp),
        )
        if (hint != null && LocalDensity.current.fontScale >= 1.5f) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(label), style = foundation.type.body, color = foundation.colors.onBackground)
                Text(stringResource(hint), style = foundation.type.caption, color = foundation.colors.onBackgroundMuted)
            }
        } else {
            Text(
                text = stringResource(label),
                color = foundation.colors.onBackground,
                style = foundation.type.body,
                modifier = Modifier.weight(1f),
            )
            if (hint != null) {
                Text(
                    text = stringResource(hint),
                    color = foundation.colors.onBackgroundMuted,
                    style = foundation.type.caption,
                    modifier = Modifier.weight(1.4f),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}
