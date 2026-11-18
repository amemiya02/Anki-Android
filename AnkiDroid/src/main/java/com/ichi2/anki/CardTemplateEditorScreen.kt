// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.InputType
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.view.ActionMode
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentContainerView
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.notetype.FieldActionRow
import com.ichi2.anki.notetype.NoteTypeSheet
import com.ichi2.anki.notetype.templateFieldPlaceholder
import com.ichi2.anki.previewer.PreviewerTabs
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.compose.theme.foundation
import androidx.appcompat.R as AppCompatR
import com.ichi2.anki.common.android.R as CommonR

internal enum class TemplateEditorAction {
    PREVIEW,
    SAVE,
    ADD,
    RENAME,
    REPOSITION,
    DECK_OVERRIDE,
    BROWSER_APPEARANCE,
    INSERT_FIELD,
    COPY,
    RESTORE,
    DELETE,
}

@Composable
internal fun CardTemplateEditorScreen(
    noteTypeName: String,
    templateNames: List<String>,
    selectedTemplate: Int,
    section: EditorViewType,
    availableFields: List<String>,
    deckOverrideEnabled: Boolean,
    dynamicTemplates: Boolean,
    saving: Boolean,
    splitPane: Boolean,
    panePreferences: SharedPreferences,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onSelectTemplate: (Int) -> Unit,
    onSelectSection: (EditorViewType) -> Unit,
    onInsertField: (String) -> Unit,
    onAction: (TemplateEditorAction) -> Unit,
    onEditorReady: (EditText) -> Unit,
    onTextChanged: (String) -> Unit,
    onInsertFieldRequest: () -> Unit,
    onPreviewReady: (FragmentContainerView) -> Unit,
) {
    val paper = MaterialTheme.foundation
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(
        Modifier.fillMaxSize().background(paper.colors.background).windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val compactHeight = maxHeight < 240.dp
        val combineTabRows = compactHeight && !splitPane && maxWidth >= 560.dp
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp).heightIn(min = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TemplateEditorIcon(
                    CommonR.drawable.symbol_arrow_back,
                    stringResource(AppCompatR.string.abc_action_bar_up_description),
                    onClick = onBack,
                )
                Column(Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(
                        stringResource(R.string.note_type_templates),
                        style = paper.type.studyTitle,
                        color = paper.colors.onBackground,
                        maxLines = if (compactHeight) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!compactHeight) {
                        Text(
                            noteTypeName,
                            style = paper.type.caption,
                            color = paper.colors.onBackgroundMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (!splitPane) {
                    TemplateEditorIcon(R.drawable.symbol_visibility, stringResource(CommonString.card_editor_preview_card), !saving) {
                        onAction(TemplateEditorAction.PREVIEW)
                    }
                }
                TemplateEditorIcon(R.drawable.symbol_check, stringResource(CommonString.save), !saving) {
                    onAction(TemplateEditorAction.SAVE)
                }
                TemplateEditorIcon(
                    CommonR.drawable.symbol_more_vert,
                    stringResource(AppCompatR.string.abc_action_menu_overflow_description),
                    !saving,
                ) { menuOpen = true }
            }
            if (combineTabRows) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        PreviewerTabs(templateNames, selectedTemplate, onSelectTemplate)
                    }
                    Box(Modifier.weight(2f)) {
                        TemplateSections(section, onSelectSection, compactHeight = true)
                    }
                }
            } else {
                PreviewerTabs(templateNames, selectedTemplate, onSelectTemplate)
            }
            val editor: @Composable () -> Unit = {
                Column(Modifier.fillMaxSize()) {
                    if (!combineTabRows) TemplateSections(section, onSelectSection, compactHeight)
                    TemplateCode(
                        section = section,
                        enabled = !saving,
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = paper.spacing.screenMargin),
                        onReady = onEditorReady,
                        onTextChanged = onTextChanged,
                        onInsertFieldRequest = onInsertFieldRequest,
                    )
                    if (section != EditorViewType.STYLING && !compactHeight) {
                        FieldInsertRow(availableFields, onInsertField, onInsertFieldRequest)
                    }
                }
            }
            if (splitPane) {
                TemplateEditorPanes(
                    Modifier.weight(1f).fillMaxWidth(),
                    panePreferences,
                    editor,
                    onPreviewReady,
                )
            } else {
                Box(Modifier.weight(1f).fillMaxWidth()) { editor() }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    if (menuOpen) {
        NoteTypeSheet(
            title = templateNames[selectedTemplate],
            onDismiss = { menuOpen = false },
        ) {
            TemplateActions(
                canReorder = !dynamicTemplates && templateNames.size > 1,
                canDelete = templateNames.size > 1,
                canAdd = !dynamicTemplates,
                canInsert = section != EditorViewType.STYLING,
                deckOverrideEnabled = deckOverrideEnabled,
            ) {
                menuOpen = false
                onAction(it)
            }
        }
    }
}

