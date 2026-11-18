// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2021 Akshay Jadhav <jadhavakshay0701@gmail.com>

package com.ichi2.anki.dialogs

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CheckResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.core.text.parseAsHtml
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.ichi2.anki.CardTemplateEditor
import com.ichi2.anki.CommonString
import com.ichi2.anki.Flag
import com.ichi2.anki.R
import com.ichi2.anki.dialogs.InsertFieldDialogViewModel.Companion.ARG_FIELD_NAMES
import com.ichi2.anki.dialogs.InsertFieldDialogViewModel.Companion.ARG_INSERT_FIELD_METADATA
import com.ichi2.anki.dialogs.InsertFieldDialogViewModel.Companion.ARG_REQUEST_KEY
import com.ichi2.anki.dialogs.InsertFieldDialogViewModel.Tab
import com.ichi2.anki.launchCatchingViewTask
import com.ichi2.anki.model.SpecialField
import com.ichi2.anki.model.SpecialFields
import com.ichi2.anki.utils.ext.requireString
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.theme.foundation
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActions
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.DialogTitle
import com.ichi2.themes.Themes
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import androidx.compose.material3.Tab as MaterialTab

/**
 * Dialog fragment used to show the fields that the user can insert in the card editor. This
 * fragment can notify other fragments from the same activity about an inserted field.
 *
 * @see [CardTemplateEditor]
 */
class InsertFieldDialog : DialogFragment() {
    private val viewModel by viewModels<InsertFieldDialogViewModel>()
    private val requestKey: String by lazy {
        requireArguments().requireString(ARG_REQUEST_KEY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.ThemeOverlay_AnkiDroid_AlertDialog_FullScreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        FrameLayout(inflater.context).apply {
            val content =
                ComposeView(context).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent { AnkiDroidTheme { InsertFieldContent() } }
                }
            addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupEdgeToEdge(view)

        // setup flows
        launchCatchingViewTask {
            viewModel.selectedFieldFlow.collect { field ->
                if (field == null) return@collect
                parentFragmentManager.setFragmentResult(
                    requestKey,
                    Bundle().apply { putString(RESULT_INSERTED_FIELD, field.renderToTemplateTag()) },
                )
                dismiss()
            }
        }
    }

    private fun setupEdgeToEdge(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(systemBars() or displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val window = dialog?.window ?: return
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !Themes.isNightTheme
        }
    }

    @Composable
    private fun InsertFieldContent() {
        val colors = MaterialTheme.foundation.colors
        val pager = rememberPagerState(initialPage = viewModel.currentTab.position) { Tab.entries.size }
        val scope = rememberCoroutineScope()
        LaunchedEffect(pager) {
            snapshotFlow { pager.settledPage }.collect { page -> viewModel.currentTab = Tab.entries[page] }
        }

        Surface(Modifier.fillMaxSize(), color = colors.background, contentColor = colors.onBackground) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                DialogTitle(getString(CommonString.card_template_editor_select_field))
                PrimaryTabRow(selectedTabIndex = pager.currentPage, containerColor = colors.background) {
                    Tab.entries.forEach { tab ->
                        MaterialTab(
                            selected = pager.currentPage == tab.position,
                            onClick = { scope.launch { pager.animateScrollToPage(tab.position) } },
                            text = { Text(getString(tab.label)) },
                        )
                    }
                }
                HorizontalPager(pager, modifier = Modifier.weight(1f)) { page ->
                    LazyColumn(Modifier.fillMaxSize()) {
                        when (Tab.entries[page]) {
                            Tab.FIELDS ->
                                items(viewModel.fieldNames) { field ->
                                    DialogChoiceRow(
                                        DialogChoice(field.name),
                                        checked = false,
                                        onClick = { viewModel.selectNamedField(field) },
                                        multiple = false,
                                    )
                                }
                            Tab.SPECIAL ->
                                items(viewModel.specialFields) { field ->
                                    DialogChoiceRow(
                                        DialogChoice(
                                            label = "{{${field.name}}}",
                                            description = field.buildDescription(requireContext(), viewModel.metadata).toString(),
                                        ),
                                        checked = false,
                                        onClick = { viewModel.selectSpecialField(field) },
                                        multiple = false,
                                    )
                                }
                        }
                    }
                }
                DialogActions(listOf(DialogAction(getString(CommonString.dialog_cancel), ::dismiss)))
            }
        }
    }

    companion object {
        /**
         * A key in the extras of the Fragment Result
         *
         * Represents the template tag for the selected field: `{{Front}}`
         */
        const val RESULT_INSERTED_FIELD = "key_inserted_field"

        /**
         * Creates a new instance of [InsertFieldDialog]
         *
         * @param fieldItems The list of field names to be displayed in the dialog.
         * @param requestKey The key used to identify the result when returning the selected field
         *                   to the calling fragment.
         */
        fun newInstance(
            fieldItems: List<String>,
            metadata: InsertFieldMetadata,
            requestKey: String,
        ): InsertFieldDialog =
            InsertFieldDialog().apply {
                arguments =
                    Bundle().apply {
                        putStringArrayList(ARG_FIELD_NAMES, ArrayList(fieldItems))
                        putParcelable(ARG_INSERT_FIELD_METADATA, metadata)
                        putString(ARG_REQUEST_KEY, requestKey)
                    }
            }
    }
}

@VisibleForTesting
@CheckResult
fun SpecialField.buildDescription(
    context: Context,
    metadata: InsertFieldMetadata,
): Spanned {
    fun buildSuffix(value: String?): String {
        if (value == null) return ""
        return context.getString(CommonString.special_field_example_suffix, value)
    }
    return when (this) {
        SpecialFields.FrontSide -> context.getString(CommonString.special_field_front_side_help)
        SpecialFields.Deck ->
            context.getString(CommonString.special_field_deck_help, buildSuffix(metadata.deck))

        SpecialFields.Subdeck ->
            context.getString(CommonString.special_field_subdeck_help, buildSuffix(metadata.subdeck))
        SpecialFields.Flag -> {
            val code = metadata.flag ?: "N"
            context.getString(
                CommonString.special_field_flag_help,
                if (code == "N") "flag$code" else "<b>flag$code</b>",
                "<b>$code</b>",
                Flag.entries.minOf { it.code }.toString(),
                Flag.entries.maxOf { it.code }.toString(),
            )
        }
        SpecialFields.Tags -> {
            val tags = if (metadata.tags.isNullOrBlank()) null else metadata.tags
            context.getString(CommonString.special_field_tags_help, buildSuffix(tags))
        }
        SpecialFields.CardId ->
            context.getString(CommonString.special_field_card_id_help, buildSuffix(metadata.cardId?.toString()))

        SpecialFields.CardTemplate ->
            context.getString(
                CommonString.special_field_card_help,
                buildSuffix(metadata.cardTemplateName),
            )

        SpecialFields.NoteType ->
            context.getString(
                CommonString.special_field_type_help,
                buildSuffix(metadata.noteTypeName),
            )
        // this shouldn't happen
        else -> ""
    }.parseAsHtml(HtmlCompat.FROM_HTML_MODE_LEGACY)
}

private val Tab.label: Int
    get() =
        when (this) {
            Tab.FIELDS -> CommonString.standard_fields_tab_header
            Tab.SPECIAL -> CommonString.special_fields_tab_header
        }
