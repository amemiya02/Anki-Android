// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.fragment.app.Fragment
import com.ichi2.anki.CardBrowser
import com.ichi2.anki.CommonString
import com.ichi2.anki.analytics.AnalyticsDialogFragment
import com.ichi2.anki.browser.search.SavedSearch
import com.ichi2.anki.browser.search.SavedSearches
import com.ichi2.anki.browser.search.toMap
import com.ichi2.anki.compat.CompatHelper.Companion.getSerializableCompat
import com.ichi2.anki.dialogs.SavedBrowserSearchesDialogFragment.Companion.RESULT_SAVED_SEARCH
import com.ichi2.anki.dialogs.SavedBrowserSearchesDialogFragment.Companion.TYPE_SEARCH_REMOVED
import com.ichi2.anki.dialogs.SavedBrowserSearchesDialogFragment.Companion.TYPE_SEARCH_SELECTED
import com.ichi2.anki.launchCatchingTask
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import timber.log.Timber
import com.ichi2.anki.common.android.R as CommonR

/**
 * Dialog that displays the user's saved browser searches.
 */
// TODO modify the adapter to allow the user to delete multiple entries without dismissing the
//  dialog(or maybe even add an option to remove all entries directly)
class SavedBrowserSearchesDialogFragment : AnalyticsDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val savedFilters: HashMap<String, String>? =
            requireArguments().getSerializableCompat(ARG_SAVED_FILTERS)
        val data =
            savedFilters
                ?.toList()
                ?.sortedWith { str1: Pair<String, String>, str2: Pair<String, String> ->
                    str1.first.compareTo(str2.first, ignoreCase = true)
                } ?: emptyList()
        return createPaperDialog(requireContext()) {
            PaperDialog(
                title = getString(CommonString.card_browser_list_my_searches_title),
                onDismissRequest = ::dismiss,
                actions = listOf(DialogAction(getString(CommonString.dialog_cancel), ::dismiss)),
            ) {
                data.forEach { (name, query) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                            DialogChoiceRow(DialogChoice(name, query), false, {
                                setSearchResult(TYPE_SEARCH_SELECTED, name)
                                dismiss()
                            }, multiple = false)
                        }
                        IconButton(onClick = { removeSearch(name) }) {
                            Icon(painterResource(CommonR.drawable.symbol_delete), getString(CommonString.dialog_positive_delete))
                        }
                    }
                }
            }
        }
    }

    private fun removeSearch(searchName: String) {
        createPaperDialog(requireContext()) { confirmation ->
            ConfirmationDialog(
                title = "",
                message = getString(CommonString.card_browser_list_my_searches_remove_content, searchName),
                confirm =
                    DialogAction(getString(CommonString.dialog_positive_delete), {
                        setSearchResult(TYPE_SEARCH_REMOVED, searchName)
                        confirmation.dismiss()
                        dismiss()
                    }, DialogActionStyle.Destructive),
                onDismissRequest = confirmation::dismiss,
            )
        }.show()
    }

    private fun setSearchResult(
        action: Int,
        searchName: String,
    ) {
        parentFragmentManager.setFragmentResult(
            REQUEST_SAVED_SEARCH_ACTION,
            Bundle().apply {
                putInt(RESULT_SAVED_SEARCH_ACTION, action)
                putString(RESULT_SAVED_SEARCH, searchName)
            },
        )
    }

    companion object {
        const val TAG: String = "manageSavedSearches"
        const val REQUEST_SAVED_SEARCH_ACTION = "request_saved_search_action"
        const val TYPE_SEARCH_SELECTED = 0
        const val TYPE_SEARCH_REMOVED = 1

        /**
         * The user given name for the saved search that can be used as an identifier.
         */
        const val RESULT_SAVED_SEARCH = "arg_saved_search"

        /**
         * [Int] number identifying the type of action for the saved search specified by
         * [RESULT_SAVED_SEARCH]. Currently there are just two possible values [TYPE_SEARCH_SELECTED]
         * and [TYPE_SEARCH_REMOVED].
         */
        const val RESULT_SAVED_SEARCH_ACTION = "arg_type"
        private const val ARG_SAVED_FILTERS = "arg_saved_filters"

        fun newInstance(savedFilters: List<SavedSearch>): SavedBrowserSearchesDialogFragment =
            SavedBrowserSearchesDialogFragment().apply {
                arguments =
                    Bundle().also {
                        it.putSerializable(ARG_SAVED_FILTERS, HashMap(savedFilters.toMap()))
                    }
            }
    }
}

/**
 * Registers a fragment result listener to notify [CardBrowser] about user actions on a saved search.
 * @param action a lambda with the type of action and the name of the target saved search
 */
fun CardBrowser.registerSavedSearchActionHandler(action: (Int, String?) -> Unit) {
    supportFragmentManager.setFragmentResultListener(
        SavedBrowserSearchesDialogFragment.REQUEST_SAVED_SEARCH_ACTION,
        this,
    ) { _, bundle ->
        val type = bundle.getInt(SavedBrowserSearchesDialogFragment.RESULT_SAVED_SEARCH_ACTION)
        val searchName = bundle.getString(RESULT_SAVED_SEARCH)
        Timber.d("On user saved search selection named: %s", searchName)
        action(type, searchName)
    }
}

/**
 * Registers a fragment result listener to notify [CardBrowser] about user actions on a saved search.
 * @param action a lambda with the type of action and the name of the target saved search
 */
fun Fragment.registerSavedSearchActionHandler(action: (ManageSavedSearchAction) -> Unit) {
    childFragmentManager.setFragmentResultListener(
        SavedBrowserSearchesDialogFragment.REQUEST_SAVED_SEARCH_ACTION,
        this,
    ) { _, bundle ->
        val type = bundle.getInt(SavedBrowserSearchesDialogFragment.RESULT_SAVED_SEARCH_ACTION)
        val searchName = bundle.getString(SavedBrowserSearchesDialogFragment.RESULT_SAVED_SEARCH) ?: return@setFragmentResultListener
        Timber.d("On user saved search selection named: %s", searchName)

        launchCatchingTask {
            val search = SavedSearches.byName(searchName) ?: return@launchCatchingTask

            val searchAction =
                when (type) {
                    TYPE_SEARCH_SELECTED -> ManageSavedSearchAction.SelectSearch(search)
                    TYPE_SEARCH_REMOVED -> ManageSavedSearchAction.Delete(search)
                    else -> {
                        Timber.w("unhandled code %d", type)
                        return@launchCatchingTask
                    }
                }

            action(searchAction)
        }
    }
}

sealed class ManageSavedSearchAction {
    data class SelectSearch(
        val search: SavedSearch,
    ) : ManageSavedSearchAction()

    data class Delete(
        val search: SavedSearch,
    ) : ManageSavedSearchAction()
}
