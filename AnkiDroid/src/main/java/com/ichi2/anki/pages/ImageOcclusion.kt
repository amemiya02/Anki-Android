// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2023 Abdo <abdo@abdnh.net>

package com.ichi2.anki.pages

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import androidx.activity.addCallback
import androidx.core.os.BundleCompat
import androidx.fragment.app.viewModels
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.dialogs.DiscardChangesDialog
import com.ichi2.anki.dialogs.registerDeckSelectedHandler
import com.ichi2.anki.dialogs.startDeckSelection
import com.ichi2.anki.model.SelectableDeck
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.pages.viewmodel.ImageOcclusionArgs
import com.ichi2.anki.pages.viewmodel.ImageOcclusionViewModel
import com.ichi2.anki.pages.viewmodel.ImageOcclusionViewModel.Companion.ARG_IMAGE_OCCLUSION
import com.ichi2.anki.utils.ext.collectInLifecycle

/**
 * Page provided by the backend, for a user to add or edit an image occlusion (IO) note
 *
 * IO: Like an image-based cloze: hide parts of an image, revealed on the back
 * ([docs](https://docs.ankiweb.net/editing.html#image-occlusion) and
 * [source](https://github.com/ankitects/anki/blob/main/proto/anki/image_occlusion.proto)).
 *
 * When adding, a user may select the deck of the note
 *
 * **Paths**
 * `/image-occlusion/$PATH`
 * `/image-occlusion/$NOTE_ID`
 *
 * @see ImageOcclusionViewModel
 * @see ImageOcclusion.getIntent
 */
class ImageOcclusion : PageFragment() {
    private val viewModel: ImageOcclusionViewModel by viewModels()
    override val selectDeck: (() -> Unit)?
        get() = if (viewModel.deckNameFlow == null) null else ({ startDeckSelection(allowAll = false, allowFiltered = false) })

    override val save: () -> Unit = { webViewLayout.evaluateJavascript("anki.imageOcclusion.save()") }

    override val pagePath: String by lazy {
        val args =
            BundleCompat.getParcelable(requireArguments(), ARG_IMAGE_OCCLUSION, ImageOcclusionArgs::class.java)
                ?: throw IllegalArgumentException("IO args were not setup correctly")
        val suffix =
            when (args) {
                is ImageOcclusionArgs.Add -> Uri.encode(args.imagePath)
                is ImageOcclusionArgs.Edit -> args.noteId
            }
        "image-occlusion/$suffix"
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        with(requireActivity()) {
            onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                DiscardChangesDialog.showDialog(this@with) {
                    finishScreen()
                }
            }
        }

        registerDeckSelectedHandler(action = ::onDeckSelected)
        setupFlows()
    }

    override fun onCreateWebViewClient(savedInstanceState: Bundle?): PageWebViewClient =
        object : PageWebViewClient() {
            override fun onPageFinished(
                view: WebView?,
                url: String?,
            ) {
                super.onPageFinished(view, url)
                viewModel.args.toImageOcclusionMode().let { options ->
                    view?.evaluateJavascript(
                        """
                        (() => {
                            const mode = $options;
                            if (mode.kind === "add") {
                                mode.notetypeId = BigInt(mode.notetypeId);
                            } else {
                                mode.noteId = BigInt(mode.noteId);
                            }
                            globalThis.anki.imageOcclusion.mode = mode;
                        })();
                        """.trimIndent(),
                        null,
                    )
                }
            }
        }

    private fun onDeckSelected(deck: SelectableDeck?) {
        if (deck == null) return
        require(deck is SelectableDeck.Deck)
        viewModel.handleDeckSelection(deck.deckId)
    }

    // HACK: detect a successful save; #19443 will provide a better method
    // backend calls are only made on success; .save() does not notify on failure
    override suspend fun handlePostRequest(
        uri: PostRequestUri,
        bytes: ByteArray,
    ): ByteArray =
        super.handlePostRequest(uri, bytes).also {
            when (uri.backendMethodName) {
                "addImageOcclusionNote", "updateImageOcclusionNote" -> viewModel.onSaveOperationCompleted()
            }
        }

    private fun setupFlows() {
        viewModel.deckNameFlow?.collectInLifecycle { pageTitle = it }
    }

    companion object {
        /**
         * @param args arguments for either adding or editing a note
         */
        fun getIntent(
            context: Context,
            args: ImageOcclusionArgs,
        ): Intent {
            val arguments = Bundle().apply { putParcelable(ARG_IMAGE_OCCLUSION, args) }
            return SingleFragmentActivity.getIntent(context, ImageOcclusion::class, arguments)
        }
    }
}
