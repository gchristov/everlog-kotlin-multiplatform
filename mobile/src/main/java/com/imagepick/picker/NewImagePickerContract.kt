package com.imagepick.picker

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class NewImagePickerContract : ActivityResultContract<NewImagePickerOptions, NewImagePickerResult>() {
    override fun createIntent(context: Context, input: NewImagePickerOptions): Intent {
        return Intent(context, NewImagePickerActivity::class.java).apply {
            putExtra(NewImagePickerActivity.EXTRA_OPTIONS, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): NewImagePickerResult {
        return if (resultCode == Activity.RESULT_OK && intent != null) {
            val result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NewImagePickerActivity.EXTRA_RESULT, NewImagePickerResult::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NewImagePickerActivity.EXTRA_RESULT)
            }
            result ?: NewImagePickerResult.Cancelled
        } else {
            NewImagePickerResult.Cancelled
        }
    }
}
