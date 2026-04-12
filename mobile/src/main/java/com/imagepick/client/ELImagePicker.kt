package com.imagepick.client

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.everlog.R
import com.imagepick.picker.ImagePickerActivity
import com.imagepick.picker.dialog.ELPickerDialog
import java.lang.ref.WeakReference

class ELImagePicker private constructor(activity: Activity) : ELImagePickerBuilder {

    private val activityRef = WeakReference(activity)
    private var errorListener: ELImagePickerBuilder.PermissionErrorListener? = BasePermissionErrorListener()
    private var removeListener: ELImagePickerBuilder.RemoveImageListener? = null

    companion object {
        const val REQUEST_IMAGE = 999
        private const val MAX_IMAGE_SIZE = 512

        @JvmStatic
        fun withActivity(activity: Activity): ELImagePickerBuilder {
            return ELImagePicker(activity)
        }
    }

    override fun withPermissionErrorListener(listener: ELImagePickerBuilder.PermissionErrorListener?): ELImagePickerBuilder {
        this.errorListener = listener
        return this
    }

    override fun withImageRemoveListener(removeListener: ELImagePickerBuilder.RemoveImageListener?): ELImagePickerBuilder {
        this.removeListener = removeListener
        return this
    }

    override fun pick(): Int {
        showSourcePicker()
        return REQUEST_IMAGE
    }

    private fun showSourcePicker() {
        val activity = activityRef.get() ?: return
        ELPickerDialog.withActivity(activity)
            .title(R.string.select_image)
            .menuLayout(if (removeListener != null) R.menu.menu_image_source_remove else R.menu.menu_image_source)
            .actionListener { which ->
                when (which) {
                    R.id.action_take_new -> handleCameraAction()
                    R.id.action_choose -> handleGalleryAction()
                    R.id.action_remove -> removeListener?.onRemoveImage()
                }
            }
            .show()
    }

    private fun handleCameraAction() {
        val activity = activityRef.get() ?: return
        val permissions = mutableListOf(Manifest.permission.CAMERA)
        
        // On older versions, we might need storage permissions to save the captured image before cropping
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (hasPermissions(permissions)) {
            launchCameraIntent()
        } else {
            requestPermissions(permissions, REQUEST_IMAGE)
        }
    }

    private fun handleGalleryAction() {
        val activity = activityRef.get() ?: return
        
        // Android 13+ (API 33) doesn't need permissions for the Photo Picker
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launchGalleryIntent()
        } else {
            val permissions = listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (hasPermissions(permissions)) {
                launchGalleryIntent()
            } else {
                requestPermissions(permissions, REQUEST_IMAGE)
            }
        }
    }

    private fun hasPermissions(permissions: List<String>): Boolean {
        val activity = activityRef.get() ?: return false
        return permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions(permissions: List<String>, requestCode: Int) {
        val activity = activityRef.get() ?: return
        ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), requestCode)
    }

    private fun launchCameraIntent() {
        val activity = activityRef.get() ?: return
        val intent = Intent(activity, ImagePickerActivity::class.java).apply {
            putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_IMAGE_CAPTURE)
            putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true)
            putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1)
            putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1)
            putExtra(ImagePickerActivity.INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, true)
            putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_WIDTH, MAX_IMAGE_SIZE)
            putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_HEIGHT, MAX_IMAGE_SIZE)
        }
        activity.startActivityForResult(intent, REQUEST_IMAGE)
    }

    private fun launchGalleryIntent() {
        val activity = activityRef.get() ?: return
        val intent = Intent(activity, ImagePickerActivity::class.java).apply {
            putExtra(ImagePickerActivity.INTENT_IMAGE_PICKER_OPTION, ImagePickerActivity.REQUEST_GALLERY_IMAGE)
            putExtra(ImagePickerActivity.INTENT_LOCK_ASPECT_RATIO, true)
            putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_X, 1)
            putExtra(ImagePickerActivity.INTENT_ASPECT_RATIO_Y, 1)
            putExtra(ImagePickerActivity.INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, true)
            putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_WIDTH, MAX_IMAGE_SIZE)
            putExtra(ImagePickerActivity.INTENT_BITMAP_MAX_HEIGHT, MAX_IMAGE_SIZE)
        }
        activity.startActivityForResult(intent, REQUEST_IMAGE)
    }

    private inner class BasePermissionErrorListener : ELImagePickerBuilder.PermissionErrorListener {
        override fun onPermissionMissing() {
            activityRef.get()?.let {
                Toast.makeText(it, "Missing permissions", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

interface ELImagePickerBuilder {
    fun withPermissionErrorListener(errorListener: PermissionErrorListener?): ELImagePickerBuilder
    fun withImageRemoveListener(removeListener: RemoveImageListener?): ELImagePickerBuilder
    fun pick(): Int

    interface PermissionErrorListener {
        fun onPermissionMissing()
    }

    interface RemoveImageListener {
        fun onRemoveImage()
    }
}
