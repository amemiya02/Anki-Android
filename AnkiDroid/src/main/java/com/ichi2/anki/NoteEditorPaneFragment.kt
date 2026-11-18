// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2025 Hari Srinivasan <harisrini21@gmail.com>

package com.ichi2.anki

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.displayCutout
import androidx.core.view.WindowInsetsCompat.Type.ime
import androidx.core.view.WindowInsetsCompat.Type.systemBars
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.ichi2.anki.android.input.ShortcutGroup
import com.ichi2.anki.android.input.ShortcutGroupProvider
import com.ichi2.anki.databinding.ActivityNoteEditorBinding
import com.ichi2.anki.libanki.CardOrdinal
import com.ichi2.anki.noteeditor.NoteEditorFragmentDelegate
import com.ichi2.anki.previewer.TemplatePreviewerArguments
import com.ichi2.anki.previewer.TemplatePreviewerFragment
import com.ichi2.anki.settings.uiConfigPrefs
import com.ichi2.anki.ui.ResizablePaneManager
import com.ichi2.anki.utils.bottomCornerClearance
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import com.ichi2.anki.common.android.R as CommonR

/** Hosts the note editor and its live preview pane on larger screens. */
class NoteEditorPaneFragment :
    Fragment(),
    DispatchKeyEventListener,
    ShortcutGroupProvider,
    NoteEditorFragmentDelegate {
    internal val launchIntent: Intent
        get() = BundleCompat.getParcelable(requireArguments(), ARG_LAUNCH_INTENT, Intent::class.java) ?: Intent()

    lateinit var noteEditorFragment: NoteEditorFragment

    /**
     * Reference to the previewer container that exists only on larger screens.
     * Non-null if and only if the layout is x-large and includes the previewer frame
     *
     * Unlike lateinit variables, this will remain null throughout the activity
     * lifecycle on smaller screens that don't include the previewer frame.
     *
     * Fragmentation is determined by this view's visibility after inflation.
     */
    private var previewerFrame: FragmentContainerView? = null

    /**
     * The user's persisted previewer visibility preference.
     * Applied to the views by [applyPreviewerVisibility], restored in [onCreate].
     */
    private var previewerVisibilityPref: Boolean = true

    /**
     * Job for managing delayed previewer refresh operations.
     * Automatically cancelled when the lifecycle scope is destroyed, preventing memory leaks.
     */
    private var refreshPreviewerJob: Job? = null

    /**
     * Whether the previewer pane is currently visible.
     *
     * `false` in single-pane layouts, and in split layouts where the user has
     * hidden the pane via the "Show preview sidebar" menu action.
     */
    val isPreviewerVisible: Boolean
        get() = previewPaneVisible

    private var previewPaneVisible by mutableStateOf(false)

    /** Whether this layout contains a previewer pane (split layouts only); never changes at runtime. */
    val hasPreviewerPane: Boolean
        get() = previewerFrame != null

    @VisibleForTesting
    internal lateinit var binding: ActivityNoteEditorBinding
        private set

    private fun setupEdgeToEdge() {
        val activity = requireAnkiActivity()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.window.isNavigationBarContrastEnforced = false
        } else {
            activity.setNavigationBarColor(CommonR.attr.toolbarBackgroundColor)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(systemBars() or displayCutout() or ime())
            binding.toolbarContainer.updatePadding(left = bars.left, top = bars.top, right = bars.right)
            binding.previewerFrameLayout?.let { pane ->
                val isRtl = pane.layoutDirection == View.LAYOUT_DIRECTION_RTL
                pane.updatePadding(
                    left = if (isRtl) bars.left else 0,
                    right = if (isRtl) 0 else bars.right,
                    bottom = maxOf(bars.bottom, insets.bottomCornerClearance(pane)),
                )
            }
            insets
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = ActivityNoteEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupEdgeToEdge()

        previewerFrame = binding.previewerFrame
        previewerVisibilityPref = loadPreviewerVisibilityPreference()
        applyPreviewerVisibility()
        Timber.i("Note Editor is in %s mode", if (isPreviewerVisible) "split" else "single-pane")

        // TODO: specify how non-null but invalid extras are handled
        val fragmentArgs = launchIntent.extras ?: NoteEditorFragment.addNoteArgs()

        val existingFragment = childFragmentManager.findFragmentByTag(FRAGMENT_TAG)

        if (existingFragment == null) {
            childFragmentManager.commit {
                replace(R.id.note_editor_fragment_frame, NoteEditorFragment.newInstance(fragmentArgs), FRAGMENT_TAG)
                setReorderingAllowed(true)
                /*
                 * Initializes the noteEditorFragment reference only after the transaction is committed.
                 * This ensures the fragment is fully created and available in the activity before
                 * any code attempts to interact with it, preventing potential null reference issues.
                 */
                runOnCommit {
                    noteEditorFragment = childFragmentManager.findFragmentByTag(FRAGMENT_TAG) as NoteEditorFragment
                    noteEditorFragment.setDelegate(this@NoteEditorPaneFragment)
                }
            }
        } else {
            noteEditorFragment = existingFragment as NoteEditorFragment
            noteEditorFragment.setDelegate(this)
        }

        if (hasPreviewerPane) {
            val parentLayout = binding.noteEditorXlView!!
            val divider = binding.noteEditorResizingDivider!!
            val noteEditorPane = binding.noteEditorFragmentFrame
            val previewerPane = binding.previewerFrameLayout!!
            ResizablePaneManager(
                parentLayout = parentLayout,
                divider = divider,
                startPane = noteEditorPane,
                endPane = previewerPane,
                sharedPrefs = requireContext().uiConfigPrefs(),
                startPaneWeightKey = PREF_NOTE_EDITOR_PANE_WEIGHT,
                endPaneWeightKey = PREF_PREVIEWER_PANE_WEIGHT,
            )
            if (isPreviewerVisible) {
                // Defer previewer loading to avoid blocking onCreate
                binding.previewerFrame!!.post {
                    loadNoteEditorPreviewer(true)
                }
            }
        }

    }

    fun setPreviewerVisible(visible: Boolean) {
        if (!hasPreviewerPane) return

        previewerVisibilityPref = visible
        savePreviewerVisibilityPreference(visible)
        applyPreviewerVisibility()
        if (visible) {
            loadNoteEditorPreviewer(true)
        }
    }

    private fun loadPreviewerVisibilityPreference(): Boolean = uiConfigPrefs().getBoolean(PREF_SHOW_PREVIEWER, true)

    private fun savePreviewerVisibilityPreference(visible: Boolean) {
        uiConfigPrefs().edit { putBoolean(PREF_SHOW_PREVIEWER, visible) }
    }

    private fun applyPreviewerVisibility() {
        previewPaneVisible = hasPreviewerPane && previewerVisibilityPref
        binding.previewerFrameLayout?.isVisible = isPreviewerVisible
        binding.noteEditorResizingDivider?.isVisible = isPreviewerVisible
        ViewCompat.requestApplyInsets(binding.root)
    }

    /**
     * Loads and configures the note editor previewer.
     *
     * This method orchestrates the entire preview process including:
     * - Processing the current note fields and tags
     * - Setting up the previewer fragment with the appropriate configuration
     * - Configuring the tab layout for card template navigation
     *
     * The preview will reflect the current state of the note being edited,
     * allowing users to see how their cards will appear during review.
     *
     * BUG: Fragment replacement loses user state
     * - Scroll position is reset when user has scrolled through long card content
     *
     * Ideally, we should:
     * Preserve scroll position when possible
     *
     * State preservation behavior:
     * - Regular templates: Content-only updates preserve tab selection and front/back state
     * - Cloze notes: Fragment recreation with preserved tab selection to handle dynamic cloze changes
     */
    fun loadNoteEditorPreviewer(forceReplace: Boolean) {
        if (!isPreviewerVisible) {
            return
        }

        // Check if noteEditorFragment is initialized before proceeding
        if (!::noteEditorFragment.isInitialized) {
            Timber.w("loadNoteEditorPreviewer called before noteEditorFragment was initialized")
            return
        }

        // Check if editorNote is available before proceeding
        val note =
            noteEditorFragment.editorNote ?: run {
                Timber.w("loadNoteEditorPreviewer called before editorNote was available")
                return
            }

        launchCatchingTask {
            try {
                val fields = noteEditorFragment.prepareNoteFields()
                val tags = noteEditorFragment.selectedTags.orEmpty()

                fun updatePreviewerFragment(ord: Int) {
                    val previewerFragment = createPreviewerFragment(fields, tags, ord)
                    childFragmentManager.commit {
                        replace(R.id.previewer_frame, previewerFragment)
                    }
                }

                val existingPreviewer = childFragmentManager.findFragmentById(R.id.previewer_frame)

                when {
                    forceReplace -> {
                        updatePreviewerFragment(ord = noteEditorFragment.determineCardOrdinal(fields))
                    }
                    existingPreviewer is TemplatePreviewerFragment -> {
                        if (note.notetype.isCloze) {
                            // For cloze notes, force recreation to handle dynamic cloze changes
                            // but preserve the user's selected tab
                            updatePreviewerFragment(ord = existingPreviewer.getSafeClozeOrd())
                        } else {
                            // For regular templates, just update content
                            existingPreviewer.updateContent(fields, tags)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to load note editor previewer")
            }
        }
    }

    /**
     * Creates and configures the template previewer fragment.
     *
     * @param fields The processed note fields
     * @param tags The selected tags for the note
     * @param ord The ordinal (position) of the card template to display
     * @return The configured previewer fragment
     */
    private fun createPreviewerFragment(
        fields: List<String>,
        tags: List<String>,
        ord: CardOrdinal,
    ): TemplatePreviewerFragment {
        val args =
            TemplatePreviewerArguments(
                notetypeFile =
                    NotetypeFile(
                        requireContext(),
                        noteEditorFragment.editorNote!!.notetype,
                    ),
                fields = fields,
                tags = tags,
                id = noteEditorFragment.editorNote!!.id,
                ord = ord,
                fillEmpty = false,
            )

        return TemplatePreviewerFragment.newInstance(args)
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean =
        ::noteEditorFragment.isInitialized && noteEditorFragment.dispatchKeyEvent(event)

    override val shortcuts: ShortcutGroup?
        get() = if (::noteEditorFragment.isInitialized) noteEditorFragment.shortcuts else null

    override fun onResume() {
        super.onResume()
        // Refresh the previewer when activity resumes, if needed
        if (isPreviewerVisible) {
            loadNoteEditorPreviewer(false)
        }
    }

    //region NoteEditorFragmentDelegate Protocol Methods

    override fun onNoteEditorReady() {
        // Load the previewer if visible, else do nothing
        if (!isPreviewerVisible) return

        loadNoteEditorPreviewer(false)
    }

    override fun onNoteTextChanged() {
        if (!isPreviewerVisible) return

        refreshPreviewerJob?.cancel()
        refreshPreviewerJob =
            launchCatchingTask {
                delay(REFRESH_NOTE_EDITOR_PREVIEW_DELAY)
                loadNoteEditorPreviewer(false)
            }
    }

    override fun onNoteSaved() {
        if (!isPreviewerVisible) return

        loadNoteEditorPreviewer(true)
    }

    override fun onNoteTypeChanged() {
        if (!isPreviewerVisible) return

        loadNoteEditorPreviewer(true)
    }
    //endregion

    companion object {
        private const val ARG_LAUNCH_INTENT = "noteEditorLaunch"

        internal fun arguments(intent: Intent): Bundle = bundleOf(ARG_LAUNCH_INTENT to intent)

        const val FRAGMENT_TAG = "NoteEditorFragmentTag"

        // Keys for saving pane weights in SharedPreferences
        private const val PREF_NOTE_EDITOR_PANE_WEIGHT = "noteEditorPaneWeight"
        private const val PREF_PREVIEWER_PANE_WEIGHT = "previewerPaneWeight"
        private const val PREF_SHOW_PREVIEWER = "noteEditorShowPreviewer"

        private val REFRESH_NOTE_EDITOR_PREVIEW_DELAY = 100.milliseconds
    }
}
