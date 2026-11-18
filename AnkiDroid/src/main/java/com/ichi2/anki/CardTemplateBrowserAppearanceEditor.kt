// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.CheckResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.dialogs.DiscardChangesDialog
import com.ichi2.anki.libanki.CardTemplate
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.themes.Themes
import org.jetbrains.annotations.Contract
import timber.log.Timber
import androidx.appcompat.R as AppCompatR
import com.ichi2.anki.common.android.R as CommonR

/** Edits the template formats used by the Card Browser's question and answer columns. */
class CardTemplateBrowserAppearanceEditor : AnkiActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (showedActivityFailedScreen(savedInstanceState)) return
        super.onCreate(savedInstanceState)
        if (intent.extras == null) {
            showThemedToast(this, getString(CommonString.something_wrong), true)
            finish()
            return
        }
        setResult(RESULT_CANCELED)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { Themes.isNightTheme })
        setContent {
            AnkiDroidTheme {
                BrowserAppearance(
                    initialQuestion = intent.getStringExtra(EXTRA_QUESTION_FORMAT).orEmpty(),
                    initialAnswer = intent.getStringExtra(EXTRA_ANSWER_FORMAT).orEmpty(),
                    onBack = { hasChanges ->
                        if (hasChanges) DiscardChangesDialog.showDialog(this) { finish() } else finish()
                    },
                    onSave = ::saveAndExit,
                )
            }
        }
    }

    private fun saveAndExit(
        question: String,
        answer: String,
    ) {
        val data =
            Intent().apply {
                putExtra(EXTRA_QUESTION_FORMAT, question)
                putExtra(EXTRA_ANSWER_FORMAT, answer)
            }
        setResult(RESULT_OK, data)
        finish()
    }

    class Result private constructor(
        question: String?,
        answer: String?,
    ) {
        val question: String = question ?: VALUE_USE_DEFAULT
        val answer: String = answer ?: VALUE_USE_DEFAULT

        fun applyTo(template: CardTemplate) {
            template.bqfmt = question
            template.bafmt = answer
        }

        companion object {
            @Contract("null -> null")
            fun fromIntent(intent: Intent?): Result? =
                if (intent == null) {
                    null
                } else {
                    try {
                        val question = intent.getStringExtra(EXTRA_QUESTION_FORMAT)
                        val answer = intent.getStringExtra(EXTRA_ANSWER_FORMAT)
                        Result(question, answer)
                    } catch (e: Exception) {
                        Timber.w(e, "Could not read result from intent")
                        null
                    }
                }
        }
    }

    companion object {
        const val EXTRA_QUESTION_FORMAT = "bqfmt"
        const val EXTRA_ANSWER_FORMAT = "bafmt"

        /** Specified the card browser should use the default template formatter  */
        const val VALUE_USE_DEFAULT = ""

        @CheckResult
        fun getIntentFromTemplate(
            context: Context,
            template: CardTemplate,
        ): Intent = getIntent(context, template.bqfmt, template.bafmt)

        @CheckResult
        fun getIntent(
            context: Context,
            questionFormat: String,
            answerFormat: String,
        ): Intent =
            Intent(context, CardTemplateBrowserAppearanceEditor::class.java).apply {
                putExtra(EXTRA_QUESTION_FORMAT, questionFormat)
                putExtra(EXTRA_ANSWER_FORMAT, answerFormat)
            }
    }
}

@Composable
private fun BrowserAppearance(
    initialQuestion: String,
    initialAnswer: String,
    onBack: (Boolean) -> Unit,
    onSave: (String, String) -> Unit,
) {
    val paper = MaterialTheme.foundation
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val goBack = { changed: Boolean ->
        keyboard?.hide()
        onBack(changed)
    }
    var question by rememberSaveable { mutableStateOf(initialQuestion) }
    var answer by rememberSaveable { mutableStateOf(initialAnswer) }
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    val changed = question != initialQuestion || answer != initialAnswer
    BackHandler(enabled = changed) { goBack(true) }
    Column(
        Modifier
            .fillMaxSize()
            .background(paper.colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TemplateEditorIcon(
                CommonR.drawable.symbol_arrow_back,
                stringResource(AppCompatR.string.abc_action_bar_up_description),
            ) { goBack(changed) }
            Text(
                with(context) { TR.sentenceCase.browserAppearance },
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 4.dp),
                style = paper.type.studyTitle,
                color = paper.colors.onBackground,
            )
            TemplateEditorIcon(R.drawable.symbol_restart_alt, with(context) { TR.sentenceCase.restoreToDefault }) {
                keyboard?.hide()
                confirmRestore = true
            }
            TemplateEditorIcon(R.drawable.symbol_check, stringResource(CommonString.save)) { onSave(question, answer) }
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = paper.spacing.screenMargin, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(CommonString.card_template_browser_appearance_summary),
                style = paper.type.body,
                color = paper.colors.onBackgroundMuted,
            )
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text(stringResource(CommonString.card_template_browser_appearance_question_format)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = paper.type.body.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                minLines = 3,
            )
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text(stringResource(CommonString.card_template_browser_appearance_answer_format)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = paper.type.body.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                minLines = 3,
            )
            Text(
                stringResource(CommonString.card_template_browser_appearance_saving_note),
                style = paper.type.caption,
                color = paper.colors.onBackgroundMuted,
            )
        }
    }
    if (confirmRestore) {
        ConfirmationDialog(
            title = with(context) { TR.sentenceCase.restoreToDefault },
            message = stringResource(CommonString.card_template_browser_appearance_restore_default_dialog),
            confirm =
                DialogAction(stringResource(CommonString.restore), {
                    onSave(
                        CardTemplateBrowserAppearanceEditor.VALUE_USE_DEFAULT,
                        CardTemplateBrowserAppearanceEditor.VALUE_USE_DEFAULT,
                    )
                }),
            onDismissRequest = { confirmRestore = false },
        )
    }
}
