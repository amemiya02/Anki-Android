// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import com.ichi2.anki.common.time.TimeManager
import com.ichi2.anki.common.time.getTimestamp
import com.ichi2.anki.compat.CompatHelper
import com.ichi2.anki.dialogs.DiscardChangesDialog
import com.ichi2.anki.multimedia.AttachmentAppBar
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.ui.windows.reviewer.whiteboard.WhiteboardFragment
import com.ichi2.anki.ui.windows.reviewer.whiteboard.WhiteboardView
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.themes.Themes
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.ichi2.anki.common.android.R as CommonR

class DrawingFragment : Fragment() {
    private val whiteboardFragment
        get() = childFragmentManager.findFragmentById(R.id.fragment_container) as? WhiteboardFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = inflater.context
        val toolbar =
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AnkiDroidTheme {
                        AttachmentAppBar(
                            title = stringResource(CommonString.drawing),
                            onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                            onSave = ::onSaveDrawing,
                        )
                    }
                }
            }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(MaterialColors.getColor(this, CommonR.attr.foundationBackground))
            addView(toolbar, MATCH_PARENT, WRAP_CONTENT)
            addView(FragmentContainerView(context).apply { id = R.id.fragment_container }, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (whiteboardFragment == null) childFragmentManager.commitNow { replace(R.id.fragment_container, WhiteboardFragment()) }
        setupBackPressHandling()
    }

    /**
     * Wires the discard-dialog back-press flow and tells the child WhiteboardFragment
     * to suppress its "go back again to exit" snackbar.
     *
     * Deferred via `view.post` so it runs after the child's `onViewCreated`, ensuring
     * our [OnBackPressedCallback] is added to the dispatcher *after* the child's and
     * wins LIFO. The relative order of parent vs child `onViewCreated` differs
     * across platforms (Robolectric runs the child first; real devices have shown
     * the opposite), so the post normalizes it.
     */
    private fun setupBackPressHandling() {
        requireView().post {
            if (!isAdded) return@post
            val whiteboard = whiteboardFragment ?: return@post

            whiteboard.setDrawingMode(true)

            // Standard isEnabled pattern: only intercept the back press when there's
            // content to discard. When empty, the callback stays disabled so the
            // press falls through to the activity finishing and predictive back
            // shows its system exit animation.
            val backCallback =
                object : OnBackPressedCallback(enabled = false) {
                    override fun handleOnBackPressed() {
                        DiscardChangesDialog.showDialog(requireContext()) {
                            isEnabled = false
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

            whiteboard.isEmptyFlow
                .onEach { isEmpty -> backCallback.isEnabled = !isEmpty }
                .launchIn(viewLifecycleOwner.lifecycleScope)
        }
    }

    private fun onSaveDrawing() {
        val whiteboardView = whiteboardFragment?.whiteboardView ?: return
        val imagePath = saveWhiteboard(whiteboardView)
        val result =
            Intent().apply {
                putExtra(EXTRA_IMAGE_PATH, imagePath)
            }
        finishScreen(Activity.RESULT_OK, result)
    }

    private fun saveWhiteboard(view: WhiteboardView): Uri {
        val bitmap = createBitmap(view.width, view.height)
        val canvas = Canvas(bitmap)

        // TODO: drop the baked-in background and save with a transparent canvas, so the drawing
        //  adapts to whichever theme the card is reviewed under and empty space costs nothing
        //  in the file.
        val backgroundColor =
            if (Themes.isNightTheme) {
                Color.BLACK
            } else {
                Color.WHITE
            }
        canvas.drawColor(backgroundColor)

        view.draw(canvas)

        val baseFileName = "Whiteboard" + getTimestamp(TimeManager.time)
        return CompatHelper.compat.saveImage(
            requireContext(),
            bitmap,
            baseFileName,
            "webp",
            CompatHelper.compat.webpLossyFormat,
            90,
        )
    }

    companion object {
        const val EXTRA_IMAGE_PATH = "path"

        fun getIntent(context: Context): Intent = SingleFragmentActivity.getIntent(context, DrawingFragment::class)
    }
}
