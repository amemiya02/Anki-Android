// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.ui.windows.reviewer.whiteboard

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.annotation.VisibleForTesting
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.ThemeUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.view.isInvisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.ichi2.anki.AnkiDroidApp
import com.ichi2.anki.CommonString
import com.ichi2.anki.DispatchKeyEventListener
import com.ichi2.anki.R
import com.ichi2.anki.android.back.doubleBackPressCallback
import com.ichi2.anki.cardviewer.Gesture
import com.ichi2.anki.common.time.TimeManager
import com.ichi2.anki.common.time.getTimestamp
import com.ichi2.anki.common.utils.android.systemIsInNightMode
import com.ichi2.anki.compat.CompatHelper.Companion.compat
import com.ichi2.anki.databinding.FragmentWhiteboardBinding
import com.ichi2.anki.preferences.reviewer.WhiteboardAction
import com.ichi2.anki.reviewer.BindingMap
import com.ichi2.anki.reviewer.ReviewerBinding
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.utils.ext.sharedPrefs
import com.ichi2.compose.ui.dialogs.ConfirmationDialog
import com.ichi2.compose.ui.dialogs.DialogAction
import com.ichi2.compose.ui.dialogs.DialogActionStyle
import com.ichi2.compose.ui.dialogs.DialogChoice
import com.ichi2.compose.ui.dialogs.DialogChoiceRow
import com.ichi2.compose.ui.dialogs.PaperDialog
import com.ichi2.compose.ui.dialogs.createPaperDialog
import com.ichi2.utils.increaseHorizontalPaddingOfMenuIcons
import com.ichi2.utils.toRGBAHex
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.roundToInt

/**
 * Fragment that displays a whiteboard and its controls.
 */
