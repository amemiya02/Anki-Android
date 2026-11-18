// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.noteeditor

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.common.utils.android.getColorFromAttr
import com.ichi2.anki.noteeditor.presentation.ToolbarLayoutMode
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.utils.AndroidUiUtils.showSoftInput
import timber.log.Timber
import com.ichi2.anki.common.android.R as CommonR
import com.ichi2.anki.noteeditor.R as NoteEditorR

/** Hosts the formatting controls beside the native EditTexts that own selection and IME input. */
class Toolbar(
    context: Context,
    attrs: AttributeSet?,
) : FrameLayout(context, attrs) {
    var formatListener: TextFormatListener? = null

    @VisibleForTesting
    internal val scrollView =
        HorizontalScrollView(context).apply {
            id = R.id.toolbar_scrollview
            clipToPadding = false
            isHorizontalScrollBarEnabled = false
        }

    private var actions by mutableStateOf<List<Action>>(emptyList())
    private var layout by mutableStateOf(ToolbarLayoutMode.HIDDEN)
    private val stacked = composeToolbar(stacked = true)
    internal val stackedControlsHeight get() = stacked.height
    private val rulePaint = Paint().apply { color = getColorFromAttr(context, CommonR.attr.foundationOutline) }

    init {
        setBackgroundColor(getColorFromAttr(context, CommonR.attr.foundationBackground))
        setWillNotDraw(false)
        scrollView.addView(
            composeToolbar(stacked = false),
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )
        addView(scrollView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(stacked, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun composeToolbar(stacked: Boolean) =
        ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val visibleLayout = if (stacked) ToolbarLayoutMode.STACK else ToolbarLayoutMode.SCROLL
                if (layout == visibleLayout) {
                    AnkiDroidTheme { FormatActions(actions, stacked) }
                }
            }
        }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        if (!stacked.isVisible || MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.AT_MOST) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        // Keep room for typing above the controls when using Stack with the keyboard
        // or many custom buttons. All rows remain reachable by scrolling vertically.
        val maximumHeight = MeasureSpec.getSize(heightMeasureSpec)
        val availableHeight = (maximumHeight - paddingBottom).coerceAtLeast(0)
        val firstRow = resources.getDimensionPixelSize(R.dimen.note_editor_toolbar_height)
        val controlsHeight = maxOf(firstRow, availableHeight / 3)
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(minOf(maximumHeight, paddingBottom + controlsHeight), MeasureSpec.AT_MOST),
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val hairline = resources.getDimensionPixelSize(CommonR.dimen.foundation_shape_hairline)
        canvas.drawRect(0f, 0f, width.toFloat(), hairline.toFloat(), rulePaint)
    }

    /**
     * The scroll row clips at the pane edge, with space for the display's rounded corners
     * at the first and last resting positions.
     */
    fun setSideClearance(
        left: Int,
        right: Int,
    ) {
        scrollView.updatePadding(left = left, right = right)
        stacked.updatePadding(left = left, right = right)
    }

    internal fun update(
        layout: ToolbarLayoutMode,
        cloze: List<Action>,
        custom: List<Action>,
    ) {
        actions = cloze + defaultActions(separated = cloze.isNotEmpty()) + custom
        this.layout = layout
        scrollView.isVisible = layout == ToolbarLayoutMode.SCROLL
        stacked.isVisible = layout == ToolbarLayoutMode.STACK
        isVisible = layout != ToolbarLayoutMode.HIDDEN
    }

    private fun defaultActions(separated: Boolean): List<Action> =
        listOf(
            Action(
                context.getString(CommonString.format_insert_bold),
                NoteEditorR.drawable.symbol_format_bold,
                shortcut = 'b',
                separated = separated,
                onClick = { format(TextWrapper("<b>", "</b>")) },
            ),
            Action(
                context.getString(CommonString.format_insert_italic),
                NoteEditorR.drawable.symbol_format_italic,
                shortcut = 'i',
                onClick = { format(TextWrapper("<i>", "</i>")) },
            ),
            Action(
                context.getString(CommonString.format_insert_underline),
                NoteEditorR.drawable.symbol_format_underlined,
                shortcut = 'u',
                onClick = { format(TextWrapper("<u>", "</u>")) },
            ),
            Action(
                context.getString(CommonString.insert_mathjax),
                NoteEditorR.drawable.symbol_functions,
                shortcut = 'm',
                onLongClick = ::displayInsertMathJaxEquationsDialog,
                onClick = { format(TextWrapper("\\(", "\\)")) },
            ),
            Action(
                context.getString(CommonString.insert_heading),
                NoteEditorR.drawable.symbol_title,
                shortcut = 'h',
                onClick = ::displayInsertHeadingDialog,
            ),
            Action(
                context.getString(CommonString.format_font_size),
                NoteEditorR.drawable.symbol_format_size,
                shortcut = 'f',
                onClick = ::displayFontSizeDialog,
            ),
            Action(
                context.getString(CommonString.insert_horizontal_line),
                NoteEditorR.drawable.symbol_horizontal_rule,
                shortcut = 'r',
                onClick = { format(TextWrapper("<hr>", "")) },
            ),
        )

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (!event.isCtrlPressed || event.isAltPressed || event.isShiftPressed || event.isMetaPressed) return false
        val shortcut =
            try {
                event.getUnicodeChar(0).toChar()
            } catch (e: Exception) {
                Timber.w(e)
                return false
            }
        val action = actions.firstOrNull { it.shortcut == shortcut } ?: return false
        action.onClick()
        return true
    }

    internal fun format(formatter: TextFormatter) {
        (context as? Activity).showSoftInput()
        onFormat(formatter)
    }

    internal data class Action(
        val label: String,
        @DrawableRes val icon: Int = 0,
        val text: String? = null,
        val shortcut: Char? = null,
        val emphasis: Boolean = false,
        val separated: Boolean = false,
        val onLongClick: (() -> Unit)? = null,
        val onClick: () -> Unit,
    )

    /**
     * Displays a dialog which allows the HTML size codes to be inserted around text (xx-small to xx-large)
     *
     * @see [R.array.html_size_codes]
     */
    private fun displayFontSizeDialog() {
        val results = resources.getStringArray(R.array.html_size_codes)
        showFormattingChoices(CommonString.menu_font_size, resources.getStringArray(R.array.html_size_code_labels).toList()) { index ->
            onFormat(TextWrapper(prefix = "<span style=\"font-size:${results[index]}\">", suffix = "</span>"))
        }
    }

    /** Displays a dialog for inserting an HTML heading. */
    private fun displayInsertHeadingDialog() {
        val headings = listOf("h1", "h2", "h3", "h4", "h5")
        showFormattingChoices(CommonString.insert_heading, headings) { index ->
            val heading = headings[index]
            onFormat(TextWrapper(prefix = "<$heading>", suffix = "</$heading>"))
        }
    }

    /** Displays the available formats for inserting a MathJax equation. */
    private fun displayInsertMathJaxEquationsDialog() {
        val options = MathJaxFormat.entries
        showFormattingChoices(CommonString.insert_mathjax, options.map { it.label() }) { index ->
            onFormat(options[index].toTextWrapper())
        }
    }

    private fun showFormattingChoices(
        title: Int,
        choices: List<String>,
        onSelect: (Int) -> Unit,
    ) {
        createPaperDialog(context) { dialog ->
            SingleChoiceDialog(
                title = context.getString(title),
                choices = choices.map { DialogChoice(it) },
                selectedIndex = null,
                onSelect = { index ->
                    dialog.dismiss()
                    onSelect(index)
                },
                onDismissRequest = dialog::dismiss,
            )
        }.show()
    }

    fun onFormat(formatter: TextFormatter) {
        formatListener?.performFormat(formatter)
    }

    /** @see performFormat */
    fun interface TextFormatListener {
        /**
         * A function which accepts a [TextFormatter] and performs some formatting, handling selection changes
         * In the note editor: this takes the [TextFormatter], determines the correct EditText and selection,
         * applies the [TextFormatter] to the selection, and ensures the selection is valid
         *
         * We use a [TextFormatter] to ensure that the selection is correct after the modification
         */
        fun performFormat(formatter: TextFormatter)
    }

    /**
     * A function which takes and returns a [StringFormat] structure
     * Providing a method of inserting text and knowledge of how the selection should change
     */
    fun interface TextFormatter {
        /**
         * A function which takes and returns a [StringFormat] structure
         * Providing a method of inserting text and knowledge of how the selection should change
         */
        fun format(s: String): StringFormat
    }

    /**
     * A [TextFormatter] which wraps the selected string with [prefix] and [suffix]
     * If there's no selection, the cursor is placed in the middle of the prefix and suffix
     * If text is selected and wrapped, the whole string is selected
     */
    class TextWrapper(
        private val prefix: String,
        private val suffix: String,
    ) : TextFormatter {
        override fun format(s: String): StringFormat =
            StringFormat(result = prefix + s + suffix).apply {
                if (s.isEmpty()) {
                    // if there's no selection: place the cursor between the start and end tag
                    selectionStart = prefix.length
                    selectionEnd = prefix.length
                } else {
                    // otherwise, wrap the newly formatted context
                    selectionStart = 0
                    selectionEnd = result.length
                }
            }
    }

    /**
     * Defines a string insertion, and the selection which should occur once the string is inserted
     *
     * @param result The string which should be inserted
     *
     * @param selectionStart
     * The number of characters inside [result] where the selection should start
     * For example: in {{c1::}}, we should set this to 6, to start after the ::
     *
     * @param selectionEnd
     * The number of character inside [result] where the selection should end
     * If the input was empty, we typically want this between the start and end tags
     * If not, at the end of the string
     */
    data class StringFormat(
        var result: String = "",
        var selectionStart: Int = 0,
        var selectionEnd: Int = 0,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormatActions(
    actions: List<Toolbar.Action>,
    stacked: Boolean,
) {
    if (stacked) {
        FlowRow(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 2.dp)) {
            actions.forEach { FormatAction(it, showGroupRule = false) }
        }
    } else {
        Row(Modifier.padding(vertical = 2.dp)) {
            actions.forEach { FormatAction(it, showGroupRule = it.separated) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormatAction(
    action: Toolbar.Action,
    showGroupRule: Boolean,
) {
    val foundation = MaterialTheme.foundation
    val sideRule =
        if (showGroupRule) {
            Modifier.drawBehind {
                val x = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width
                drawLine(
                    color = foundation.colors.outline,
                    start = Offset(x, 11.dp.toPx()),
                    end = Offset(x, size.height - 11.dp.toPx()),
                    strokeWidth = foundation.shapes.hairline.toPx(),
                )
            }
        } else {
            Modifier
        }
    val contextClick =
        if (action.onLongClick != null) {
            Modifier.pointerInput(action.onLongClick) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            event.changes.first().consume()
                            action.onLongClick()
                        }
                    }
                }
            }
        } else {
            Modifier
        }

    val color = if (action.emphasis) foundation.colors.emphasis else foundation.colors.onBackground
    Box(
        modifier =
            Modifier
                .then(sideRule)
                .then(contextClick)
                .widthIn(min = foundation.spacing.touchTarget, max = 84.dp)
                .heightIn(min = foundation.spacing.touchTarget)
                .combinedClickable(role = Role.Button, onClick = action.onClick, onLongClick = action.onLongClick)
                .semantics { contentDescription = action.label }
                .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (action.text != null) {
            // Cloze marks are icons. Keep them intact at large font sizes and in RTL.
            val style =
                if (action.emphasis) {
                    with(LocalDensity.current) {
                        foundation.type.caption.copy(
                            fontSize = 16.dp.toSp(),
                            lineHeight = 20.dp.toSp(),
                            textDirection = TextDirection.Ltr,
                        )
                    }
                } else {
                    foundation.type.button
                }
            Text(
                action.text,
                modifier = Modifier.clearAndSetSemantics {},
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Icon(
                painterResource(action.icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color,
            )
        }
    }
}
