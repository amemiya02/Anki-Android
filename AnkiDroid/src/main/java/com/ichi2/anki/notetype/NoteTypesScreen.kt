// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.notetype

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.os.ConfigurationCompat
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonPlurals
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.compose.theme.foundation
import java.text.NumberFormat
import java.util.Locale
import androidx.appcompat.R as AppCompatR
import com.ichi2.anki.common.android.R as CommonR

@Composable
internal fun NoteTypesScreen(
    state: ManageNoteTypesState,
    searching: Boolean,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onQuery: (String) -> Unit,
    onAdd: () -> Unit,
    onNoteType: (NoteTypeItemState) -> Unit,
    onSelect: (NoteTypeItemState) -> Unit,
    onDeleteSelected: () -> Unit,
) {
    val title = stringResource(R.string.note_types_title)
    val paper = MaterialTheme.foundation
    NoteTypePage {
        NoteTypeBar(onBack) {
            when {
                state.isInMultiSelectMode -> {
                    val count = state.noteTypes.count { it.isSelected }
                    Text(pluralStringResource(CommonPlurals.note_types_selected, count, count), Modifier.weight(1f))
                    NoteTypeIconButton(
                        CommonR.drawable.symbol_delete,
                        stringResource(CommonString.dialog_positive_delete),
                        onDeleteSelected,
                    )
                }
                searching -> {
                    val focus = remember { FocusRequester() }
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onQuery,
                        modifier = Modifier.weight(1f).focusRequester(focus),
                        label = { Text(stringResource(AppCompatR.string.abc_search_hint)) },
                        singleLine = true,
                    )
                    LaunchedEffect(Unit) { focus.requestFocus() }
                    NoteTypeIconButton(CommonR.drawable.symbol_close, stringResource(CommonString.close)) { onQuery("") }
                }
                else -> {
                    Spacer(Modifier.weight(1f))
                    NoteTypeIconButton(CommonR.drawable.symbol_search, stringResource(AppCompatR.string.abc_search_hint), onSearch)
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = paper.spacing.screenMargin,
                        end = paper.spacing.screenMargin,
                        bottom = if (state.isInMultiSelectMode) 16.dp else 84.dp,
                    ),
            ) {
                if (!searching && !state.isInMultiSelectMode) {
                    item {
                        PageTitle(title)
                        Text(
                            stringResource(R.string.note_types_lead),
                            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                            style = paper.type.lead,
                            color = paper.colors.onBackgroundMuted,
                        )
                        NoteTypeDivider()
                    }
                }
                items(state.noteTypes.filter { it.shouldBeDisplayed }, key = { it.id }) { item ->
                    NoteTypeRow(
                        title = item.name,
                        subtitle = { NoteTypeCaption(noteTypeCounts(item.useCount, item.templateCount)) },
                        modifier =
                            Modifier
                                .combinedClickable(
                                    role = Role.Button,
                                    onClick = { onNoteType(item) },
                                    onLongClick = { onSelect(item) },
                                ).semantics { if (state.isInMultiSelectMode) selected = item.isSelected },
                    ) {
                        if (state.isInMultiSelectMode) {
                            Checkbox(checked = item.isSelected, onCheckedChange = null)
                        } else {
                            Icon(
                                painterResource(CommonR.drawable.symbol_chevron_right),
                                contentDescription = null,
                                tint = paper.colors.onBackgroundMuted,
                            )
                        }
                    }
                    NoteTypeDivider()
                }
            }
            if (!state.isInMultiSelectMode && !searching) {
                FloatingActionButton(
                    onClick = onAdd,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = paper.spacing.screenMargin, bottom = 16.dp),
                    shape = paper.shapes.addButton,
                    containerColor = paper.colors.primary,
                    contentColor = paper.colors.onPrimary,
                ) {
                    Icon(painterResource(CommonR.drawable.symbol_add), stringResource(R.string.note_type_add_title))
                }
            }
        }
    }
}

