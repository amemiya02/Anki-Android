// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.compose.theme.foundation
import com.ichi2.anki.common.android.R as CommonR

/**
 * More, the bottom bar's last tab on phones: the drawer's lower group (Settings, Help and
 * Support AnkiDroid), then the Fork's name. Each row opens what the drawer's row opened.
 */
@Composable
fun MoreScreen(
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onSupport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(
            // The name sits at the foot of the page, below the rows when the text is large
            modifier =
                Modifier
                    // hands scrolling to the bottom bar's HideViewOnScrollBehavior
                    .nestedScroll(rememberNestedScrollInteropConnection())
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            MoreRows(onSettings = onSettings, onHelp = onHelp, onSupport = onSupport)
            MoreFooter()
        }
    }
}

@Composable
private fun MoreRows(
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onSupport: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val margin = foundation.spacing.screenMargin
    Column {
        Text(
            text = TR.studyingMore(),
            style = foundation.type.screenTitle,
            color = foundation.colors.onBackground,
            modifier =
                Modifier
                    .padding(start = margin, end = margin, top = 24.dp, bottom = 10.dp)
                    .semantics { heading() },
        )
        Column(modifier = Modifier.padding(start = margin, end = margin, top = 8.dp)) {
            MoreDivider()
            MoreRow(
                icon = R.drawable.symbol_settings,
                title = stringResource(CommonR.string.settings),
                summary = stringResource(CommonR.string.more_settings_summary),
                trailing = CommonR.drawable.symbol_chevron_right,
                onClick = onSettings,
            )
            MoreRow(
                icon = CommonR.drawable.symbol_help,
                title = stringResource(CommonR.string.help),
                summary = stringResource(CommonR.string.more_help_summary),
                trailing = R.drawable.symbol_open_in_new,
                onClick = onHelp,
            )
            MoreRow(
                icon = R.drawable.symbol_volunteer_activism,
                title = stringResource(CommonR.string.help_title_support_ankidroid),
                summary = stringResource(CommonR.string.more_support_summary),
                trailing = R.drawable.symbol_open_in_new,
                onClick = onSupport,
            )
        }
    }
}

@Composable
private fun MoreFooter() {
    val foundation = MaterialTheme.foundation
    val margin = foundation.spacing.screenMargin
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = margin, end = margin, top = 32.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(CommonR.string.fork_name),
            style = foundation.type.rowTitle,
            color = foundation.colors.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(CommonR.string.about_fork_title),
            style = foundation.type.caption,
            color = foundation.colors.onBackgroundMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MoreRow(
    @DrawableRes icon: Int,
    title: String,
    summary: String,
    @DrawableRes trailing: Int,
    onClick: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = foundation.spacing.listRow)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = foundation.colors.onBackground,
            modifier = Modifier.size(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(text = title, style = foundation.type.rowTitle, color = foundation.colors.onBackground)
            Text(text = summary, style = foundation.type.caption, color = foundation.colors.onBackgroundMuted)
        }
        Icon(
            painter = painterResource(trailing),
            contentDescription = null,
            tint = foundation.colors.onBackgroundMuted,
            modifier = Modifier.size(20.dp),
        )
    }
    MoreDivider()
}

@Composable
private fun MoreDivider() {
    HorizontalDivider(
        thickness = MaterialTheme.foundation.shapes.hairline,
        color = MaterialTheme.foundation.colors.outline,
    )
}
