package com.hanyi.gamecontroller.ui.screen

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hanyi.gamecontroller.ui.icon.TablerCapture

@Composable
fun ScreenshotButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(
        modifier = modifier,
        onClick = onClick
    ) {
        Icon(
            imageVector = TablerCapture,
            contentDescription = "Screenshot"
        )
    }
}