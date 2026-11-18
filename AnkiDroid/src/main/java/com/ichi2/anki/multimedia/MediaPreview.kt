// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.multimedia

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ichi2.anki.R
import com.ichi2.compose.theme.foundation

/** Common frame around an image or a playable attachment. */
@Composable
internal fun MediaPreview(
    fileSize: String,
    preview: @Composable () -> Unit,
    controls: @Composable ColumnScope.() -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Column(
        Modifier
            .fillMaxSize()
            .background(
                foundation.colors.background,
            ).padding(horizontal = foundation.spacing.screenMargin, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = MaterialTheme.colorScheme.surface,
            shape = foundation.shapes.button,
            content = preview,
        )
        if (fileSize.isNotEmpty()) {
            Text(fileSize, style = foundation.type.caption, color = foundation.colors.onBackgroundMuted)
        }
        MediaControlsAnchor()
        controls()
    }
}

/** Keeps the activity's error message above the controls. */
@Composable
internal fun MediaControlsAnchor() {
    AndroidView(
        factory = { View(it).apply { id = R.id.multimedia_controls_anchor } },
        modifier = Modifier.fillMaxWidth().height(1.dp),
    )
}
