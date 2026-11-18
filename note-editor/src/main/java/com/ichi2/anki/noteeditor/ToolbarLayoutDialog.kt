// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.noteeditor

import androidx.annotation.StringRes
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CommonString
import com.ichi2.anki.noteeditor.presentation.ToolbarLayoutMode
import com.ichi2.anki.settings.enums.Theme
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.preview.AnkiDroidPreview
import com.ichi2.compose.ui.preview.AnkiDroidThemeProvider
import com.ichi2.anki.common.android.R as CommonR

/** Lets the user choose a scrolling, stacked or hidden [ToolbarLayoutMode] for the note editor. */
@Composable
fun ToolbarLayoutDialog(
    selected: ToolbarLayoutMode,
    onSelect: (ToolbarLayoutMode) -> Unit,
    onDismiss: () -> Unit,
) {
    PaperDialog(
        title = stringResource(CommonString.note_editor_toolbar_layout),
        onDismissRequest = onDismiss,
    ) {
        BoxWithConstraints(Modifier.padding(bottom = 24.dp).fillMaxWidth()) {
            // Use full-width rows when large text would push a choice off-screen.
            val useVerticalList = (maxWidth - 16.dp) / 3 < 72.dp * LocalDensity.current.fontScale
            if (useVerticalList) {
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolbarLayoutMode.entries.forEach { layout ->
                        ToolbarLayoutChoice(
                            layout,
                            selected,
                            onSelect,
                            horizontal = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                Row(Modifier.selectableGroup().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolbarLayoutMode.entries.forEach { layout ->
                        ToolbarLayoutChoice(
                            layout,
                            selected,
                            onSelect,
                            horizontal = false,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

/** Displays one selectable layout as a horizontal row or a vertical card. */
@Composable
private fun ToolbarLayoutChoice(
    layout: ToolbarLayoutMode,
    selected: ToolbarLayoutMode,
    onSelect: (ToolbarLayoutMode) -> Unit,
    horizontal: Boolean,
    modifier: Modifier = Modifier,
) {
    val isSelected = layout == selected
    val selectionBackground =
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isPositioned by remember { mutableStateOf(false) }
    // Scroll to the selected choice if needed, after layout has determined its position.
    LaunchedEffect(isSelected, isPositioned) {
        if (isSelected && isPositioned) bringIntoViewRequester.bringIntoView()
    }
    val choiceModifier =
        modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .onGloballyPositioned { isPositioned = true }
            .clip(MaterialTheme.shapes.small)
            .background(selectionBackground)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = MaterialTheme.shapes.small,
            ).selectable(
                selected = isSelected,
                interactionSource = null,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = { onSelect(layout) },
            ).padding(12.dp)
    if (horizontal) {
        Row(choiceModifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolbarLayoutSample(layout, Modifier.width(72.dp))
            Text(stringResource(layout.label()), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        Column(choiceModifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolbarLayoutSample(layout)
            Text(stringResource(layout.label()), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

/** Display label for a toolbar layout. */
@StringRes
fun ToolbarLayoutMode.label(): Int =
    when (this) {
        ToolbarLayoutMode.SCROLL -> CommonString.note_editor_toolbar_scroll
        ToolbarLayoutMode.STACK -> CommonString.note_editor_toolbar_stack
        ToolbarLayoutMode.HIDDEN -> CommonString.note_editor_toolbar_hidden
    }

/**
 * Visual preview/image:
 * Sample note editor, using a toolbar rendered with [layout].
 */
@Composable
private fun ToolbarLayoutSample(
    layout: ToolbarLayoutMode,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(6.dp)
    val icons =
        listOf(
            R.drawable.symbol_format_bold,
            R.drawable.symbol_format_italic,
            R.drawable.symbol_format_underlined,
            R.drawable.symbol_horizontal_rule,
            R.drawable.symbol_title,
            R.drawable.symbol_format_size,
            R.drawable.symbol_functions,
            R.drawable.symbol_add_box,
        )
    Column(
        modifier
            .fillMaxWidth()
            .height(72.dp)
            .clearAndSetSemantics {}
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, shape),
    ) {
        Column(Modifier.weight(1f).fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(if (layout == ToolbarLayoutMode.HIDDEN) 2 else 1) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Spacer(Modifier.fillMaxWidth(0.7f).height(3.dp).background(colors.outlineVariant))
                    Spacer(Modifier.fillMaxWidth(0.45f).height(3.dp).background(colors.outlineVariant))
                }
            }
        }
        if (layout != ToolbarLayoutMode.HIDDEN) {
            Column(Modifier.fillMaxWidth().background(colors.surfaceContainer.compositeOver(colors.surface)).padding(4.dp)) {
                if (layout == ToolbarLayoutMode.STACK) {
                    icons.chunked(4).asReversed().forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { PreviewIcon(it, Modifier.weight(1f)) }
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        icons.take(3).forEach { PreviewIcon(it, Modifier.weight(1f)) }
                        Icon(
                            painterResource(CommonR.drawable.symbol_chevron_right),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = colors.primary,
                        )
                    }
                }
            }
        }
    }
}

/** Shows a toolbar icon in the layout preview. */
@Composable
private fun PreviewIcon(
    icon: Int,
    modifier: Modifier = Modifier,
) {
    Icon(
        painterResource(icon),
        contentDescription = null,
        modifier = modifier.size(20.dp).padding(2.dp),
        tint = MaterialTheme.colorScheme.onSurface,
    )
}

@Preview(name = "Scroll")
@Composable
private fun ToolbarLayoutDialogPreview(
    @PreviewParameter(AnkiDroidThemeProvider::class) theme: Theme,
) {
    AnkiDroidPreview(theme = theme) {
        ToolbarLayoutDialog(
            selected = ToolbarLayoutMode.SCROLL,
            onSelect = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Stack")
@Composable
private fun ToolbarLayoutDialogStackPreview(
    @PreviewParameter(AnkiDroidThemeProvider::class) theme: Theme,
) {
    AnkiDroidPreview(theme = theme) {
        ToolbarLayoutDialog(
            selected = ToolbarLayoutMode.STACK,
            onSelect = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Hidden")
@Composable
private fun ToolbarLayoutDialogHiddenPreview(
    @PreviewParameter(AnkiDroidThemeProvider::class) theme: Theme,
) {
    AnkiDroidPreview(theme = theme) {
        ToolbarLayoutDialog(
            selected = ToolbarLayoutMode.HIDDEN,
            onSelect = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Large text - short window", widthDp = 320, heightDp = 240, fontScale = 2f)
@Composable
private fun ToolbarLayoutDialogLargeTextPreview(
    @PreviewParameter(AnkiDroidThemeProvider::class) theme: Theme,
) {
    AnkiDroidPreview(theme = theme) {
        ToolbarLayoutDialog(
            selected = ToolbarLayoutMode.HIDDEN,
            onSelect = {},
            onDismiss = {},
        )
    }
}
