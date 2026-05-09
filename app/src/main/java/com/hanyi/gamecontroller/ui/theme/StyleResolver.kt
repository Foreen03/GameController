package com.hanyi.gamecontroller.ui.theme

import androidx.compose.ui.graphics.Color
import com.hanyi.gamecontroller.domain.model.ButtonTheme
import com.hanyi.gamecontroller.domain.model.Component
import com.hanyi.gamecontroller.domain.model.ResolvedButtonStyle

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: IllegalArgumentException) {
        Color.Black // Fallback color
    }
}

fun resolveButtonStyle(
    component: Component,
    theme: ButtonTheme
): ResolvedButtonStyle {
    val style = component.style

    val backgroundColor = style?.backgroundColor?.let { parseColor(it) } ?: parseColor(theme.backgroundColor)
    val textColor = style?.textColor?.let { parseColor(it) } ?: parseColor(theme.textColor)

    return ResolvedButtonStyle(
        backgroundColor = backgroundColor,
        textColor = textColor,
        pressedAlpha = style?.pressedAlpha ?: theme.pressedAlpha,
        textSizeSp = style?.textSizeSp ?: theme.textSizeSp,
        showBackground = style?.showBackground ?: true
    )
}
