package com.imagepick

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract

class ImagePickerContract : ActivityResultContract<ImagePickerOptions, ImagePickerResult>() {
    override fun createIntent(context: Context, input: ImagePickerOptions): Intent {
        return Intent(context, ImagePickerActivity::class.java).apply {
            putExtra(ImagePickerActivity.EXTRA_OPTIONS, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ImagePickerResult {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(ImagePickerActivity.EXTRA_RESULT, ImagePickerResult::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(ImagePickerActivity.EXTRA_RESULT)
            }
            result ?: ImagePickerResult.Cancelled
        } else {
            ImagePickerResult.Cancelled
        }
    }
}
