// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.export

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import anki.cards.cardIds
import anki.generic.Empty
import anki.import_export.ExportLimit
import anki.import_export.exportLimit
import anki.notes.noteIds
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CollectionManager.withCol
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.anki.browser.IdsFile
import com.ichi2.anki.browser.removeSafely
import com.ichi2.anki.common.ALL_DECKS_ID
import com.ichi2.anki.common.time.TimeManager
import com.ichi2.anki.common.time.getTimestamp
import com.ichi2.anki.common.utils.android.showThemedToast
import com.ichi2.anki.compat.CompatHelper.Companion.getSerializableCompat
import com.ichi2.anki.dialogs.DeckSelectionDialog
import com.ichi2.anki.dialogs.registerChildDeckSelectedHandler
import com.ichi2.anki.dialogs.startDeckSelection
import com.ichi2.anki.exportApkgPackage
import com.ichi2.anki.exportCollectionPackage
import com.ichi2.anki.exportSelectedCards
import com.ichi2.anki.exportSelectedNotes
import com.ichi2.anki.libanki.DeckId
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.requireAnkiActivity
import com.ichi2.anki.ui.internationalization.sentenceCase
import com.ichi2.anki.utils.ext.getLongOrNull
import com.ichi2.anki.utils.ext.requireParcelable
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.DialogDetail
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.SingleChoiceDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Shows the possible options for exporting(collection, decks or notes/card selection).
 * Intended to replicate the desktop UI.
 */
class ExportDialogFragment : AnalyticsDialogFragment() {
    private var selectedDeck by mutableStateOf<SelectableDeck?>(null)

