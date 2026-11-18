// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.anki.multimedia

import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.ichi2.anki.CollectionManager.TR
import com.ichi2.anki.CommonString
import com.ichi2.anki.common.annotations.NeedsTest
import com.ichi2.anki.common.crashreporting.CrashReportService
import com.ichi2.anki.compat.CompatHelper.Companion.getSerializableCompat
import com.ichi2.anki.dialogs.DiscardChangesDialog
import com.ichi2.anki.multimediacard.IMultimediaEditableNote
import com.ichi2.anki.multimediacard.fields.IField
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.ErrorDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * Abstract base class for fragments that handle multimedia operations.
 *
 * This class provides a common framework for fragments that need to handle multimedia operations,
 * including caching directories, managing fields and notes, and setting toolbar titles.
 *
 * @see MultimediaActivity
 */
abstract class MultimediaFragment : Fragment() {
    abstract val title: String
    internal open val replaceAction: MediaReplaceAction? = null

    protected abstract fun createMediaView(): View

    val viewModel: MultimediaViewModel by viewModels()

    private val argsStorage by lazy { MultimediaArgsStorage.create(requireContext()) }

    protected var ankiCacheDirectory: String? = null

    protected var indexValue: Int = 0
    protected lateinit var field: IField
    protected lateinit var note: IMultimediaEditableNote
    protected var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val multimediaActivityExtra =
            try {
                arguments
                    ?.getSerializableCompat<File>(MultimediaActivity.EXTRA_FRAGMENT_ARGS)
                    ?.let(argsStorage::read)
            } catch (e: Exception) {
                Timber.w(e, "Unable to load multimedia arguments")
                showErrorDialog()
                return null
            }
        if (multimediaActivityExtra != null) {
            indexValue = multimediaActivityExtra.index
            field = multimediaActivityExtra.field
            note = multimediaActivityExtra.note
            if (multimediaActivityExtra.imageUri != null) {
                imageUri = multimediaActivityExtra.imageUri.toUri()
            }
        }

        return createMediaView()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Keep the input while the fragment can be restored after recreation or process death.
        if (isRemoving || parentFragment?.isRemoving == true || activity?.isFinishing == true) {
            arguments?.getSerializableCompat<File>(MultimediaActivity.EXTRA_FRAGMENT_ARGS)?.delete()
        }
    }

    @NeedsTest("test discard dialog shown in case there are changes")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        (parentFragment as? MultimediaFlowFragment)?.setMediaToolbar(title, replaceAction)

        val backCallback =
            object : OnBackPressedCallback(
                enabled = viewModel.currentMultimediaPath.value != null,
            ) {
                override fun handleOnBackPressed() {
                    DiscardChangesDialog.showDialog(requireContext(), message = TR.addingDiscardCurrentInput()) {
                        Timber.i("MultimediaFragment:: OK button pressed to confirm discard changes")
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentMultimediaPath.collectLatest { value ->
                backCallback.isEnabled = value != null
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
    }

    /**
     * Get Uri based on current image path
     *
     * @param file the file to get URI for
     * @return current image path's uri
     */
    fun getUriForFile(file: File): Uri {
        Timber.d("getUriForFile() %s", file)
        try {
            return FileProvider.getUriForFile(
                requireActivity(),
                requireActivity().applicationContext.packageName + ".apkgfileprovider",
                file,
            )
        } catch (e: Exception) {
            // #6628 - What would cause this? Is the fallback is effective? Telemetry to diagnose more:
            Timber.w(e, "getUriForFile failed on %s - attempting fallback", file)
            CrashReportService.sendExceptionReport(e, "MultimediaFragment", "Unexpected getUriForFile failure on $file", true)
        }
        return Uri.fromFile(file)
    }

    fun File.toHumanReadableSize(): String = Formatter.formatFileSize(requireContext(), this.length())

    /**
     * Shows a Snackbar at the bottom of the screen with a predefined
     * "Something went wrong" message from the application's resources.
     */
    fun showSomethingWentWrong() {
        showSnackbar(resources.getString(CommonString.multimedia_editor_something_wrong))
    }

    /**
     * Creates and shows an AlertDialog with an error message
     * from the application's resources. Acknowledging or cancelling the dialog finishes
     * the current activity.
     */
    fun showErrorDialog(errorMessage: String? = null) {
        val screen = parentFragment as? MultimediaFlowFragment ?: this
        createPaperDialog(requireContext()) { dialog ->
            ErrorDialog(
                title = getString(CommonString.vague_error),
                message = errorMessage ?: getString(CommonString.something_wrong),
                onDismissRequest = dialog::dismiss,
                dismissText = null,
                primaryAction =
                    DialogAction(getString(CommonString.dialog_ok), {
                        dialog.dismiss()
                        screen.finishScreen()
                    }),
            )
        }.apply { setOnCancelListener { screen.finishScreen() } }.show()
    }

    /**
     * Finishes the activity with a [MultimediaResult.Cancelled] result when no media
     * has been captured yet. Call from child-launcher cancel branches to propagate
     * the cancellation without losing partial captures.
     */
    protected fun cancelIfEmpty() {
        if (viewModel.currentMultimediaUri.value == null) {
            setMultimediaResultAndFinish(MultimediaResult.Cancelled(indexValue))
        }
    }

    /**
     * Attaches the currently captured media to [field] and finishes the activity
     * with a [MultimediaResult.Success]. Call from confirm/done actions.
     */
    protected fun finishWithMedia() {
        field.mediaFile = viewModel.currentMultimediaPath.value
        field.hasTemporaryMedia = true
        setMultimediaResultAndFinish(MultimediaResult.Success(indexValue, field))
    }
}
