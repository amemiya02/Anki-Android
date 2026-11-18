// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.browser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.ichi2.anki.R
import com.ichi2.anki.model.CardsOrNotes
import com.ichi2.anki.utils.ext.requireParcelable
import com.ichi2.compose.theme.AnkiDroidTheme

/** Edits the visible browser columns for Cards and Notes independently. */
class BrowserColumnSelectionFragment : DialogFragment() {
    private val viewModel: CardBrowserViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.ThemeOverlay_AnkiDroid_AlertDialog_FullScreen)
    }

    override fun onStart() {
        super.onStart()
        requireDialog().window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            id = R.id.browser_columns_compose
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AnkiDroidTheme {
                    BrowserColumnsPage(viewModel, requireArguments().requireParcelable(ARG_MODE), onClose = ::dismiss)
                }
            }
        }

    companion object {
        private const val ARG_MODE = "mode"

        fun createInstance(cardsOrNotes: CardsOrNotes): BrowserColumnSelectionFragment =
            BrowserColumnSelectionFragment().apply {
                arguments =
                    Bundle().apply {
                        putParcelable(ARG_MODE, cardsOrNotes)
                    }
            }
    }
}
