package com.hanyi.gamecontroller.ui.screen

import FeatherCamera
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hanyi.gamecontroller.domain.model.ButtonTheme
import com.hanyi.gamecontroller.domain.model.Component
import com.hanyi.gamecontroller.domain.model.SafeArea
import com.hanyi.gamecontroller.domain.model.SystemComponent
import com.hanyi.gamecontroller.ui.icon.LucideEye
import com.hanyi.gamecontroller.ui.icon.LucideEyeOff
import com.hanyi.gamecontroller.ui.icon.Pause
import com.hanyi.gamecontroller.ui.theme.resolveButtonStyle
import androidx.compose.foundation.layout.absoluteOffset

@Composable
fun SystemButton(
    systemComponent: SystemComponent,
    theme: ButtonTheme,
    isPaused: Boolean,
    isSystemBarVisible: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onScreenshot: () -> Unit,
    onToggleSystemBar: () -> Unit,
    modifier: Modifier = Modifier,
    usableWidth: Dp,
    usableHeight: Dp,
    safeArea: SafeArea,
    screenWidth: Dp,
    screenHeight: Dp
) {
    val component = Component(
        type = "button",
        id = systemComponent.id,
        position = com.hanyi.gamecontroller.domain.model.Position(0f, 0f),
        size = com.hanyi.gamecontroller.domain.model.Size(0f, 0f),
        shape = "circle",
        command = "",
        content = com.hanyi.gamecontroller.domain.model.ButtonContent("text", ""),
        style = systemComponent.style
    )
    val resolvedStyle = resolveButtonStyle(component, theme)
    val backgroundColor = resolvedStyle.backgroundColor

    val width = systemComponent.size?.let {
        (it.width * usableWidth.value).dp
    } ?: 56.dp

    val height = systemComponent.size?.let {
        (it.height * usableHeight.value).dp
    } ?: 56.dp

    val diameter = minOf(width, height)

    val buttonWidth =
        if (systemComponent.shape == "circle")
            diameter
        else
            width

    val buttonHeight =
        if (systemComponent.shape == "circle")
            diameter
        else
            height

    val centerX =
        safeArea.left * screenWidth.value +
                systemComponent.position.x * usableWidth.value

    val centerY =
        safeArea.top * screenHeight.value +
                systemComponent.position.y * usableHeight.value

    val offsetX = (centerX - buttonWidth.value / 2f).dp
    val offsetY = (centerY - buttonHeight.value / 2f).dp

    val clipShape = when (systemComponent.shape) {
        "circle" -> CircleShape
        else -> RoundedCornerShape(32.dp)
    }

    IconButton(
        onClick = {
            when (systemComponent.type) {
                "pause" -> if (isPaused) onResume() else onPause()
                "screenshot" -> onScreenshot()
                "toggle_system_bar" -> onToggleSystemBar()
            }
        },
        modifier = modifier
            .absoluteOffset(x = offsetX, y = offsetY)
            .size(buttonWidth, buttonHeight)
            .clip(clipShape)
            .background(backgroundColor)
    ) {
        when (systemComponent.type) {
            "pause" -> Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Pause,
                contentDescription = "Pause",
                tint = resolvedStyle.textColor
            )

            "screenshot" -> Icon(
                imageVector = FeatherCamera,
                contentDescription = "Screenshot",
                tint = resolvedStyle.textColor
            )

            "toggle_system_bar" -> Icon(
                imageVector = if (isSystemBarVisible) LucideEye else LucideEyeOff,
                contentDescription = "Toggle System Bar",
                tint = resolvedStyle.textColor
            )
        }
    }
}