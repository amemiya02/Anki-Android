// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.noteeditor

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.compose.theme.foundation
import androidx.appcompat.R as AppCompatR
import com.ichi2.anki.common.android.R as CommonR

internal data class NoteEditorMenuAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val checked: Boolean? = null,
)

@Composable
internal fun NoteEditorAppBar(
    adding: Boolean,
    showSave: Boolean,
    showPreview: Boolean,
    menuActions: List<NoteEditorMenuAction>,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onPreview: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(foundation.colors.background)
                .padding(horizontal = 4.dp)
                .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorIconButton(
            icon = if (adding) CommonR.drawable.symbol_close else CommonR.drawable.symbol_arrow_back,
            label = stringResource(if (adding) CommonString.close else AppCompatR.string.abc_action_bar_up_description),
            onClick = onBack,
            tint = foundation.colors.onBackground,
        )
        Text(
            text = stringResource(if (adding) CommonString.menu_add else CommonString.cardeditor_title_edit_card),
            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
            color = foundation.colors.onBackground,
            style = foundation.type.studyTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (showPreview) {
            EditorIconButton(
                icon = R.drawable.symbol_visibility,
                label = stringResource(CommonString.card_editor_preview_card),
                onClick = onPreview,
                tint = foundation.colors.onBackground,
            )
        }
        if (showSave) {
            Box(
                modifier =
                    Modifier
                        .padding(horizontal = 4.dp)
                        .clip(foundation.shapes.button)
                        .background(foundation.colors.primary)
                        .clickable(role = Role.Button, onClick = onSave)
                        .heightIn(min = foundation.spacing.touchTarget)
                        .widthIn(min = foundation.spacing.touchTarget)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(if (adding) CommonString.menu_add else CommonString.save),
                    color = foundation.colors.onPrimary,
                    style = foundation.type.button,
                    maxLines = 1,
                )
            }
        }
        Box {
            EditorIconButton(
                icon = CommonR.drawable.symbol_more_vert,
                label = stringResource(AppCompatR.string.abc_action_menu_overflow_description),
                onClick = { menuOpen = true },
                tint = foundation.colors.onBackground,
            )
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = foundation.colors.background,
                shape = foundation.shapes.button,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(foundation.shapes.hairline, foundation.colors.outline),
            ) {
                menuActions.forEach { action ->
                    DropdownMenuItem(
                        modifier =
                            if (action.checked != null) {
                                Modifier.semantics { selected = action.checked }
                            } else {
                                Modifier
                            },
                        text = {
                            Text(
                                action.label,
                                color = if (action.enabled) foundation.colors.onBackground else foundation.colors.onBackgroundMuted,
                            )
                        },
                        enabled = action.enabled,
                        trailingIcon =
                            if (action.checked == true) {
                                {
                                    Icon(
                                        painterResource(R.drawable.symbol_check),
                                        contentDescription = null,
                                        tint = foundation.colors.emphasis,
                                    )
                                }
                            } else {
                                null
                            },
                        onClick = {
                            menuOpen = false
                            action.onClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun NoteEditorSelectors(
    noteType: String,
    deck: String,
    adding: Boolean,
    isCardDeck: Boolean,
    onNoteTypeClick: () -> Unit,
    onDeckClick: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Column {
        EditorRule()
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            EditorSelector(
                label = TR.notetypesNotetype(),
                value = noteType,
                icon = if (adding) R.drawable.symbol_arrow_drop_down else R.drawable.symbol_swap_horiz,
                onClick = onNoteTypeClick,
                modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 8.dp),
            )
            Box(Modifier.fillMaxHeight().width(foundation.shapes.hairline).background(foundation.colors.outline))
            EditorSelector(
                label = if (isCardDeck) stringResource(CommonString.CardEditorCardDeck).trimEnd(':', '：') else TR.decksDeck(),
                value = deck.replace("::", " › "),
                icon = R.drawable.symbol_arrow_drop_down,
                onClick = onDeckClick,
                modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 12.dp),
            )
        }
        EditorRule()
    }
}

@Composable
private fun EditorSelector(
    label: String,
    value: String,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val foundation = MaterialTheme.foundation
    Row(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick).heightIn(min = 60.dp).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = foundation.type.caption, color = foundation.colors.onBackgroundMuted)
            Text(
                text = value,
                style = foundation.type.button,
                color = foundation.colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(painterResource(icon), contentDescription = null, tint = foundation.colors.onBackgroundMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun NoteEditorFieldHeader(
    label: String,
    expanded: Boolean,
    sticky: Boolean?,
    onExpandToggle: () -> Unit,
    onStickyToggle: () -> Unit,
    onAttach: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (expanded) TR.editingCollapseField() else TR.editingExpandField(),
                        onClick = onExpandToggle,
                    ).heightIn(min = foundation.spacing.touchTarget),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f, fill = false),
                style = foundation.type.caption,
                color = foundation.colors.onBackgroundMuted,
            )
            Icon(
                painterResource(if (expanded) R.drawable.symbol_expand_less else CommonR.drawable.symbol_expand_more),
                contentDescription = null,
                tint = foundation.colors.onBackgroundMuted,
                modifier = Modifier.size(16.dp),
            )
        }
        if (sticky != null) {
            Box(
                modifier =
                    Modifier
                        .size(foundation.spacing.touchTarget)
                        .toggleable(value = sticky, role = Role.Checkbox, onValueChange = { onStickyToggle() }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.symbol_keep),
                    contentDescription = stringResource(CommonString.note_editor_toggle_sticky, label),
                    tint = if (sticky) foundation.colors.emphasis else foundation.colors.onBackgroundMuted,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
        EditorIconButton(
            icon = R.drawable.symbol_attach_file,
            label = stringResource(CommonString.multimedia_editor_attach_mm_content, label),
            onClick = onAttach,
        )
    }
}

@Composable
internal fun NoteEditorFieldFooter(
    focused: Boolean,
    duplicate: Boolean,
    onShowDuplicates: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val strong = focused || duplicate
    HorizontalDivider(
        thickness = if (strong) foundation.shapes.currentTabRule else foundation.shapes.hairline,
        color = if (strong) foundation.colors.emphasis else foundation.colors.onBackgroundMuted,
    )
    if (duplicate) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painterResource(R.drawable.symbol_warning),
                contentDescription = null,
                tint = foundation.colors.emphasis,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.note_editor_duplicate_first_field),
                color = foundation.colors.emphasis,
                style = foundation.type.caption,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            Box(
                modifier =
                    Modifier
                        .weight(1f, fill = false)
                        .clickable(role = Role.Button, onClick = onShowDuplicates)
                        .heightIn(min = foundation.spacing.touchTarget)
                        .padding(vertical = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = TR.editingShowDuplicates(),
                    style = foundation.type.caption.copy(textDecoration = TextDecoration.Underline),
                    color = foundation.colors.emphasis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NoteEditorDetails(
    tags: List<String>,
    templates: List<String>,
    activeTemplate: String?,
    onTagsClick: () -> Unit,
    onCardsClick: () -> Unit,
) {
    val foundation = MaterialTheme.foundation
    Column(Modifier.padding(top = 22.dp)) {
        EditorRule()
        Row(
            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onTagsClick).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(R.drawable.symbol_label), contentDescription = null, tint = foundation.colors.onBackgroundMuted)
            FlowRow(
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(TR.browsingSidebarTags(), color = foundation.colors.onBackgroundMuted, style = foundation.type.caption)
                tags.forEach { tag ->
                    Text(
                        text = tag,
                        style = foundation.type.caption,
                        color = foundation.colors.onBackground,
                        modifier =
                            Modifier
                                .border(foundation.shapes.hairline, foundation.colors.outline, foundation.shapes.button)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Icon(painterResource(CommonR.drawable.symbol_add), contentDescription = null, tint = foundation.colors.onBackgroundMuted)
        }
        EditorRule()
        Row(
            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onCardsClick).heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(painterResource(R.drawable.symbol_cards_star), contentDescription = null, tint = foundation.colors.onBackgroundMuted)
            FlowRow(
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(TR.browsingCards(), color = foundation.colors.onBackgroundMuted, style = foundation.type.caption)
                Text(
                    text =
                        buildAnnotatedString {
                            templates.forEachIndexed { index, name ->
                                if (index > 0) append(", ")
                                if (name == activeTemplate) {
                                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(name) }
                                } else {
                                    append(name)
                                }
                            }
                        },
                    color = foundation.colors.onBackground,
                    style = foundation.type.button,
                )
            }
            Icon(
                painterResource(CommonR.drawable.symbol_chevron_right),
                contentDescription = null,
                tint = foundation.colors.onBackgroundMuted,
            )
        }
        EditorRule()
    }
}

@Composable
private fun EditorRule() {
    val foundation = MaterialTheme.foundation
    HorizontalDivider(thickness = foundation.shapes.hairline, color = foundation.colors.outline)
}

@Composable
private fun EditorIconButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.foundation.colors.onBackgroundMuted,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(MaterialTheme.foundation.spacing.touchTarget)) {
        Icon(painterResource(icon), contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
    }
}