class WhiteboardFragment :
    Fragment(R.layout.fragment_whiteboard),
    PopupMenu.OnMenuItemClickListener,
    DispatchKeyEventListener {
    @VisibleForTesting
    internal val viewModel: WhiteboardViewModel by viewModels {
        WhiteboardViewModel.factory(AnkiDroidApp.sharedPrefs())
    }

    val binding by viewBinding(FragmentWhiteboardBinding::bind)
    private lateinit var bindingMap: BindingMap<ReviewerBinding, WhiteboardAction>
    private var doubleBackCallback: OnBackPressedCallback? = null

    private var eraserDialog: ComponentDialog? = null
    private var brushDialog: ComponentDialog? = null

    /** Called when no bindings used the triggered gestures */
    var gestureFallbackListener: ((Gesture) -> Unit)? = null

    /** Called when the drawing is hidden or shown from the whiteboard's menu */
    var onDrawingShownChanged: ((Boolean) -> Unit)? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val isNightMode = systemIsInNightMode(requireContext())
        viewModel.loadState(isNightMode)

        setupUI()
        observeViewModel(binding.whiteboardView)
        setupDoubleBackPress()

        binding.whiteboardView.isStylusButtonOverriding = viewModel.isStylusOverriding
        binding.whiteboardView.onNewPath = viewModel::addPath
        binding.whiteboardView.onEraseGestureStart = viewModel::startPathEraseGesture
        binding.whiteboardView.onEraseGestureMove = viewModel::erasePathsToPoint
        binding.whiteboardView.onEraseGestureCancel = viewModel::cancelPathEraseGesture
        binding.whiteboardView.onEraseGestureEnd = viewModel::endPathEraseGesture
        binding.whiteboardView.onStylusButtonStateChanged = { isPressed ->
            viewModel.setStylusButtonPressed(isPressed)
            binding.whiteboardView.activeTool = viewModel.activeTool.value
        }
    }

    private fun setupDoubleBackPress() {
        doubleBackCallback =
            doubleBackPressCallback(
                enabled = computeDoubleBackEnabled(),
                onFirstBack = { showSnackbar(CommonString.back_pressed_once, Snackbar.LENGTH_SHORT) },
                shouldReEnable = { computeDoubleBackEnabled() },
            ).also {
                requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, it)
            }
        // Keep the callback in sync as the inputs change (host toggling drawing mode,
        // hidden state changes from the reviewer's show/hide transactions).
        viewModel.isDrawing
            .onEach { doubleBackCallback?.isEnabled = computeDoubleBackEnabled() }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun computeDoubleBackEnabled(): Boolean {
        val isUsingGesturesNavigation = context?.let { compat.isUsingSystemGestureNavigation(it) } == true
        return !viewModel.isDrawing.value && !isHidden && isUsingGesturesNavigation
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        doubleBackCallback?.isEnabled = computeDoubleBackEnabled()
    }

    /**
     * Switches the whiteboard into "drawing" mode, where the host owns back
     * navigation (e.g. a discard-changes dialog) and the reviewer's "go back again
     * to exit" snackbar is suppressed.
     */
    fun setDrawingMode(drawing: Boolean) {
        viewModel.isDrawing.value = drawing
    }

    /** Shows or hides the drawing, as the host remembers it, without calling [onDrawingShownChanged] */
    fun setDrawingShown(isShown: Boolean) {
        viewModel.isDrawingShown.value = isShown
    }

    /** Fills [menu] with the whiteboard's overflow actions in their current state */
    @VisibleForTesting
    internal fun prepareOverflowMenu(menu: Menu) {
        requireActivity().menuInflater.inflate(R.menu.whiteboard, menu)
        with(menu) {
            findItem(R.id.action_toggle_stylus).isChecked = viewModel.isStylusOnlyMode.value
            (this as? MenuBuilder)?.setOptionalIconsVisible(true)
            context?.increaseHorizontalPaddingOfMenuIcons(this)

            val alignmentMenuItemId =
                when (viewModel.toolbarAlignment.value) {
                    ToolbarAlignment.LEFT -> R.id.action_align_left
                    ToolbarAlignment.RIGHT -> R.id.action_align_right
                    ToolbarAlignment.BOTTOM -> R.id.action_align_bottom
                }
            findItem(alignmentMenuItemId).isEnabled = false

            // hiding and saving the drawing belong to the study screen, not to drawing an image
            val isStudying = !viewModel.isDrawing.value
            findItem(R.id.action_save_whiteboard).isVisible = isStudying
            findItem(R.id.action_toggle_drawing).apply {
                isVisible = isStudying
                val isShown = viewModel.isDrawingShown.value
                setTitle(if (isShown) CommonString.hide_whiteboard else CommonString.show_whiteboard)
                setIcon(if (isShown) R.drawable.symbol_visibility_off else R.drawable.symbol_visibility)
            }
        }
    }

    private fun setupUI() {
        val toolbar = binding.whiteboardToolbar

        toolbar.setControls {
            WhiteboardControls(
                model = viewModel,
                overflowMenu = { MenuBuilder(requireContext()).also(::prepareOverflowMenu) },
                onMenuItem = ::onMenuItemClick,
                onEraser = {
                    if (viewModel.activeTool.value is WhiteboardTool.Eraser) showEraserOptions() else viewModel.enableEraser()
                },
                onBrush = { index ->
                    if (viewModel.activeBrushIndex.value == index && viewModel.activeTool.value !is WhiteboardTool.Eraser) {
                        showBrushOptions(index)
                    } else {
                        viewModel.setActiveBrush(index)
                    }
                },
                onRemoveBrush = { index ->
                    if (viewModel.brushes.value.size >
                        1
                    ) {
                        showRemoveColorDialog(index)
                    } else {
                        showSnackbar(CommonString.cannot_remove_last_brush_message)
                    }
                },
            )
        }

        binding.whiteboardToolbar.onToolbarVisibilityChanged = { isShown ->
            viewModel.setIsToolbarShown(isShown)
        }

        bindingMap = BindingMap(sharedPrefs(), WhiteboardAction.entries, viewModel)
        binding.root.setOnGenericMotionListener { _, event ->
            bindingMap.onGenericMotionEvent(event)
        }
        binding.whiteboardView.setOnMultiTouchListener { touchNumber ->
            val gesture =
                when (touchNumber) {
                    2 -> Gesture.TWO_FINGER_TAP
                    3 -> Gesture.THREE_FINGER_TAP
                    4 -> Gesture.FOUR_FINGER_TAP
                    else -> return@setOnMultiTouchListener
                }
            val result = onGesture(gesture)
            if (!result) {
                gestureFallbackListener?.invoke(gesture)
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isHidden || event.action != KeyEvent.ACTION_DOWN) return false
        return bindingMap.onKeyDown(event)
    }

    fun onScreenShake(): Boolean = !isHidden && onGesture(Gesture.SHAKE)

    /** Handles gestures detected on the whiteboard or on the card beneath it in stylus mode. */
    fun onGesture(gesture: Gesture): Boolean = bindingMap.onGesture(gesture)

    /**
     * Sets up observers for the ViewModel's flows.
     */
    private fun observeViewModel(whiteboardView: WhiteboardView) {
        val toolbar = binding.whiteboardToolbar

        viewModel.paths.onEach(whiteboardView::setHistory).launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.activeTool
            .onEach { tool ->
                whiteboardView.activeTool = tool
                if (tool !is WhiteboardTool.Eraser) {
                    eraserDialog?.dismiss()
                }
            }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.isStylusOnlyMode
            .onEach { isEnabled ->
                whiteboardView.isStylusOnlyMode = isEnabled
            }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.toolbarAlignment
            .onEach { alignment ->
                toolbar.setAlignment(alignment)
                updateToolbarPosition(alignment)
            }.launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.isDrawingShown
            .onEach { isShown -> whiteboardView.isInvisible = !isShown }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        viewModel.isToolbarShown
            .onEach { isShown ->
                if (isShown) {
                    toolbar.show()
                } else {
                    toolbar.hide()
                }
            }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    /**
     * Shows a dialog for adding a new brush color.
     */
    private fun showAddColorDialog() {
        requireContext()
            .showColorPickerDialog(viewModel.currentBrushColor) { color ->
                Timber.i("Added brush with color ${color.toRGBAHex()}")
                viewModel.addBrush(color)
            }
    }

    /**
     * Shows a confirmation dialog for removing a brush.
     */
    private fun showRemoveColorDialog(index: Int) {
        createPaperDialog(requireContext()) { dialog ->
            ConfirmationDialog(
                title = "",
                message = getString(CommonString.whiteboard_remove_brush_message),
                onDismissRequest = dialog::dismiss,
                confirm =
                    DialogAction(getString(CommonString.dialog_remove), {
                        dialog.dismiss()
                        Timber.i("Removed brush of index %d", index)
                        viewModel.removeBrush(index)
                    }, DialogActionStyle.Destructive),
            )
        }.show()
    }

    private fun showBrushOptions(brushIndex: Int) {
        brushDialog =
            createPaperDialog(requireContext()) { dialog ->
                val brushes by viewModel.brushes.collectAsStateWithLifecycle()
                val brush = brushes.getOrNull(brushIndex) ?: return@createPaperDialog
                val label = stringResource(CommonString.whiteboard_stroke_width)
                PaperDialog(
                    title = label,
                    onDismissRequest = dialog::dismiss,
                    actions = listOf(DialogAction(stringResource(CommonString.dialog_ok), dialog::dismiss)),
                ) {
                    Text(brush.width.roundToInt().toString())
                    Slider(
                        value = brush.width,
                        onValueChange = viewModel::setActiveStrokeWidth,
                        valueRange = 1f..60f,
                        steps = 58,
                        modifier = Modifier.semantics { contentDescription = label },
                    )
                    TextButton(onClick = {
                        requireContext().showColorPickerDialog(brush.color) { color ->
                            viewModel.updateBrushColor(color)
                            dialog.dismiss()
                        }
                    }) {
                        Canvas(Modifier.size(40.dp)) {
                            drawCircle(Color(brush.color), radius = (brush.width / 2).coerceAtMost(size.minDimension / 2))
                        }
                        Text(stringResource(CommonString.choose_color))
                    }
                }
            }.apply {
                setOnDismissListener { brushDialog = null }
                show()
            }
    }

    private fun showEraserOptions() {
        eraserDialog =
            createPaperDialog(requireContext()) { dialog ->
                val active by viewModel.activeTool.collectAsStateWithLifecycle()
                val eraser = active as? WhiteboardTool.Eraser ?: return@createPaperDialog
                PaperDialog(
                    title = stringResource(CommonString.eraser),
                    onDismissRequest = dialog::dismiss,
                    actions = listOf(DialogAction(stringResource(CommonString.dialog_ok), dialog::dismiss)),
                ) {
                    for (mode in listOf(EraserMode.INK, EraserMode.STROKE)) {
                        val name = if (mode == EraserMode.INK) CommonString.whiteboard_ink_eraser else CommonString.whiteboard_stroke_eraser
                        DialogChoiceRow(
                            choice = DialogChoice(stringResource(name)),
                            checked = eraser.mode == mode,
                            onClick = { viewModel.setEraserMode(mode) },
                            multiple = false,
                        )
                    }
                    val widthLabel = stringResource(CommonString.whiteboard_stroke_width)
                    Text(eraser.width.roundToInt().toString())
                    Slider(
                        value = eraser.width,
                        onValueChange = viewModel::setEraserStrokeWidth,
                        valueRange = 5f..200f,
                        steps = 38,
                        modifier = Modifier.semantics { contentDescription = widthLabel },
                    )
                }
            }.apply {
                setOnDismissListener { eraserDialog = null }
                show()
            }
    }

    override fun onDestroyView() {
        brushDialog?.dismiss()
        eraserDialog?.dismiss()
        super.onDestroyView()
    }

    @SuppressLint("RtlHardcoded")
    private fun updateToolbarPosition(alignment: ToolbarAlignment) {
        binding.whiteboardToolbar.updateLayoutParams<FrameLayout.LayoutParams> {
            gravity =
                when (alignment) {
                    ToolbarAlignment.BOTTOM -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    ToolbarAlignment.LEFT -> Gravity.LEFT or Gravity.CENTER_VERTICAL
                    ToolbarAlignment.RIGHT -> Gravity.RIGHT or Gravity.CENTER_VERTICAL
                }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        Timber.i("WhiteboardFragment::onMenuItemClick %s", item.title)
        when (item.itemId) {
            R.id.action_add_brush -> showAddColorDialog()
            R.id.action_toggle_stylus -> {
                item.isChecked = !item.isChecked
                viewModel.toggleStylusOnlyMode()
            }
            R.id.action_hide_toolbar -> viewModel.setIsToolbarShown(false)
            R.id.action_align_left -> viewModel.setToolbarAlignment(ToolbarAlignment.LEFT)
            R.id.action_align_bottom -> viewModel.setToolbarAlignment(ToolbarAlignment.BOTTOM)
            R.id.action_align_right -> viewModel.setToolbarAlignment(ToolbarAlignment.RIGHT)
            R.id.action_clear -> viewModel.clearCanvas()
            R.id.action_toggle_drawing -> {
                val isShown = !viewModel.isDrawingShown.value
                viewModel.isDrawingShown.value = isShown
                onDrawingShownChanged?.invoke(isShown)
            }
            R.id.action_save_whiteboard -> saveDrawing()
            else -> return false
        }
        return true
    }

    /** Saves the drawing as an image on the Card's paper, and says where it went */
    private fun saveDrawing() {
        val view = binding.whiteboardView
        if (view.width == 0 || view.height == 0) return
        val bitmap = createBitmap(view.width, view.height)
        Canvas(bitmap).apply {
            drawColor(ThemeUtils.getThemeAttrColor(requireContext(), android.R.attr.colorBackground))
            view.draw(this)
        }
        val context = requireContext().applicationContext
        val fileName = "Whiteboard" + getTimestamp(TimeManager.time)
        lifecycleScope.launch {
            val message =
                try {
                    val uri =
                        withContext(Dispatchers.IO) {
                            // PNG: a drawing is lines on paper, and image/png is a type every MediaStore accepts
                            compat.saveImage(context, bitmap, fileName, "png", Bitmap.CompressFormat.PNG, 100)
                        }
                    getString(CommonString.white_board_image_saved, uri.path)
                } catch (e: Exception) {
                    Timber.w(e, "saving the whiteboard")
                    getString(CommonString.white_board_image_save_failed, e.localizedMessage)
                }
            showSnackbar(message, Snackbar.LENGTH_SHORT)
        }
    }

    /**
     * Sets a listener to when the whiteboard is scrolled vertically,
     * which can happen by scrolling with two fingers, or with just one
     * if the `Stylus mode` is enabled.
     */
    fun setOnScrollByListener(listener: OnScrollByListener) {
        binding.whiteboardView.setOnScrollByListener(listener)
    }

    fun resetCanvas() = viewModel.reset()

    /**
     * @return whether the whiteboard is completely empty, including the undo and redo stacks.
     */
    fun isEmpty(): Boolean = !viewModel.canUndo.value && !viewModel.canRedo.value

    /**
     * Emits `true` when the whiteboard is empty (cannot undo or redo) and `false` otherwise.
     * Useful for hosts that need to react to content changes, e.g. to toggle a back-press
     * callback's `isEnabled`.
     */
    val isEmptyFlow: Flow<Boolean>
        get() =
            combine(viewModel.canUndo, viewModel.canRedo) { canUndo, canRedo ->
                !canUndo && !canRedo
            }
}
