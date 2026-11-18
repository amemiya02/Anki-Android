// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2024 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.multimedia

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.SingleFragmentActivity
import com.ichi2.anki.multimediacard.IMultimediaEditableNote
import com.ichi2.anki.multimediacard.fields.IField
import com.ichi2.anki.snackbar.BaseSnackbarBuilderProvider
import com.ichi2.anki.snackbar.SnackbarBuilder
import com.ichi2.anki.utils.bottomCornerClearance
import com.ichi2.compose.theme.AnkiDroidTheme
import com.ichi2.compose.ui.components.HeroToolbarActionButton
import com.ichi2.utils.FragmentFactoryUtils
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * Required information for multimedia activities.
 *
 * This combines three elements into a single unit: Index, IField and IMultimediaEditableNote.
 *  - `Int`: The index of the field within the multimedia note that the multimedia content is associated with.
 *  @see IField
 *  @see IMultimediaEditableNote
 */
// TODO: move it to a better data model (remove IField & IMultimediaEditableNote)
data class MultimediaActivityExtra(
    val index: Int,
    val field: IField,
    val note: IMultimediaEditableNote,
    val imageUri: String? = null,
) : Serializable

/**
 * Multimedia activity that allows users to attach media files to an input field in NoteEditor.
 */
class MultimediaActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment = MultimediaFlowFragment().apply { arguments = Bundle(intent.extras ?: Bundle.EMPTY) }

    companion object {
        const val EXTRA_FRAGMENT_ARGS = "extra_fragment_args"
        const val EXTRA_FRAGMENT_NAME = "extra_fragment_name"

        /** used in case a fragment supports more than media operations **/
        const val EXTRA_MEDIA_OPTIONS = "extra_media_options"

        fun getIntent(
            context: Context,
            fragmentClass: KClass<out Fragment>,
            arguments: MultimediaActivityExtra? = null,
            mediaOptions: Serializable? = null,
        ): Intent =
            Intent(context, MultimediaActivity::class.java).apply {
                putExtra(EXTRA_FRAGMENT_ARGS, arguments?.let { MultimediaArgsStorage.create(context).save(it) })
                putExtra(EXTRA_FRAGMENT_NAME, fragmentClass.jvmName)
                putExtra(EXTRA_MEDIA_OPTIONS, mediaOptions)
            }
    }
}

/** Hosts the existing recording, playback or image editor and returns its typed field result. */
class MultimediaFlowFragment :
    Fragment(),
    BaseSnackbarBuilderProvider {
    private var toolbarTitle by mutableStateOf("")
    private var replaceMedia by mutableStateOf<MediaReplaceAction?>(null)

    internal fun setMediaToolbar(
        title: String,
        action: MediaReplaceAction? = null,
    ) {
        toolbarTitle = title
        replaceMedia = action
    }

    internal var isImageEditorBusy by mutableStateOf(false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (childFragmentManager.findFragmentById(R.id.multimedia_editor_container) != null) return
        val args = requireArguments()
        val name = requireNotNull(args.getString(MultimediaActivity.EXTRA_FRAGMENT_NAME))
        val editor = FragmentFactoryUtils.instantiate<Fragment>(requireActivity(), name).apply { arguments = Bundle(args) }
        childFragmentManager.commit { replace(R.id.multimedia_editor_container, editor) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val context = inflater.context
        val imageEditor = arguments?.getString(MultimediaActivity.EXTRA_FRAGMENT_NAME) == MultimediaImageFragment::class.jvmName
        val appBar =
            ComposeView(context).apply {
                id = R.id.app_bar
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AnkiDroidTheme {
                        AttachmentAppBar(
                            title = toolbarTitle,
                            onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                            actions = {
                                if (imageEditor) {
                                    HeroToolbarActionButton(enabled = !isImageEditorBusy, onClick = {
                                        val image =
                                            childFragmentManager.findFragmentById(
                                                R.id.multimedia_editor_container,
                                            ) as? MultimediaImageFragment
                                        image?.onDone()
                                    })
                                }
                                replaceMedia?.let { replace ->
                                    IconButton(onClick = replace.onClick) {
                                        Icon(painterResource(replace.icon), stringResource(CommonString.reselect))
                                    }
                                }
                            },
                        )
                    }
                }
            }
        val content = FragmentContainerView(context).apply { id = R.id.multimedia_editor_container }
        val layout =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(appBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                addView(content, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
            }
        val root =
            CoordinatorLayout(context).apply {
                id = R.id.root_layout
                addView(
                    layout,
                    CoordinatorLayout.LayoutParams(
                        CoordinatorLayout.LayoutParams.MATCH_PARENT,
                        CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            appBar.updatePadding(left = bars.left, top = bars.top, right = bars.right)
            content.updatePadding(
                left = bars.left,
                right = bars.right,
                bottom = maxOf(bars.bottom, insets.bottomCornerClearance(view)),
            )
            insets
        }
        return root
    }

    override val baseSnackbarBuilder: SnackbarBuilder = {
        // Keep error messages above the editor's bottom controls.
        anchorView = view?.findViewById<View>(R.id.multimedia_controls_anchor)
    }
}

internal data class MediaReplaceAction(
    val icon: Int,
    val onClick: () -> Unit,
)
