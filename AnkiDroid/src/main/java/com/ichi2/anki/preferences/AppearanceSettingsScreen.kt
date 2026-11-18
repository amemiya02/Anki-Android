// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.preferences

import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.settings.enums.DayTheme
import com.ichi2.anki.settings.enums.NightTheme
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.theme.toFoundation

internal enum class AppearanceTheme(
    @StringRes val label: Int,
) {
    SYSTEM(R.string.settings_theme_system),
    LIGHT(R.string.settings_theme_light),
    DARK(R.string.settings_theme_dark),
    BLACK(R.string.settings_theme_black),
}

internal data class AppearanceSettingsState(
    val theme: AppearanceTheme,
    val nightTheme: NightTheme,
    val hasBackground: Boolean,
)

@Composable
internal fun AppearanceSettingsScreen(
    state: AppearanceSettingsState,
    searchTarget: String?,
    onBack: (() -> Unit)?,
    onThemeChange: (AppearanceTheme) -> Unit,
    onNightThemeChange: (NightTheme) -> Unit,
    onChooseBackground: () -> Unit,
    onRemoveBackground: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    SettingsScrollPage(stringResource(R.string.settings_appearance), onBack) {
        SettingsSearchTarget(searchTarget == stringResource(R.string.app_theme_key)) { modifier, highlighted ->
            Column(
                if (highlighted) {
                    modifier.border(
                        foundation.shapes.hairline,
                        foundation.colors.emphasis,
                        foundation.shapes.button,
                    )
                } else {
                    modifier
                },
            ) {
                SettingsSectionTitle(stringResource(CommonString.app_theme))
                ThemeTiles(state.theme, onThemeChange)
            }
        }
        Column {
            Text(
                stringResource(R.string.settings_theme_note),
                Modifier.padding(top = 12.dp, bottom = 8.dp),
                style = foundation.type.caption,
                color = foundation.colors.onBackgroundMuted,
            )
            SettingsDivider()
            SettingsSearchTarget(searchTarget == stringResource(R.string.night_theme_key)) { modifier, highlighted ->
                NightThemeRow(state, highlighted, onNightThemeChange, modifier)
            }
            SettingsDivider()
        }
        SettingsSectionTitle(stringResource(R.string.settings_deck_list_background))
        SettingsDivider()
        SettingsSearchTarget(searchTarget == stringResource(R.string.pref_deck_picker_background_key)) { modifier, highlighted ->
            SettingsRow(
                title = stringResource(R.string.settings_choose_image),
                summary = stringResource(R.string.settings_choose_image_summary),
                icon = R.drawable.symbol_image,
                highlighted = highlighted,
                modifier = modifier,
                onClick = onChooseBackground,
                trailing = { SettingsChevron() },
            )
        }
        SettingsSearchTarget(searchTarget == stringResource(R.string.pref_remove_wallpaper_key)) { modifier, highlighted ->
            SettingsRow(
                title = stringResource(R.string.settings_remove_background),
                icon = R.drawable.symbol_hide_image,
                onClick = onRemoveBackground,
                enabled = state.hasBackground,
                highlighted = highlighted,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ThemeTiles(
    selected: AppearanceTheme,
    onThemeChange: (AppearanceTheme) -> Unit,
) {
    val themes = AppearanceTheme.entries
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columnCount = if (maxWidth / LocalDensity.current.fontScale >= 300.dp) themes.size else 2
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            themes.chunked(columnCount).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { theme ->
                        ThemeTile(theme, selected == theme, { onThemeChange(theme) }, Modifier.weight(1f))
                    }
                    repeat(columnCount - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ThemeTile(
    theme: AppearanceTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val foundation = MaterialTheme.foundation
    Box(
        modifier
            .border(
                if (selected) 2.dp else foundation.shapes.hairline,
                if (selected) foundation.colors.emphasis else foundation.colors.outline,
                foundation.shapes.button,
            ).selectable(selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeThumbnail(theme)
            Box(Modifier.heightIn(min = 34.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(theme.label),
                    color = foundation.colors.onBackground,
                    style = foundation.type.caption,
                    fontWeight = if (selected) FontWeight.SemiBold else null,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(foundation.colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.symbol_check), null, Modifier.size(16.dp), tint = foundation.colors.onPrimary)
            }
        }
    }
}

@Composable
private fun ThemeThumbnail(theme: AppearanceTheme) {
    val context = LocalContext.current
    val colors =
        remember(context, theme) {
            val styles =
                when (theme) {
                    AppearanceTheme.SYSTEM -> listOf(DayTheme.LIGHT.styleResId, NightTheme.DARK.styleResId)
                    AppearanceTheme.LIGHT -> listOf(DayTheme.LIGHT.styleResId)
                    AppearanceTheme.DARK -> listOf(NightTheme.DARK.styleResId)
                    AppearanceTheme.BLACK -> listOf(NightTheme.BLACK.styleResId)
                }
            styles.map { ContextThemeWrapper(context, it).toFoundation().colors }
        }
    val lightSide = colors.first()
    val darkSide = colors.last()
    val outline =
        if (theme == AppearanceTheme.DARK || theme == AppearanceTheme.BLACK) {
            darkSide.onBackgroundMuted
        } else {
            lightSide.onBackgroundMuted
        }
    Canvas(Modifier.size(width = 56.dp, height = 84.dp)) {
        val corner = CornerRadius(4.dp.toPx())
        val background =
            if (theme == AppearanceTheme.SYSTEM) {
                Brush.linearGradient(0.5f to lightSide.background, 0.5f to darkSide.background)
            } else {
                Brush.linearGradient(listOf(lightSide.background, lightSide.background))
            }
        drawRoundRect(background, cornerRadius = corner)
        drawRoundRect(outline, cornerRadius = corner, style = Stroke(1.dp.toPx()))

        val detail = if (theme == AppearanceTheme.SYSTEM) darkSide.onBackgroundMuted else lightSide.onBackgroundMuted
        val inset = 8.dp.toPx()
        val lineCorner = CornerRadius(2.dp.toPx())
        drawRoundRect(detail, Offset(inset, 10.dp.toPx()), Size(28.dp.toPx(), 6.dp.toPx()), lineCorner)
        listOf(25, 33, 41).forEachIndexed { index, top ->
            val width = if (index == 2) 24.dp else 39.dp
            drawRoundRect(detail, Offset(inset, top.dp.toPx()), Size(width.toPx(), 3.dp.toPx()), lineCorner)
        }
        drawRoundRect(
            Brush.linearGradient(0.5f to lightSide.primary, 0.5f to darkSide.primary),
            Offset(inset, size.height - 18.dp.toPx()),
            Size(40.dp.toPx(), 8.dp.toPx()),
            lineCorner,
        )
    }
}

@Composable
private fun NightThemeRow(
    state: AppearanceSettingsState,
    highlighted: Boolean,
    onChange: (NightTheme) -> Unit,
    modifier: Modifier,
) {
    val foundation = MaterialTheme.foundation
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (highlighted) {
                    Modifier.border(foundation.shapes.hairline, foundation.colors.emphasis, foundation.shapes.button)
                } else {
                    Modifier
                },
            ).padding(vertical = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val title: @Composable () -> Unit = {
            Text(stringResource(R.string.settings_night_theme), style = foundation.type.rowTitle, color = foundation.colors.onBackground)
        }
        val options: @Composable () -> Unit = {
            NightThemeOptions(state.nightTheme, enabled = state.theme == AppearanceTheme.SYSTEM, onChange = onChange)
        }
        if (maxWidth / LocalDensity.current.fontScale >= 300.dp) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.weight(1f)) { title() }
                options()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                title()
                options()
            }
        }
    }
}

@Composable
private fun NightThemeOptions(
    selected: NightTheme,
    enabled: Boolean,
    onChange: (NightTheme) -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val foreground = if (enabled) foundation.colors.onBackground else foundation.colors.onBackgroundMuted
    Row(
        Modifier
            .selectableGroup()
            .border(foundation.shapes.hairline, foundation.colors.outline, foundation.shapes.button)
            .clip(foundation.shapes.button),
    ) {
        listOf(NightTheme.DARK to R.string.settings_theme_dark, NightTheme.BLACK to R.string.settings_theme_black).forEach { (theme, label) ->
            val current = theme == selected
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    Modifier
                        .heightIn(min = foundation.spacing.touchTarget)
                        .selectable(current, enabled, Role.RadioButton) { onChange(theme) }
                        .drawWithContent {
                            drawContent()
                            if (current) {
                                val thickness = foundation.shapes.hairline.toPx() * 2
                                drawLine(
                                    color = if (enabled) foundation.colors.emphasis else foreground,
                                    start = Offset(0f, size.height - thickness / 2),
                                    end = Offset(size.width, size.height - thickness / 2),
                                    strokeWidth = thickness,
                                )
                            }
                        }.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (current) {
                        Icon(painterResource(R.drawable.symbol_check), null, Modifier.size(16.dp), tint = foreground)
                    }
                    Text(
                        stringResource(label),
                        color = foreground,
                        style = foundation.type.body,
                        fontWeight = if (current) FontWeight.SemiBold else null,
                    )
                }
            }
        }
    }
}
