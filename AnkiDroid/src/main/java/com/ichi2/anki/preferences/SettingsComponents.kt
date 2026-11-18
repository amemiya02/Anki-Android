// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.relocation.BringIntoViewRequester
import androidx.compose.ui.relocation.bringIntoViewRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.ichi2.compose.theme.foundation
import kotlinx.coroutines.delay
import com.ichi2.anki.common.android.R as CommonR

/** Shared settings toolbar, system insets and page margin. Content owns its scrolling. */
@Composable
internal fun SettingsPage(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Column(
        modifier
            .fillMaxSize()
            .background(foundation.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(start = if (onBack == null) foundation.spacing.screenMargin else 8.dp, end = foundation.spacing.screenMargin)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    SettingsDirectionalIcon(
                        CommonR.drawable.symbol_arrow_back,
                        stringResource(androidx.appcompat.R.string.abc_action_bar_up_description),
                    )
                }
            }
            Text(
                text = title,
                style = foundation.type.screenTitle,
                color = foundation.colors.onBackground,
                modifier = Modifier.semantics { heading() },
            )
        }
        Column(Modifier.weight(1f).padding(horizontal = foundation.spacing.screenMargin), content = content)
    }
}

@Composable
internal fun SettingsScrollPage(
    title: String,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsPage(title, onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 20.dp), content = content)
    }
}

@Composable
internal fun SettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    val foundation = MaterialTheme.foundation
    Text(
        title,
        modifier.padding(top = 22.dp, bottom = 6.dp).semantics { heading() },
        style = foundation.type.caption,
        color = foundation.colors.onBackgroundMuted,
    )
}

@Composable
internal fun SettingsDivider() {
    val foundation = MaterialTheme.foundation
    HorizontalDivider(thickness = foundation.shapes.hairline, color = foundation.colors.outline)
}

@Composable
internal fun SettingsRow(
    title: String,
    summary: String? = null,
    value: String? = null,
    @DrawableRes icon: Int? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    SettingsRow(
        AnnotatedString(title),
        summary?.let(::AnnotatedString),
        value?.let(::AnnotatedString),
        icon,
        onClick,
        enabled,
        highlighted,
        modifier,
        selected,
        trailing,
    )
}

@Composable
internal fun SettingsRow(
    title: AnnotatedString,
    summary: AnnotatedString? = null,
    value: AnnotatedString? = null,
    @DrawableRes icon: Int? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    val foundation = MaterialTheme.foundation
    val color = if (enabled) foundation.colors.onBackground else foundation.colors.onBackgroundMuted
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = if (summary != null || value != null) foundation.spacing.listRow else foundation.spacing.settingsRow)
                .then(
                    if (onClick == null) {
                        Modifier
                    } else {
                        Modifier.clickable(
                            role = Role.Button,
                            enabled = enabled,
                            onClick = onClick,
                        )
                    },
                ).then(if (selected) Modifier.semantics { this.selected = true } else Modifier)
                .then(
                    if (highlighted || selected) {
                        Modifier.border(foundation.shapes.hairline, foundation.colors.emphasis, foundation.shapes.button)
                    } else {
                        Modifier
                    },
                ).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = foundation.type.rowTitle, color = color)
            if (summary != null) {
                Text(summary, style = foundation.type.caption, color = foundation.colors.onBackgroundMuted)
            }
            if (value != null) {
                Text(value, style = foundation.type.body, color = foundation.colors.onBackgroundMuted)
            }
        }
        trailing?.invoke()
    }
    SettingsDivider()
}

@Composable
internal fun SettingsChevron() {
    SettingsDirectionalIcon(CommonR.drawable.symbol_chevron_right, size = 20)
}

@Composable
private fun SettingsDirectionalIcon(
    @DrawableRes icon: Int,
    description: String? = null,
    size: Int = 24,
) {
    Icon(
        painterResource(icon),
        contentDescription = description,
        tint = MaterialTheme.foundation.colors.onBackgroundMuted,
        modifier = Modifier.size(size.dp),
    )
}

/** Scrolls a search target into view, then clears its visible mark. */
@Composable
internal fun SettingsSearchTarget(
    isTarget: Boolean,
    content: @Composable (modifier: Modifier, highlighted: Boolean) -> Unit,
) {
    val requester = remember { BringIntoViewRequester() }
    var highlighted by remember(isTarget) { mutableStateOf(isTarget) }
    LaunchedEffect(isTarget) {
        if (isTarget) {
            withFrameNanos { }
            requester.bringIntoView()
            delay(2_000)
            highlighted = false
        }
    }
    content(Modifier.bringIntoViewRequester(requester), highlighted)
}

@Composable
internal fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    summary: String? = null,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.foundation.colors
    SettingsRow(
        title = title,
        summary = summary,
        enabled = enabled,
        highlighted = highlighted,
        modifier = modifier.toggleable(checked, enabled, Role.Switch, onChange),
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = colors.onPrimary,
                        checkedTrackColor = colors.primary,
                        uncheckedThumbColor = colors.onBackgroundMuted,
                        uncheckedTrackColor = Color.Transparent,
                        uncheckedBorderColor = colors.onBackgroundMuted,
                    ),
            )
        },
    )
}
