package com.hanyi.gamecontroller.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.Dp
import com.hanyi.gamecontroller.domain.model.ButtonTheme
import com.hanyi.gamecontroller.domain.model.Component
import kotlin.math.roundToInt
import androidx.core.graphics.toColorInt
import com.hanyi.gamecontroller.domain.model.SafeArea

@Composable
fun GameButton(
    component: Component,
    theme: ButtonTheme,
    safeArea: SafeArea,
    usableWidth: Dp,
    usableHeight: Dp,
    screenWidth: Dp,
    screenHeight: Dp,
    onDown: (String) -> Unit,
    onUp: (String) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val width = (component.size.width * usableWidth.value).dp
    val height = (component.size.height * usableHeight.value).dp
    val diameter = minOf(width, height)
    val buttonWidth = if (component.shape == "circle") diameter else width
    val buttonHeight = if (component.shape == "circle") diameter else height
    val centerX = safeArea.left * screenWidth.value +
            component.position.x * usableWidth.value

    val centerY = safeArea.top * screenHeight.value +
            component.position.y * usableHeight.value

    val offsetX = (centerX - buttonWidth.value / 2f).dp
    val offsetY = (centerY - buttonHeight.value / 2f).dp

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed)
            Color(theme.color.toColorInt()).copy(alpha = theme.pressedAlpha)
        else
            Color(theme.color.toColorInt())
    )

    Box(
        modifier = Modifier
            .absoluteOffset(x = offsetX, y = offsetY)
            .size(buttonWidth, buttonHeight)
            .clip(
                when (component.shape) {
                    "circle" -> CircleShape
                    else -> CircleShape // you can add RoundedCornerShape later
                }
            )
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onDown(component.command)
                        tryAwaitRelease()
                        isPressed = false
                        onUp(component.command)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = component.label,
            color = Color(theme.textColor.toColorInt()),
            fontSize = theme.textSizeSp.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