    private var deckSelection: Job? = null

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // onDismiss is also called on a configuration change (from onDestroyView), in which case
        // the dialog is recreated with the same arguments and still needs the ids file
        if (activity?.isChangingConfigurations == true) return
        if (arguments?.containsKey(ARG_IDS_FILE) == true) {
            removeIdsFile()
        }
    }

    private val hasSelection: Boolean get() = arguments?.containsKey(ARG_TYPE) == true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val requestedDeck = arguments?.getLongOrNull(ARG_DECK_ID)
        if (hasSelection) return createPaperDialog(requireContext()) { ExportContent(ExportConfiguration.Apkg) }
        val initialFormat = if (requestedDeck == null) ExportConfiguration.Collection else ExportConfiguration.Apkg
        registerChildDeckSelectedHandler(REQUEST_EXPORT_DECK) { deck -> if (deck != null) selectedDeck = deck }
        lifecycleScope.launch {
            val deckId = savedInstanceState?.getLongOrNull(STATE_SELECTED_DECK_ID) ?: requestedDeck
            selectedDeck =
                withCol { decks.get(deckId, default = false) }?.let { SelectableDeck.Deck(it.id, it.name) } ?: SelectableDeck.AllDecks
        }
        return createPaperDialog(requireContext()) { ExportContent(initialFormat) }
    }

    @Composable
    private fun ExportContent(initialFormat: ExportConfiguration) {
        var index by rememberSaveable { mutableIntStateOf(initialFormat.index) }
        var choosingFormat by rememberSaveable { mutableStateOf(false) }
        BackHandler(choosingFormat) { choosingFormat = false }
        var selections by rememberSaveable { mutableStateOf(ExportConfiguration.entries.map { it.defaultSelection }.toIntArray()) }
        val formats =
            listOf(
                "${TR.sentenceCase.ankiCollectionPackage} (.colpkg)",
                "${TR.sentenceCase.ankiDeckPackage} (.apkg)",
                "${TR.sentenceCase.notesInPlainText} (.txt)",
                "${TR.sentenceCase.cardsInPlainText} (.txt)",
            )
        val format = ExportConfiguration.from(index)
        val exportType = HtmlCompat.fromHtml(TR.exportingExportFormat(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        val options = format.options
        val selection = selections[index]
        val deck = selectedDeck
        val heading =
            if (format == ExportConfiguration.Apkg &&
                deck is SelectableDeck.Deck
            ) {
                getString(CommonString.paper_dialog_export_deck_title, deck.getFullDisplayName(requireContext()))
            } else {
                TR.actionsExport()
            }
        if (choosingFormat) {
            SingleChoiceDialog(
                title = exportType,
                choices = formats.map { DialogChoice(it) },
                selectedIndex = index,
                onSelect = {
                    index = it
                    choosingFormat = false
                },
                onDismissRequest = { choosingFormat = false },
            )
            return
        }
        PaperDialog(
            title = heading,
            onDismissRequest = ::dismiss,
            actions =
                listOf(
                    DialogAction(getString(CommonString.dialog_cancel), ::dismiss),
                    DialogAction(TR.actionsExport(), {
                        when (format) {
                            ExportConfiguration.Collection ->
                                handleCollectionExport(
                                    ExportOption.Media in selection,
                                    ExportOption.Legacy in selection,
                                )
                            ExportConfiguration.Apkg ->
                                withExportLimit {
                                    handleAnkiPackageExport(
                                        it,
                                        ExportOption.Schedule in selection,
                                        ExportOption.DeckConfigs in selection,
                                        ExportOption.Media in selection,
                                        ExportOption.Legacy in selection,
                                    )
                                }
                            ExportConfiguration.Notes ->
                                withExportLimit {
                                    handleNotesInPlainTextExport(
                                        it,
                                        ExportOption.Html in selection,
                                        ExportOption.Tags in selection,
                                        ExportOption.Deck in selection,
                                        ExportOption.Notetype in selection,
                                        ExportOption.Guid in selection,
                                    )
                                }
                            ExportConfiguration.Cards ->
                                withExportLimit {
                                    handleCardsInPlainTextExport(
                                        it,
                                        ExportOption.Html in selection,
                                    )
                                }
                        }
                        dismiss()
                    }, DialogActionStyle.Filled, enabled = hasSelection || format == ExportConfiguration.Collection || deck != null),
                ),
        ) {
            ExportSelector(exportType, formats[index]) { choosingFormat = true }
            if (format != ExportConfiguration.Collection && hasSelection) {
                DialogDetail(TR.exportingSelectedNotes())
            } else if (format != ExportConfiguration.Collection) {
                val include = HtmlCompat.fromHtml(TR.exportingInclude(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                ExportSelector(
                    include,
                    deck?.getFullDisplayName(requireContext()).orEmpty(),
                    enabled = deck != null,
                    onClick = ::showDeckListSelection,
                )
            }
            options.forEach { option ->
                DialogChoiceRow(
                    DialogChoice(option.label()),
                    checked = option in selection,
                    onClick = { selections = selections.copyOf().also { it[index] = selection xor option.bit } },
                    multiple = true,
                )
            }
            when (format) {
                ExportConfiguration.Collection -> DialogDetail(getString(CommonString.export_collection_replaces))
                ExportConfiguration.Apkg -> DialogDetail(getString(CommonString.export_deck_merges))
                else -> Unit
            }
        }
    }

    private fun showDeckListSelection() {
        childFragmentManager.executePendingTransactions()
        if (deckSelection?.isActive == true || childFragmentManager.findFragmentByTag(DeckSelectionDialog.TAG) != null) return
        deckSelection = startDeckSelection(asChild = true, requestKey = REQUEST_EXPORT_DECK)
    }

    @Composable
    private fun ExportSelector(
        label: String,
        selection: String,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Column(Modifier.weight(1f)) {
                DialogDetail(label)
                Text(selection)
            }
            Icon(painterResource(R.drawable.symbol_arrow_drop_down), null)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedDeck?.let { outState.putLong(STATE_SELECTED_DECK_ID, (it as? SelectableDeck.Deck)?.deckId ?: ALL_DECKS_ID) }
    }

    private fun handleCollectionExport(
        includeMedia: Boolean,
        legacy: Boolean,
    ) {
        val exportPath =
            File(
                getExportRootFile(),
                "${TR.exportingCollection()}-${getTimestamp(TimeManager.time)}.colpkg",
            ).path
        requireAnkiActivity().exportCollectionPackage(exportPath, includeMedia, legacy)
    }

    private fun withExportLimit(export: (ExportLimit) -> Unit) {
        val exportLimit =
            try {
                buildExportLimit()
            } catch (e: IOException) {
                Timber.w(e, "Failed to read export IDs")
                null
            }
        if (exportLimit == null) {
            showThemedToast(requireContext(), CommonString.something_wrong, false)
            return
        }
        export(exportLimit)
    }

    private fun handleAnkiPackageExport(
        exportLimit: ExportLimit,
        includeSchedule: Boolean,
        includeDeckConfigs: Boolean,
        includeMedia: Boolean,
        legacy: Boolean,
    ) {
        val exportPath =
            File(
                getExportRootFile(),
                "${getNonCollectionNamePrefix()}-${getTimestamp(TimeManager.time)}.apkg",
            ).path
        requireAnkiActivity().exportApkgPackage(
            exportPath = exportPath,
            withScheduling = includeSchedule,
            withDeckConfigs = includeDeckConfigs,
            withMedia = includeMedia,
            limit = exportLimit,
            legacy = legacy,
        )
    }

    /**
     * Builds the prefix for the name of the exported file. This will be  either a deck's name or a
     * localized "SelectedNotes" text.
     */
    // TODO: return [Filename] once the construction of export paths is refactored
    private fun getNonCollectionNamePrefix(): String {
        val filename =
            Filename.sanitize(
                when (arguments?.getSerializableCompat<ExportType>(ARG_TYPE)) {
                    ExportType.Notes, ExportType.Cards -> TR.exportingSelectedNotes()
                    // notes/cards weren't selected so export the chosen deck(s)
                    null -> requireNotNull(selectedDeck).getFullDisplayName(requireContext())
                },
            )
        return filename.value
    }

    private fun handleNotesInPlainTextExport(
        exportLimit: ExportLimit,
        includeHtml: Boolean,
        includeTags: Boolean,
        includeDeck: Boolean,
        includeNotetype: Boolean,
        includeGuid: Boolean,
    ) {
        val exportPath =
            File(
                getExportRootFile(),
                "${getNonCollectionNamePrefix()}-${getTimestamp(TimeManager.time)}.txt",
            ).path
        requireAnkiActivity().exportSelectedNotes(
            exportPath = exportPath,
            withHtml = includeHtml,
            withTags = includeTags,
            withDeck = includeDeck,
            withNotetype = includeNotetype,
            withGuid = includeGuid,
            limit = exportLimit,
        )
    }

    private fun handleCardsInPlainTextExport(
        exportLimit: ExportLimit,
        includeHtml: Boolean,
    ) {
        val exportPath =
            File(
                getExportRootFile(),
                "${getNonCollectionNamePrefix()}-${getTimestamp(TimeManager.time)}.txt",
            ).path
        requireAnkiActivity().exportSelectedCards(
            exportPath = exportPath,
            withHtml = includeHtml,
            limit = exportLimit,
        )
    }

    /**
     * Builds the [ExportLimit] to be used when exporting. This will either restrict the export to
     * the selected notes/cards or, if those are not present, to the selected
     * deck(or all decks).
     *
     * @return an [ExportLimit] with the export constraints, or null if the selection is unavailable
     */
    private fun buildExportLimit(): ExportLimit? {
        return when (arguments?.getSerializableCompat<ExportType>(ARG_TYPE)) {
            ExportType.Notes -> {
                val ids = requireArguments().requireParcelable<IdsFile>(ARG_IDS_FILE).getIdsOrNull() ?: return null

                exportLimit { noteIds = noteIds { this.noteIds.addAll(ids) } }
            }

            ExportType.Cards -> {
                val ids = requireArguments().requireParcelable<IdsFile>(ARG_IDS_FILE).getIdsOrNull() ?: return null

                exportLimit { cardIds = cardIds { this.cids.addAll(ids) } }
            }
            // notes/cards weren't selected so export the chosen decks
            null ->
                when (val deck = requireNotNull(selectedDeck)) {
                    SelectableDeck.AllDecks -> exportLimit { this.wholeCollection = Empty.getDefaultInstance() }
                    is SelectableDeck.Deck -> exportLimit { this.deckId = deck.deckId }
                }
        }
    }

    /** Attempt to delete the associated [IdsFile] and logs the result */
    private fun removeIdsFile() {
        val idsFile = requireArguments().requireParcelable<IdsFile>(ARG_IDS_FILE)

        idsFile.removeSafely("ExportDialogFragment")
    }

    private fun getExportRootFile() =
        File(requireActivity().externalCacheDir, "export").also {
            it.mkdirs()
        }

    /**
     * Holds information about the type of export.
     *
     * @param index the order of this export type in the list of possible options
     */
    private enum class ExportConfiguration(
        val index: Int,
        val options: List<ExportOption>,
    ) {
        Collection(0, listOf(ExportOption.Media, ExportOption.Legacy)),
        Apkg(1, listOf(ExportOption.Schedule, ExportOption.Media, ExportOption.DeckConfigs, ExportOption.Legacy)),
        Notes(
            2,
            listOf(ExportOption.Html, ExportOption.Tags, ExportOption.Deck, ExportOption.Notetype, ExportOption.Guid),
        ),
        Cards(3, listOf(ExportOption.Html)),
        ;

        val defaultSelection: Int
            get() = options.filter { it.checkedByDefault }.sumOf { it.bit }

        companion object {
            fun from(index: Int) = entries.first { it.index == index }
        }
    }

    private enum class ExportOption(
        val checkedByDefault: Boolean,
        val label: () -> String,
    ) {
        Media(true, { TR.exportingIncludeMedia() }),
        Legacy(false, { TR.exportingSupportOlderAnkiVersions() }),
        Schedule(true, { TR.exportingIncludeSchedulingInformation() }),
        DeckConfigs(false, { TR.exportingIncludeDeckConfigs() }),
        Html(true, { TR.exportingIncludeHtmlAndMediaReferences() }),
        Tags(true, { TR.exportingIncludeTags() }),
        Deck(false, { TR.exportingIncludeDeck() }),
        Notetype(false, { TR.exportingIncludeNotetype() }),
        Guid(false, { TR.exportingIncludeGuid() }),
        ;

        val bit get() = 1 shl ordinal
    }

    private operator fun Int.contains(option: ExportOption) = this and option.bit != 0

    /**
     * Identifier for the list of ids that can be passed to [ExportDialogFragment]. Currently either
     * notes or cards.
     */
    enum class ExportType {
        Notes,
        Cards,
    }

    companion object {
        private const val ARG_DECK_ID = "arg_deck_id"
        private const val ARG_TYPE = "arg_type"
        private const val ARG_IDS_FILE = "arg_ids_file"
        private const val REQUEST_EXPORT_DECK = "request_export_deck"
        private const val STATE_SELECTED_DECK_ID = "selected_deck_id"

        fun forDeckList(deckId: DeckId? = null): ExportDialogFragment =
            ExportDialogFragment().apply {
                arguments =
                    Bundle().apply {
                        if (deckId != null) putLong(ARG_DECK_ID, deckId)
                    }
            }

        /**
         * Create a new instance of this dialog targeting a selection of cards or notes for export.
         */
        fun newInstance(
            idsFileDirectory: File,
            type: ExportType,
            ids: List<Long>,
        ) = ExportDialogFragment().apply {
            val idsFile = IdsFile(idsFileDirectory, ids, IdsFile.Purpose.EXPORT)

            arguments =
                Bundle().apply {
                    putSerializable(ARG_TYPE, type)
                    putParcelable(ARG_IDS_FILE, idsFile)
                }
        }
    }
}
