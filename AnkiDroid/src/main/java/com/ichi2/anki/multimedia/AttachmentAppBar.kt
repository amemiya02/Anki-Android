// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.multimedia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.compose.theme.foundation
import androidx.appcompat.R as AppCompatR
import com.ichi2.anki.common.android.R as CommonR

@Composable
internal fun AttachmentAppBar(
    title: String,
    onBack: () -> Unit,
    onSave: (() -> Unit)? = null,
) {
    val foundation = MaterialTheme.foundation
    Column(Modifier.fillMaxWidth().background(foundation.colors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(foundation.spacing.touchTarget)) {
                Icon(
                    painterResource(CommonR.drawable.symbol_arrow_back),
                    contentDescription = stringResource(AppCompatR.string.abc_action_bar_up_description),
                    tint = foundation.colors.onBackground,
                )
            }
            Text(
                title,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                color = foundation.colors.onBackground,
                style = foundation.type.studyTitle,
            )
            if (onSave != null) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.padding(horizontal = 8.dp).heightIn(min = foundation.spacing.touchTarget),
                    shape = foundation.shapes.button,
                ) {
                    Text(stringResource(CommonString.save), style = foundation.type.button)
                }
            }
        }
        HorizontalDivider(thickness = foundation.shapes.hairline, color = foundation.colors.outline)
    }
}