@Composable
internal fun NoteTypeDetailPage(
    item: NoteTypeItemState,
    detail: NoteTypeDetails?,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddField: () -> Unit,
    onField: (NoteFieldRef) -> Unit,
    onMove: (NoteFieldRef, Int) -> Unit,
    onTemplate: (NoteTemplateRow) -> Unit,
) {
    val paper = MaterialTheme.foundation
    val scroll = rememberScrollState()
    var menu by remember { mutableStateOf(false) }
    val fields = detail?.fields.orEmpty()
    val drag = remember(item.id) { FieldDrag() }
    val target by remember(fields, drag, scroll) { derivedStateOf { drag.destination(fields, scroll.value) } }
    NoteTypePage {
        NoteTypeBar(onBack) {
            Spacer(Modifier.weight(1f))
            Box {
                NoteTypeIconButton(
                    CommonR.drawable.symbol_more_vert,
                    stringResource(AppCompatR.string.abc_action_menu_overflow_description),
                ) { menu = true }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(CommonString.rename)) }, onClick = {
                        menu = false
                        onRename()
                    })
                    DropdownMenuItem(text = {
                        Text(
                            stringResource(CommonString.dialog_positive_delete),
                            color = paper.colors.emphasis,
                        )
                    }, onClick = {
                        menu = false
                        onDelete()
                    })
                }
            }
        }
        Column(
            Modifier
                .onGloballyPositioned {
                    drag.viewportTop = it.positionInWindow().y
                    drag.viewportHeight = it.size.height
                }.fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = paper.spacing.screenMargin)
                .padding(bottom = 16.dp),
        ) {
            NoteTypeCaption(stringResource(R.string.note_type_label))
            PageTitle(item.name)
            NoteTypeCaption(noteTypeCounts(item.useCount, detail?.templates?.size ?: item.templateCount))
            if (detail != null) {
                SectionHeading(stringResource(R.string.note_type_fields)) {
                    TextButton(onClick = onAddField, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Icon(painterResource(CommonR.drawable.symbol_add), contentDescription = null, Modifier.size(18.dp))
                        Text(TR.sentenceCase.addField, Modifier.padding(start = 4.dp), style = paper.type.button)
                    }
                }
                NoteTypeDivider()
                fields.forEachIndexed { index, field ->
                    key(field.field) {
                        NoteTypeFieldRow(field, index, fields, target, drag, scroll, onField, onMove)
                    }
                }
                SectionHeading(stringResource(R.string.note_type_templates))
                NoteTypeDivider()
                detail.templates.forEach { template ->
                    NoteTypeRow(
                        title = template.name,
                        leading = { Icon(painterResource(R.drawable.symbol_stacks), contentDescription = null) },
                        subtitle = {
                            if (template.frontFields.isNotEmpty() && template.backFields.isNotEmpty()) {
                                val separator = if (LocalLayoutDirection.current == LayoutDirection.Rtl) " ‹ " else " › "
                                NoteTypeCaption(template.frontFields.joinToString() + separator + template.backFields.joinToString())
                            }
                        },
                        modifier = Modifier.clickable(role = Role.Button, onClick = { onTemplate(template) }),
                    ) {}
                    NoteTypeDivider()
                }
            }
        }
    }
    AutoScrollFields(drag, scroll)
}

@Composable
private fun NoteTypeFieldRow(
    row: NoteFieldRow,
    index: Int,
    fields: List<NoteFieldRow>,
    destination: Int,
    drag: FieldDrag,
    scroll: ScrollState,
    onField: (NoteFieldRef) -> Unit,
    onMove: (NoteFieldRef, Int) -> Unit,
) {
    val paper = MaterialTheme.foundation
    val currentMove by rememberUpdatedState(onMove)
    val currentIndex by rememberUpdatedState(index)
    val currentFields by rememberUpdatedState(fields)
    val label = stringResource(R.string.note_type_field_actions, row.field.name)
    var handleY by remember { mutableFloatStateOf(0f) }
    val dragging = drag.active == index
    val target = !dragging && destination == index
    Column(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { drag.rowHeights[row.field] = it.height }
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer { translationY = if (dragging) drag.translation(scroll.value) else 0f }
            .background(paper.colors.background),
    ) {
        NoteTypeRow(
            title = row.field.name,
            subtitle = {
                val sort = if (row.sortsBrowser) stringResource(R.string.note_type_field_sort_label) else null
                val keyboard = row.keyboard?.let { stringResource(R.string.note_type_field_keyboard_label, it.getDisplayLanguage(it)) }
                val subtitle = listOfNotNull(sort, keyboard).joinToString(" · ")
                if (subtitle.isNotEmpty()) NoteTypeCaption(subtitle)
            },
            leading = {
                Box(
                    Modifier
                        .size(paper.spacing.touchTarget)
                        .onGloballyPositioned { handleY = it.positionInWindow().y }
                        .pointerInput(row.field, drag, scroll) {
                            detectDragGestures(
                                onDragStart = { drag.start(currentIndex, scroll.value, handleY + it.y) },
                                onDragCancel = { drag.stop() },
                                onDragEnd = {
                                    val from = drag.active
                                    val to = drag.destination(currentFields, scroll.value)
                                    drag.stop()
                                    if (from >= 0 && from != to) currentMove(row.field, to)
                                },
                            ) { change, amount ->
                                change.consume()
                                drag.windowY += amount.y
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.symbol_drag_indicator),
                        contentDescription = null,
                        tint = if (dragging || target) paper.colors.emphasis else paper.colors.onBackgroundMuted,
                    )
                }
            },
            modifier = Modifier.clickable(role = Role.Button, onClickLabel = label, onClick = { onField(row.field) }),
        ) {
            Icon(painterResource(CommonR.drawable.symbol_more_vert), contentDescription = null, tint = paper.colors.onBackgroundMuted)
        }
        NoteTypeDivider(if (target) paper.colors.emphasis else paper.colors.outline)
    }
}

