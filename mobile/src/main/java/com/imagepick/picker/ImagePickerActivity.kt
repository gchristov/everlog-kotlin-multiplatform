package com.imagepick.picker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.everlog.R
import com.yalantis.ucrop.UCrop
import java.io.File

class ImagePickerActivity : AppCompatActivity() {

    companion object {
        const val INTENT_IMAGE_PICKER_OPTION = "image_picker_option"
        const val INTENT_ASPECT_RATIO_X = "aspect_ratio_x"
        const val INTENT_ASPECT_RATIO_Y = "aspect_ratio_y"
        const val INTENT_LOCK_ASPECT_RATIO = "lock_aspect_ratio"
        const val INTENT_IMAGE_COMPRESSION_QUALITY = "image_compression_quality"
        const val INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT = "set_bitmap_max_width_height"
        const val INTENT_BITMAP_MAX_WIDTH = "bitmap_max_width"
        const val INTENT_BITMAP_MAX_HEIGHT = "bitmap_max_height"

        const val REQUEST_IMAGE_CAPTURE = 0
        const val REQUEST_GALLERY_IMAGE = 1

        private const val DEFAULT_ASPECT_RATIO_X = 16
        private const val DEFAULT_ASPECT_RATIO_Y = 9
        private const val DEFAULT_BITMAP_MAX_SIZE = 1000
        private const val DEFAULT_IMAGE_COMPRESSION = 80
    }

    private var aspectRatioX = DEFAULT_ASPECT_RATIO_X
    private var aspectRatioY = DEFAULT_ASPECT_RATIO_Y
    private var bitmapMaxWidth = DEFAULT_BITMAP_MAX_SIZE
    private var bitmapMaxHeight = DEFAULT_BITMAP_MAX_SIZE
    private var imageCompression = DEFAULT_IMAGE_COMPRESSION
    private var lockAspectRatio = false
    private var setBitmapMaxWidthHeight = false

    private var cameraImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            cropImage(uri)
        } else {
            setResultCancelled()
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraImageUri != null) {
            cropImage(cameraImageUri!!)
        } else {
            setResultCancelled()
        }
    }

    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = result.data?.let { UCrop.getOutput(it) }
            if (resultUri != null) {
                setResultOk(resultUri)
            } else {
                setResultCancelled()
            }
        } else {
            setResultCancelled()
        }
    }

    private val getContentLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            cropImage(uri)
        } else {
            setResultCancelled()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_picker)

        val intent = intent ?: run {
            Toast.makeText(applicationContext, getString(R.string.error_image_intent_null), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        aspectRatioX = intent.getIntExtra(INTENT_ASPECT_RATIO_X, DEFAULT_ASPECT_RATIO_X)
        aspectRatioY = intent.getIntExtra(INTENT_ASPECT_RATIO_Y, DEFAULT_ASPECT_RATIO_Y)
        imageCompression = intent.getIntExtra(INTENT_IMAGE_COMPRESSION_QUALITY, DEFAULT_IMAGE_COMPRESSION)
        lockAspectRatio = intent.getBooleanExtra(INTENT_LOCK_ASPECT_RATIO, false)
        setBitmapMaxWidthHeight = intent.getBooleanExtra(INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, false)
        bitmapMaxWidth = intent.getIntExtra(INTENT_BITMAP_MAX_WIDTH, DEFAULT_BITMAP_MAX_SIZE)
        bitmapMaxHeight = intent.getIntExtra(INTENT_BITMAP_MAX_HEIGHT, DEFAULT_BITMAP_MAX_SIZE)

        val requestCode = intent.getIntExtra(INTENT_IMAGE_PICKER_OPTION, -1)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            takeCameraImage()
        } else {
            chooseImageFromGallery()
        }
    }

    private fun takeCameraImage() {
        val fileName = "camera_image_${System.currentTimeMillis()}.jpg"
        val file = File(cacheDir, fileName)
        cameraImageUri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            file
        )
        takePictureLauncher.launch(cameraImageUri)
    }

    private fun chooseImageFromGallery() {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(this)) {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            getContentLauncher.launch("image/*")
        }
    }

    private fun cropImage(sourceUri: Uri) {
        Thread {
            // Check if URI is already a local file we have access to
            val localSourceUri = when {
                sourceUri.scheme == "file" -> sourceUri
                sourceUri.toString().contains(packageName) -> {
                    // It's likely our own FileProvider URI, resolve to file
                    val fileName = sourceUri.lastPathSegment?.split("/")?.lastOrNull() ?: "temp_image.jpg"
                    Uri.fromFile(File(cacheDir, fileName))
                }
                else -> copyUriToCache(sourceUri)
            }

            if (localSourceUri == null) {
                runOnUiThread { setResultCancelled() }
                return@Thread
            }

            val options = UCrop.Options().apply {
                setCompressionQuality(imageCompression)
                setToolbarColor(ContextCompat.getColor(this@ImagePickerActivity, R.color.background_card))
                setStatusBarColor(ContextCompat.getColor(this@ImagePickerActivity, R.color.background_card))
                setToolbarWidgetColor(ContextCompat.getColor(this@ImagePickerActivity, R.color.white_base))
                setToolbarTitle(getString(R.string.select_image))
                setActiveControlsWidgetColor(ContextCompat.getColor(this@ImagePickerActivity, R.color.main_accent))
                
                if (lockAspectRatio) {
                    withAspectRatio(aspectRatioX.toFloat(), aspectRatioY.toFloat())
                }
                if (setBitmapMaxWidthHeight) {
                    withMaxResultSize(bitmapMaxWidth, bitmapMaxHeight)
                }
            }

            val destinationUri = Uri.fromFile(File(cacheDir, "crop_image_${System.currentTimeMillis()}.jpg"))

            runOnUiThread {
                val uCropIntent = UCrop.of(localSourceUri, destinationUri)
                    .withOptions(options)
                    .getIntent(this)
                cropImageLauncher.launch(uCropIntent)
            }
        }.start()
    }

    private fun copyUriToCache(sourceUri: Uri): Uri? {
        return try {
            val destinationFile = File(cacheDir, "temp_picker_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(sourceUri)?.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destinationFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun setResultOk(imagePath: Uri) {
        val intent = Intent().apply {
            putExtra("path", imagePath)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setResultCancelled() {
        setResult(RESULT_CANCELED)
        finish()
    }
}
