// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.previewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.ichi2.anki.R
import com.ichi2.anki.settings.Prefs
import com.ichi2.anki.settings.enums.FrameStyle
import com.ichi2.anki.workarounds.SafeWebViewLayout
import com.ichi2.themes.Themes

/** Hosts the existing card renderer in the Compose preview layouts. */
abstract class ComposeCardViewerFragment : CardViewerFragment(0) {
    private var cardWebView: SafeWebViewLayout? = null

    final override val webViewLayout: SafeWebViewLayout get() = checkNotNull(cardWebView)

    protected val fullBleed get() = Prefs.frameStyle == FrameStyle.BOX

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): ComposeView {
        cardWebView = SafeWebViewLayout(inflater.context).apply { id = R.id.web_view_layout }
        return ComposeView(inflater.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
    }

    override fun onLoadInitialHtml(): String =
        stdHtml(
            context = requireContext(),
            nightMode = Themes.isNightTheme,
            extraCss = cardImageZoomCss(),
        )

    override fun onDestroyView() {
        super.onDestroyView()
        cardWebView = null
    }
}