private class FieldDrag {
    val rowHeights = mutableStateMapOf<NoteFieldRef, Int>()
    var active by mutableIntStateOf(-1)
        private set
    var windowY by mutableFloatStateOf(0f)
    var viewportTop = 0f
    var viewportHeight = 0
    private var anchorY = 0f
    private var anchorScroll = 0

    fun start(
        index: Int,
        scroll: Int,
        pointerY: Float,
    ) {
        anchorY = pointerY
        anchorScroll = scroll
        windowY = pointerY
        active = index
    }

    fun stop() {
        active = -1
    }

    fun translation(scroll: Int) = windowY - anchorY + scroll - anchorScroll

    fun destination(
        fields: List<NoteFieldRow>,
        scroll: Int,
    ): Int {
        if (active !in fields.indices) return -1
        val anchor = (0 until active).sumOf { rowHeights[fields[it].field] ?: 0 } + (rowHeights[fields[active].field] ?: 0) / 2
        val center = anchor + translation(scroll)
        var end = 0
        for (index in fields.indices) {
            end += rowHeights[fields[index].field] ?: 0
            if (center < end) return index
        }
        return fields.lastIndex
    }
}

@Composable
private fun AutoScrollFields(
    drag: FieldDrag,
    scroll: ScrollState,
) {
    val edge = with(LocalDensity.current) { 48.dp.toPx() }
    LaunchedEffect(drag.active) {
        if (drag.active < 0) return@LaunchedEffect
        while (true) {
            withFrameNanos {
                val y = drag.windowY - drag.viewportTop
                val delta =
                    when {
                        y < edge -> (y - edge).coerceAtLeast(-edge)
                        y > drag.viewportHeight - edge -> (y - drag.viewportHeight + edge).coerceAtMost(edge)
                        else -> 0f
                    }
                if (delta != 0f) scroll.dispatchRawDelta(delta / 4)
            }
        }
    }
}

@Composable
internal fun NoteTypeRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: @Composable () -> Unit = {},
    leading: (@Composable () -> Unit)? = null,
    emphasized: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val paper = MaterialTheme.foundation
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = paper.type.rowTitle, color = if (emphasized) paper.colors.emphasis else paper.colors.onBackground)
            subtitle()
        }
        trailing()
    }
}

@Composable
internal fun NoteTypeDivider(color: Color = MaterialTheme.foundation.colors.outline) {
    HorizontalDivider(thickness = MaterialTheme.foundation.shapes.hairline, color = color)
}

@Composable
internal fun NoteTypeCaption(text: String) {
    val paper = MaterialTheme.foundation
    Text(text, style = paper.type.caption, color = paper.colors.onBackgroundMuted)
}

@Composable
private fun SectionHeading(
    title: String,
    action: @Composable () -> Unit = {},
) {
    val paper = MaterialTheme.foundation
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = paper.spacing.touchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f).semantics { heading() },
            style = paper.type.label,
            color = paper.colors.onBackgroundMuted,
        )
        action()
    }
}

@Composable
private fun PageTitle(title: String) {
    val paper = MaterialTheme.foundation
    Text(title, modifier = Modifier.semantics { heading() }, style = paper.type.pageTitle, color = paper.colors.onBackground)
}

@Composable
private fun NoteTypePage(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.foundation.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize(), content = content)
    }
}

@Composable
private fun NoteTypeBar(
    onBack: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NoteTypeIconButton(CommonR.drawable.symbol_arrow_back, stringResource(AppCompatR.string.abc_action_bar_up_description), onBack)
        content()
    }
}

@Composable
private fun NoteTypeIconButton(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(painterResource(icon), label, tint = MaterialTheme.foundation.colors.onBackground)
    }
}

@Composable
private fun noteTypeCounts(
    notes: Int,
    templates: Int,
): String {
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()
    val number = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val noteCount = pluralStringResource(R.plurals.note_type_note_count, notes, number.format(notes))
    val templateCount = pluralStringResource(R.plurals.note_type_template_count, templates, number.format(templates))
    return "$noteCount · $templateCount"
}
