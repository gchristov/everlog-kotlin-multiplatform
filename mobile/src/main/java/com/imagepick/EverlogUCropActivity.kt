package com.imagepick

import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.yalantis.ucrop.UCropActivity

class EverlogUCropActivity : UCropActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootView = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.updatePadding(
                    top = systemBars.top,
                    bottom = systemBars.bottom
                )
                insets
            }
        }
    }
}