@Composable
internal fun TemplateEditorIcon(
    @DrawableRes icon: Int,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val paper = MaterialTheme.foundation
    IconButton(onClick, Modifier.size(paper.spacing.touchTarget), enabled = enabled) {
        Icon(
            painterResource(icon),
            contentDescription = label,
            tint = paper.colors.onBackground.copy(alpha = if (enabled) 1f else 0.38f),
        )
    }
}

internal fun templateSectionLabel(
    context: Context,
    section: EditorViewType,
): String =
    with(context) {
        when (section) {
            EditorViewType.FRONT -> TR.sentenceCase.frontTemplate
            EditorViewType.BACK -> TR.sentenceCase.backTemplate
            EditorViewType.STYLING -> TR.cardTemplatesTemplateStyling()
        }
    }

@Composable
private fun TemplateSections(
    section: EditorViewType,
    onSelect: (EditorViewType) -> Unit,
    compactHeight: Boolean,
) {
    val paper = MaterialTheme.foundation
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = paper.spacing.screenMargin, vertical = if (compactHeight) 2.dp else 8.dp)
            .clip(paper.shapes.button)
            .border(paper.shapes.hairline, paper.colors.outline, paper.shapes.button)
            .selectableGroup(),
    ) {
        EditorViewType.entries.forEach { candidate ->
            val selected = candidate == section
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = paper.spacing.touchTarget)
                    .background(if (selected) paper.colors.onBackground else paper.colors.background)
                    .selectable(selected, role = Role.Tab) { onSelect(candidate) }
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    templateSectionLabel(context, candidate),
                    color = if (selected) paper.colors.background else paper.colors.onBackground,
                    style = paper.type.caption,
                    textAlign = TextAlign.Center,
                    maxLines = if (compactHeight) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FieldInsertRow(
    fields: List<String>,
    onInsertField: (String) -> Unit,
    onOpenPicker: () -> Unit,
) {
    val paper = MaterialTheme.foundation
    Column(Modifier.padding(top = 8.dp)) {
        HorizontalDivider(color = paper.colors.outline, thickness = paper.shapes.hairline)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = paper.spacing.screenMargin, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FieldInsertAction(onClick = onOpenPicker, outlined = false) {
                Icon(painterResource(R.drawable.symbol_code), null, Modifier.size(18.dp), tint = paper.colors.onBackgroundMuted)
                Text(
                    stringResource(CommonString.card_template_editor_insert_field),
                    modifier = Modifier.padding(start = 4.dp),
                    style = paper.type.caption,
                    color = paper.colors.onBackgroundMuted,
                )
            }
            fields.forEach { field ->
                FieldInsertAction(onClick = { onInsertField(field) }, outlined = true) {
                    Text(field, style = paper.type.caption, color = paper.colors.onBackground, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun FieldInsertAction(
    onClick: () -> Unit,
    outlined: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    val paper = MaterialTheme.foundation
    val touchTarget = paper.spacing.touchTarget
    val minWidthPx = with(LocalDensity.current) { touchTarget.toPx() }
    var isTouchable by remember { mutableStateOf(true) }
    Row(
        Modifier
            .widthIn(min = touchTarget)
            .heightIn(min = touchTarget)
            .onGloballyPositioned { isTouchable = it.boundsInRoot().width >= minWidthPx - 0.5f }
            .clip(paper.shapes.button)
            .then(if (outlined) Modifier.border(paper.shapes.hairline, paper.colors.outline, paper.shapes.button) else Modifier)
            // At the edge of the row, a clipped chip hints that the row scrolls.
            .then(
                if (isTouchable) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier.clearAndSetSemantics { hideFromAccessibility() }
                },
            ).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun TemplateActions(
    canReorder: Boolean,
    canDelete: Boolean,
    canAdd: Boolean,
    canInsert: Boolean,
    deckOverrideEnabled: Boolean,
    onAction: (TemplateEditorAction) -> Unit,
) {
    if (canAdd) {
        FieldActionRow(stringResource(CommonString.add_card_type), CommonR.drawable.symbol_add) { onAction(TemplateEditorAction.ADD) }
        FieldActionRow(stringResource(CommonString.rename_card_type), R.drawable.symbol_edit) { onAction(TemplateEditorAction.RENAME) }
    }
    if (canReorder) {
        FieldActionRow(stringResource(CommonString.card_template_reposition_template), R.drawable.symbol_swap_horiz) {
            onAction(TemplateEditorAction.REPOSITION)
        }
    }
    if (canAdd) {
        val status =
            if (deckOverrideEnabled) {
                CommonString.card_template_editor_deck_override_on
            } else {
                CommonString.card_template_editor_deck_override_off
            }
        FieldActionRow(stringResource(status), R.drawable.symbol_move_selection_right) { onAction(TemplateEditorAction.DECK_OVERRIDE) }
    }
    val context = LocalContext.current
    FieldActionRow(with(context) { TR.sentenceCase.browserAppearance }, R.drawable.symbol_view_list) {
        onAction(TemplateEditorAction.BROWSER_APPEARANCE)
    }
    if (canInsert) {
        FieldActionRow(stringResource(CommonString.card_template_editor_insert_field), R.drawable.symbol_code) {
            onAction(TemplateEditorAction.INSERT_FIELD)
        }
    }
    FieldActionRow(stringResource(CommonString.copy_as_markdown), R.drawable.symbol_content_copy) { onAction(TemplateEditorAction.COPY) }
    FieldActionRow(
        with(context) { TR.sentenceCase.restoreToDefault },
        R.drawable.symbol_restart_alt,
    ) { onAction(TemplateEditorAction.RESTORE) }
    if (canDelete) {
        FieldActionRow(stringResource(CommonString.delete_card_type), CommonR.drawable.symbol_delete, emphasized = true) {
            onAction(TemplateEditorAction.DELETE)
        }
    }
}

@Composable
private fun TemplateEditorPanes(
    modifier: Modifier,
    panePreferences: SharedPreferences,
    editor: @Composable () -> Unit,
    onPreviewReady: (FragmentContainerView) -> Unit,
) {
    val paper = MaterialTheme.foundation
    BoxWithConstraints(modifier) {
        val legacyStart = panePreferences.getFloat("cardTemplateEditorPaneWeight", 3f)
        val legacyEnd = panePreferences.getFloat("cardTemplatePreviewerPaneWeight", 2f)
        var fraction by remember { mutableFloatStateOf(legacyStart / (legacyStart + legacyEnd)) }
        val paneWidth = (maxWidth - 8.dp).coerceAtLeast(1.dp)
        val density = LocalDensity.current
        val minimum = (240.dp * density.fontScale / paneWidth).coerceIn(0.1f, 0.5f)
        val widthPx = with(density) { paneWidth.toPx() }
        Row(Modifier.fillMaxSize()) {
            val editorWeight = fraction.coerceIn(minimum, 1f - minimum)
            Box(Modifier.weight(editorWeight).fillMaxHeight()) { editor() }
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        reverseDirection = LocalLayoutDirection.current == LayoutDirection.Rtl,
                        state =
                            rememberDraggableState { delta ->
                                fraction = (fraction.coerceIn(minimum, 1f - minimum) + delta / widthPx).coerceIn(minimum, 1f - minimum)
                            },
                        onDragStopped = {
                            panePreferences
                                .edit()
                                .putFloat("cardTemplateEditorPaneWeight", fraction * 5)
                                .putFloat("cardTemplatePreviewerPaneWeight", (1 - fraction) * 5)
                                .apply()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.fillMaxHeight().width(paper.shapes.hairline).background(paper.colors.outline))
            }
            AndroidView(
                factory = { FragmentContainerView(it).apply { id = R.id.fragment_container }.also(onPreviewReady) },
                modifier = Modifier.weight(1f - editorWeight).fillMaxHeight(),
            )
        }
    }
}

@SuppressLint("RtlHardcoded") // Template source and its line numbers are left-to-right.
@Composable
private fun TemplateCode(
    section: EditorViewType,
    enabled: Boolean,
    modifier: Modifier,
    onReady: (EditText) -> Unit,
    onTextChanged: (String) -> Unit,
    onInsertFieldRequest: () -> Unit,
) {
    val paper = MaterialTheme.foundation
    val currentSection by rememberUpdatedState(section)
    val currentInsertRequest by rememberUpdatedState(onInsertFieldRequest)
    val currentTextChanged by rememberUpdatedState(onTextChanged)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TemplateCodeInput(context).apply {
                id = R.id.edit_text
                isSaveEnabled = false // The Activity persists the code and the selection by template and section.
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN or EditorInfo.IME_FLAG_NO_EXTRACT_UI
                typeface = Typeface.MONOSPACE
                gravity = Gravity.TOP or Gravity.LEFT
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                textDirection = View.TEXT_DIRECTION_LTR
                setHorizontallyScrolling(false)
                background = null
                setPadding(dp(42), dp(12), dp(12), dp(12))
                isVerticalScrollBarEnabled = true
                val actions =
                    object : ActionMode.Callback {
                        override fun onCreateActionMode(
                            mode: ActionMode,
                            menu: Menu,
                        ) = true

                        override fun onPrepareActionMode(
                            mode: ActionMode,
                            menu: Menu,
                        ): Boolean {
                            if (currentSection == EditorViewType.STYLING || menu.findItem(INSERT_FIELD) != null) return false
                            menu.add(Menu.FIRST, INSERT_FIELD, 0, context.getString(CommonString.card_template_editor_insert_field))
                            return true
                        }

                        override fun onActionItemClicked(
                            mode: ActionMode,
                            item: MenuItem,
                        ): Boolean {
                            if (item.itemId != INSERT_FIELD) return false
                            mode.finish()
                            currentInsertRequest()
                            return true
                        }

                        override fun onDestroyActionMode(mode: ActionMode) = Unit
                    }
                customInsertionActionModeCallback = actions
                customSelectionActionModeCallback = actions
                doAfterTextChanged {
                    highlightFields()
                    currentTextChanged(text.toString())
                }
                onReady(this)
            }
        },
        update = {
            it.isEnabled = enabled
            it.contentDescription = templateSectionLabel(it.context, section)
            it.textSize = paper.type.body.fontSize.value
            it.setTextColor(paper.colors.onBackground.toArgb())
            it.fieldColor = paper.colors.emphasis.toArgb()
            it.lineColor = paper.colors.onBackgroundMuted.toArgb()
            it.highlightFields()
        },
    )
}

private const val INSERT_FIELD = 1

/** Retains the platform's multiline editor and its IME, selection toolbar and editing shortcuts. */
private class TemplateCodeInput(
    context: Context,
) : EditText(context) {
    private val linePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.RIGHT
        }
    var fieldColor = 0
    var lineColor = 0
    private var gutterSize = 0

    fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    fun highlightFields() {
        val code = text ?: return
        code.getSpans(0, code.length, FieldSpan::class.java).forEach(code::removeSpan)
        templateFieldPlaceholder.findAll(code).forEach { match ->
            code.setSpan(FieldSpan(fieldColor), match.range.first, match.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        linePaint.textSize = textSize * 0.76f
        val digits = (code.count { it == '\n' } + 1).toString().length
        val newWidth = dp(24) + linePaint.measureText("0".repeat(digits.coerceAtLeast(2))).toInt()
        if (gutterSize != newWidth) {
            gutterSize = newWidth
            setPadding(gutterSize, paddingTop, paddingRight, paddingBottom)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val codeLayout = layout ?: return
        val code = text ?: return
        linePaint.color = lineColor
        val firstVisibleLine = codeLayout.getLineForVertical(scrollY)
        val lastVisibleLine = codeLayout.getLineForVertical(scrollY + height)
        val firstOffset = codeLayout.getLineStart(firstVisibleLine)
        val startsInWrappedLine = firstOffset != 0 && code[firstOffset - 1] != '\n'
        var sourceLine = code.subSequence(0, firstOffset).count { it == '\n' } + if (startsInWrappedLine) 2 else 1
        for (line in firstVisibleLine..lastVisibleLine) {
            val start = codeLayout.getLineStart(line)
            if (start == 0 || code[start - 1] == '\n') {
                canvas.drawText(
                    sourceLine.toString(),
                    (gutterSize - dp(12) + scrollX).toFloat(),
                    (extendedPaddingTop + codeLayout.getLineBaseline(line)).toFloat(),
                    linePaint,
                )
                sourceLine++
            }
        }
    }

    private class FieldSpan(
        color: Int,
    ) : ForegroundColorSpan(color)
}
