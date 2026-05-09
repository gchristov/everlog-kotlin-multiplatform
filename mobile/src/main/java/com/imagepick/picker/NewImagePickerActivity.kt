package com.imagepick.picker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.everlog.R
import com.everlog.ui.navigator.ELNavigator
import com.imagepick.picker.dialog.ELPickerDialog
import com.yalantis.ucrop.UCrop
import java.io.File

class NewImagePickerActivity : AppCompatActivity() {

    private lateinit var options: NewImagePickerOptions
    private var cameraImageUri: Uri? = null
    private var isHandlingAction = false
    private var pendingAction: Int? = null

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            when (pendingAction) {
                R.id.action_take_new -> handleCameraAction()
                R.id.action_choose -> handleGalleryAction()
                else -> showPicker()
            }
        } else {
            val rationale = if (options.permissionRationaleResId != 0) options.permissionRationaleResId else R.string.media_permissions_required_message
            ELNavigator(this).promptForAppSettings(rationale)
        }
        pendingAction = null
    }

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
        
        options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_OPTIONS, NewImagePickerOptions::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_OPTIONS)
        } ?: NewImagePickerOptions()

        if (savedInstanceState != null) {
            val savedAction = savedInstanceState.getInt("pendingAction", -1)
            if (savedAction != -1) pendingAction = savedAction
            cameraImageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getParcelable("cameraImageUri", Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState.getParcelable("cameraImageUri")
            }
        }

        if (savedInstanceState == null) {
            showPicker()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("pendingAction", pendingAction ?: -1)
        outState.putParcelable("cameraImageUri", cameraImageUri)
    }

    private fun showPicker() {
        isHandlingAction = false
        val title = if (options.titleResId != 0) options.titleResId else R.string.select_image
        val menu = if (options.allowRemove) R.menu.menu_image_source_remove else R.menu.menu_image_source

        ELPickerDialog.withActivity(this)
            .title(title)
            .menuLayout(menu)
            .actionListener { which ->
                isHandlingAction = true
                when (which) {
                    R.id.action_take_new -> handleCameraAction()
                    R.id.action_choose -> handleGalleryAction()
                    R.id.action_remove -> setResultRemoved()
                }
            }
            .dismissListener {
                if (!isChangingConfigurations && !isHandlingAction) {
                    finish()
                }
            }
            .show()
    }

    private fun handleCameraAction() {
        pendingAction = R.id.action_take_new
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (hasPermissions(permissions)) {
            pendingAction = null
            launchCamera()
        } else {
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun handleGalleryAction() {
        pendingAction = R.id.action_choose
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingAction = null
            launchGallery()
        } else {
            val permissions = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (hasPermissions(permissions)) {
                pendingAction = null
                launchGallery()
            } else {
                requestPermissionsLauncher.launch(permissions.toTypedArray())
            }
        }
    }

    private fun hasPermissions(permissions: List<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun launchCamera() {
        val fileName = "camera_image_${System.currentTimeMillis()}.jpg"
        val file = File(cacheDir, fileName)
        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            file
        )
        cameraImageUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun launchGallery() {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(this)) {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            getContentLauncher.launch("image/*")
        }
    }

    private fun cropImage(sourceUri: Uri) {
        Thread {
            val localSourceUri = when {
                sourceUri.scheme == "file" -> sourceUri
                sourceUri.toString().contains(packageName) -> {
                    val fileName = sourceUri.lastPathSegment?.split("/")?.lastOrNull() ?: "temp_image.jpg"
                    Uri.fromFile(File(cacheDir, fileName))
                }
                else -> copyUriToCache(sourceUri)
            }

            if (localSourceUri == null) {
                runOnUiThread { setResultCancelled() }
                return@Thread
            }

            val uCropOptions = UCrop.Options().apply {
                setCompressionQuality(80)
                setToolbarColor(ContextCompat.getColor(this@NewImagePickerActivity, R.color.background_card))
                setStatusBarColor(ContextCompat.getColor(this@NewImagePickerActivity, R.color.background_card))
                setToolbarWidgetColor(ContextCompat.getColor(this@NewImagePickerActivity, R.color.white_base))
                setToolbarTitle(getString(R.string.select_image))
                setActiveControlsWidgetColor(ContextCompat.getColor(this@NewImagePickerActivity, R.color.main_accent))
                
                if (options.lockAspectRatio) {
                    withAspectRatio(options.aspectRatioX.toFloat(), options.aspectRatioY.toFloat())
                }
                withMaxResultSize(options.maxWidth, options.maxHeight)
            }

            val destinationUri = Uri.fromFile(File(cacheDir, "crop_image_${System.currentTimeMillis()}.jpg"))

            runOnUiThread {
                val uCropIntent = UCrop.of(localSourceUri, destinationUri)
                    .withOptions(uCropOptions)
                    .getIntent(this@NewImagePickerActivity)

                uCropIntent.setClass(this@NewImagePickerActivity, EverlogUCropActivity::class.java)
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

    private fun setResultOk(uri: Uri) {
        val intent = Intent().apply {
            putExtra(EXTRA_RESULT, NewImagePickerResult.Success(uri))
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setResultRemoved() {
        val intent = Intent().apply {
            putExtra(EXTRA_RESULT, NewImagePickerResult.Removed)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setResultCancelled() {
        val intent = Intent().apply {
            putExtra(EXTRA_RESULT, NewImagePickerResult.Cancelled)
        }
        setResult(RESULT_CANCELED, intent)
        finish()
    }

    companion object {
        const val EXTRA_OPTIONS = "extra_options"
        const val EXTRA_RESULT = "extra_result"
    }
}
