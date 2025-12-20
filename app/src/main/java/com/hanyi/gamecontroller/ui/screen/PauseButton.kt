package com.hanyi.gamecontroller.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hanyi.gamecontroller.ui.icon.Pause

@Composable
fun PauseButton(
    modifier: Modifier = Modifier,
    isPause: Boolean,
    onTogglePause: () -> Unit
) {
    IconButton(
        modifier = modifier,
        onClick = onTogglePause
    ) {
        Icon(
            imageVector = if (isPause) Icons.Filled.PlayArrow else Pause,
            contentDescription = if (isPause) "Resume" else "Pause"
        )
    }
}