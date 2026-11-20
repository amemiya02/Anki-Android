// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.multimedia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.anki.libanki.Decks
import com.ichi2.anki.ui.windows.reviewer.DeckPath
import com.ichi2.compose.theme.foundation
import androidx.appcompat.R as AppCompatR
import com.ichi2.anki.common.android.R as CommonR

@Composable
internal fun AttachmentAppBar(
    title: String,
    onBack: (() -> Unit)?,
    onSave: (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    // the title names a Deck: its path is split into levels, as the study bar shows it
    titleIsDeckName: Boolean = false,
    actions: @Composable () -> Unit = {},
) {
    val foundation = MaterialTheme.foundation
    Column(Modifier.fillMaxWidth().background(foundation.colors.background)) {
        Row(
            // without a back button, the title lines up with the screen margin
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(start = if (onBack == null) foundation.spacing.screenMargin - 8.dp else 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(foundation.spacing.touchTarget)) {
                    Icon(
                        painterResource(CommonR.drawable.symbol_arrow_back),
                        contentDescription = stringResource(AppCompatR.string.abc_action_bar_up_description),
                        tint = foundation.colors.onBackground,
                        modifier =
                            Modifier.scale(
                                scaleX = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f,
                                scaleY = 1f,
                            ),
                    )
                }
            }
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .then(
                            if (onTitleClick ==
                                null
                            ) {
                                Modifier
                            } else {
                                Modifier.clickable(onClick = onTitleClick).heightIn(min = foundation.spacing.touchTarget)
                            },
                        ).padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (titleIsDeckName) {
                    DeckPath(Decks.path(title), modifier = Modifier.weight(1f), maxLines = DECK_TITLE_LINES)
                } else {
                    Text(title, modifier = Modifier.weight(1f), color = foundation.colors.onBackground, style = foundation.type.studyTitle)
                }
                if (onTitleClick != null) {
                    Icon(painterResource(com.ichi2.anki.R.drawable.symbol_arrow_drop_down), null, tint = foundation.colors.onBackground)
                }
            }
            actions()
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

/** A Deck path in the title wraps once between its levels, then gives way to its last level */
private const val DECK_TITLE_LINES = 2
