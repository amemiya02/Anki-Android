// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.previewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import com.ichi2.anki.R
import com.ichi2.anki.previewer.TemplatePreviewerFragment.Companion.ARG_KEY

/** Hosts the standalone version of the note editor's preview. */
class TemplatePreviewerPage : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentContainerView =
        FragmentContainerView(inflater.context).apply {
            id = R.id.fragment_container
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        if (savedInstanceState != null) return
        val arguments = requireNotNull(BundleCompat.getParcelable(requireArguments(), ARG_KEY, TemplatePreviewerArguments::class.java))
        childFragmentManager.commitNow {
            replace(R.id.fragment_container, TemplatePreviewerFragment.newInstance(arguments))
        }
    }

    companion object {
        fun getIntent(
            context: Context,
            arguments: TemplatePreviewerArguments,
        ): Intent =
            CardViewerActivity.getIntent(
                context,
                TemplatePreviewerPage::class,
                Bundle().apply { putParcelable(ARG_KEY, arguments) },
            )
    }
}
