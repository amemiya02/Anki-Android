// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2024 Ashish Yadav <mailtoashish693@gmail.com>

package com.ichi2.imagecropper

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.canhub.cropper.CropImageView
import com.ichi2.anki.CommonString
import com.ichi2.anki.R
import com.ichi2.anki.navigation.finishScreen
import com.ichi2.anki.preferences.settingsComposeView
import com.ichi2.anki.snackbar.showSnackbar
import com.ichi2.anki.withProgress
import com.ichi2.imagecropper.ImageCropper.Companion.DECODED_IMAGE_LIMIT
import com.ichi2.utils.ContentResolverUtil
import com.ichi2.utils.openInputStreamSafe
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber

/**
 * Fragment for cropping images within the AnkiDroid, uses [CropImageView] to crop the image and
 * sends the image uri as result.
 *
 * Portions of this code were adapted from the CanHub project.
 * Original source: https://github.com/CanHub/Android-Image-Cropper
 *
 * Attribution to the original authors of the CanHub/Android-Image-Cropper for their contributions.
 */
class ImageCropper : Fragment() {
    internal lateinit var cropImageView: CropImageView
        private set

    private var tooLarge by mutableStateOf(false)
    private var canCrop by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        cropImageView = CropImageView(requireContext())
        return settingsComposeView(R.id.image_cropper_screen) {
            ImageCropperScreen(
                preview = cropImageView,
                tooLarge = tooLarge,
                canCrop = canCrop,
                onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                onRotate = { cropImageView.rotateImage(90) },
                onFlipHorizontal = cropImageView::flipImageHorizontally,
                onFlipVertical = cropImageView::flipImageVertically,
                onDone = ::crop,
            )
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        cropImageView.apply {
            setOnSetImageUriCompleteListener(::onSetImageUriComplete)
            setOnCropImageCompleteListener(::onCropImageComplete)
            cropRect = Rect(100, 300, 500, 1200)
        }
        val originalImageUri =
            BundleCompat.getParcelable(requireArguments(), ARG_IMAGE_URI, Uri::class.java)
                ?: error("No image identifier was provided for cropping")
        viewLifecycleOwner.lifecycleScope.launch {
            withProgress {
                tooLarge = isImageTooBig(originalImageUri)
                if (!tooLarge) cropImageView.setImageUriAsync(originalImageUri)
            }
        }
    }

    /**
     * Check if the image isn't too big for the crop editor to handle(issue #17378).
     * @see DECODED_IMAGE_LIMIT
     * @return true if the image is bigger than our general target limit, false otherwise(or for any error)
     */
    private fun isImageTooBig(imageUri: Uri): Boolean =
        try {
            val imageStream = requireContext().contentResolver.openInputStreamSafe(imageUri)
            if (imageStream != null) {
                val opts = BitmapFactory.Options()
                opts.inJustDecodeBounds = true
                BitmapFactory.decodeStream(imageStream, null, opts)
                val imageDimen = opts.outWidth * opts.outHeight * 4 // Bitmap.Config.ARGB_8888
                Timber.d("Crop target image size: $imageDimen")
                imageDimen > DECODED_IMAGE_LIMIT
            } else {
                false
            }
        } catch (ex: Exception) {
            false
        }

    private fun crop() {
        val imageFormat = cropImageView.imageUri?.let(::getImageCompressFormat) ?: return
        cropImageView.croppedImageAsync(saveCompressFormat = imageFormat)
    }

    private fun getImageCompressFormat(uri: Uri): Bitmap.CompressFormat {
        Timber.d("Original image URI: $uri")

        val fileExtension =
            try {
                ContentResolverUtil.getFileName(requireContext().contentResolver, uri).substringAfterLast('.')
            } catch (e: Exception) {
                Timber.w(e, "Failed to retrieve file extension from URI")
                null
            }

        return when (fileExtension?.lowercase()) {
            "png" -> Bitmap.CompressFormat.PNG
            "jpeg", "jpg" -> Bitmap.CompressFormat.JPEG
            "webp" -> {
                if (Build.VERSION.SDK_INT >= 30) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION") // WEBP: deprecated in API 30, the only option below it
                    Bitmap.CompressFormat.WEBP
                }
            }
            else -> {
                Timber.w("Unknown image format: $fileExtension. Defaulting to JPEG.")
                Bitmap.CompressFormat.JPEG
            }
        }
    }

    private fun onSetImageUriComplete(
        @Suppress("UNUSED_PARAMETER") view: CropImageView,
        @Suppress("UNUSED_PARAMETER") uri: Uri,
        error: Exception?,
    ) {
        canCrop = error == null
        if (error != null) {
            Timber.e(error, "Failed to load image by URI")
            showSnackbar(CommonString.something_wrong)
        }
    }

    private fun onCropImageComplete(
        @Suppress("UNUSED_PARAMETER") view: CropImageView,
        result: CropImageView.CropResult,
    ) {
        if (result.error == null) {
            val resultIntent = Intent()
            resultIntent.putExtra(
                EXTRA_CROP_IMAGE_RESULT,
                CropResultData(
                    uriContent = result.uriContent,
                    uriPath = context?.let { result.getUriFilePath(it) },
                ),
            )
            if (isAdded) finishScreen(Activity.RESULT_OK, resultIntent)
        } else {
            Timber.e(result.error, "Failed to crop image")
            showSnackbar(CommonString.something_wrong)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cropImageView.setOnSetImageUriCompleteListener(null)
        cropImageView.setOnCropImageCompleteListener(null)
    }

    companion object {
        private const val DECODED_IMAGE_LIMIT = 100_000_000

        /**
         * The key for the original image URI passed as an argument.
         */
        const val ARG_IMAGE_URI = "image_uri"

        /**
         * The key for the cropped image path sent back in the result Intent.
         */
        const val EXTRA_CROP_IMAGE_RESULT = "crop_image_result"
    }

    @Parcelize
    data class CropResultData(
        val uriContent: Uri? = null,
        val uriPath: String? = null,
    ) : Parcelable
}
