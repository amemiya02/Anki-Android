// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.pages

import android.os.Bundle
import android.view.View

class CardInfoFragment : PageFragment() {
    override val pagePath: String by lazy {
        val cardId = requireArguments().getLong(ARG_CARD_ID)
        "card-info/$cardId"
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val title = requireArguments().getString(ARG_TITLE)
        if (title != null) {
            pageTitle = title
        }
    }

    companion object {
        const val ARG_CARD_ID = "cardId"
        const val ARG_TITLE = "title"
    }
}
