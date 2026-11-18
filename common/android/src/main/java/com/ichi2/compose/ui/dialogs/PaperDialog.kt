// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.compose.ui.dialogs

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.res.ResourcesCompat
import com.ichi2.anki.common.android.R
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation

enum class DialogActionStyle { Text, Filled, Destructive }

data class DialogAction(
    val label: String,
    val onClick: () -> Unit,
    val style: DialogActionStyle = DialogActionStyle.Text,
    val enabled: Boolean = true,
)

data class DialogMark(
    @DrawableRes val icon: Int,
    val warning: Boolean = false,
)

private val LocalPaperDialogWindow = staticCompositionLocalOf { false }

/**
 * Hosts the same dialog patterns in existing FragmentManager routes and Activity callbacks.
 * The fragment or caller still owns cancellation, restoration and dismissal.
 */
@Suppress("DEPRECATION") // Floating dialog windows resize above the IME on Android 8 and 9.
fun createPaperDialog(
    context: Context,
    onOutsideClick: (() -> Unit)? = null,
    content: @Composable (ComponentDialog) -> Unit,
): ComponentDialog =
    object : ComponentDialog(context) {
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (onOutsideClick != null && event.actionMasked == MotionEvent.ACTION_DOWN) {
                val decor = window?.decorView
                val slop = ViewConfiguration.get(context).scaledWindowTouchSlop
                if (decor != null &&
                    (event.x < -slop || event.y < -slop || event.x > decor.width + slop || event.y > decor.height + slop)
                ) {
                    onOutsideClick()
                    return true
                }
            }
            return super.onTouchEvent(event)
        }
    }.apply {
        val dialog = this
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(paperScrimAmount(context))
            setWindowAnimations(R.style.Animation_AnkiDroidDialog)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        setContentView(
            ComposeView(context).apply {
                id = R.id.paper_dialog_content
                setContent {
                    AnkiDroidTheme {
                        CompositionLocalProvider(LocalPaperDialogWindow provides true) {
                            content(dialog)
                        }
                    }
                }
            },
            android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        window?.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
    }

internal fun paperScrimAmount(context: Context): Float {
    val key = context.obtainStyledAttributes(R.styleable.ComposeTheme)
    return try {
        val amount =
            if (key.getBoolean(R.styleable.ComposeTheme_isLightTheme, true)) {
                R.dimen.foundation_dialog_scrim_light
            } else {
                R.dimen.foundation_dialog_scrim_night
            }
        ResourcesCompat.getFloat(context.resources, amount)
    } finally {
        key.recycle()
    }
}

/** Flat paper frame for any combination of dialog pieces. */
@Composable
fun PaperDialog(
    title: String,
    onDismissRequest: () -> Unit,
    actions: List<DialogAction> = emptyList(),
    mark: DialogMark? = null,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit = {},
) {
    if (LocalPaperDialogWindow.current) {
        Box(Modifier.padding(horizontal = 24.dp, vertical = 16.dp).semantics { dialog() }) {
            PaperDialogCard(title, actions, mark, content)
        }
    } else {
        AnkiDroidDialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
            dimAmount = paperScrimAmount(LocalContext.current),
        ) {
            PaperDialogCard(title, actions, mark, content)
        }
    }
}

@Composable
private fun PaperDialogCard(
    title: String,
    actions: List<DialogAction>,
    mark: DialogMark?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    val border = if (colors.background == Color.Black) colors.onBackgroundMuted else colors.outline
    Surface(
        modifier = Modifier.widthIn(min = 280.dp, max = 560.dp).fillMaxWidth(),
        color = colors.background,
        contentColor = colors.onBackground,
        shape = foundation.shapes.dialog,
        border = BorderStroke(foundation.shapes.hairline, border),
    ) {
        Column(Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp)) {
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (mark != null) PaperDialogMark(mark)
                if (title.isNotEmpty()) {
                    Text(title, Modifier.semantics { heading() }, style = foundation.type.dialogTitle)
                }
                ProvideTextStyle(foundation.type.body.copy(lineHeight = 23.25.sp)) { content() }
            }
            if (actions.isNotEmpty()) {
                DialogActions(actions)
            }
        }
    }
}

@Composable
private fun PaperDialogMark(mark: DialogMark) {
    val foundation = MaterialTheme.foundation
    Box(
        modifier = Modifier.size(44.dp).border(foundation.shapes.hairline, foundation.colors.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(mark.icon),
            contentDescription = null,
            tint = if (mark.warning) foundation.colors.emphasis else foundation.colors.onBackground,
        )
    }
}

@Composable
fun DialogBody(text: String) {
    Text(text)
}

@Composable
fun DialogDetail(text: String) {
    val foundation = MaterialTheme.foundation
    Text(text, style = foundation.type.caption, color = foundation.colors.onBackgroundMuted)
}

@Composable
private fun DialogActions(actions: List<DialogAction>) {
    val focus = remember { FocusRequester() }
    val safeAction =
        if (actions.any { it.style == DialogActionStyle.Destructive }) {
            actions.indexOfFirst { it.style == DialogActionStyle.Text && it.enabled }
        } else {
            -1
        }
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        actions.forEachIndexed { index, action ->
            DialogActionButton(action, if (index == safeAction) Modifier.focusRequester(focus) else Modifier)
        }
    }
    if (safeAction >= 0) LaunchedEffect(focus) { focus.requestFocus() }
}

@Composable
private fun DialogActionButton(
    action: DialogAction,
    modifier: Modifier = Modifier,
) {
    val foundation = MaterialTheme.foundation
    val colors = foundation.colors
    val shape = foundation.shapes.button
    val size = modifier.widthIn(min = 64.dp).heightIn(min = foundation.spacing.touchTarget)
    val style = foundation.type.button.copy(fontSize = 16.sp, lineHeight = 22.4.sp)
    val padding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    when (action.style) {
        DialogActionStyle.Text ->
            TextButton(
                onClick = action.onClick,
                modifier = size,
                enabled = action.enabled,
                shape = shape,
                contentPadding = padding,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.onBackground),
            ) { Text(action.label, style = style) }

        DialogActionStyle.Filled ->
            Button(
                onClick = action.onClick,
                modifier = size,
                enabled = action.enabled,
                shape = shape,
                contentPadding = padding,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
            ) { Text(action.label, style = style) }

        DialogActionStyle.Destructive ->
            OutlinedButton(
                onClick = action.onClick,
                modifier = size,
                enabled = action.enabled,
                shape = shape,
                contentPadding = padding,
                border = BorderStroke(foundation.shapes.hairline, colors.emphasis),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.emphasis),
            ) {
                Icon(
                    painterResource(R.drawable.symbol_warning),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp).size(20.dp),
                )
                Text(action.label, style = style)
            }
    }
}
