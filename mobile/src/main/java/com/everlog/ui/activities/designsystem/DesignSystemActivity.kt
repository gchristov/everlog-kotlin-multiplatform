package com.everlog.ui.activities.designsystem

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.everlog.R
import com.everlog.ui.design.CommonComposeActivity
import com.everlog.ui.design.elements.AppButton
import com.everlog.ui.design.elements.AppScreen
import com.everlog.ui.design.theme.Theme

// Debug-only showcase of the Compose design system, opened from Settings.
class DesignSystemActivity : CommonComposeActivity() {
    @Composable
    override fun Content() = DesignSystemScreen(
        onButtonClick = {
            Toast.makeText(this, R.string.design_system_button_clicked, Toast.LENGTH_SHORT).show()
        }
    )
}

@Composable
internal fun DesignSystemScreen(onButtonClick: () -> Unit) {
    AppScreen {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AppButton(
                text = stringResource(R.string.design_system_button),
                onClick = onButtonClick,
            )
        }
    }
}

@Preview
@Composable
private fun DesignSystemScreenPreview() {
    Theme {
        DesignSystemScreen(onButtonClick = {})
    }
}
