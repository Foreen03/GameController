package com.hanyi.gamecontroller.domain.model

import androidx.compose.ui.graphics.Color

data class ResolvedButtonStyle(
    val backgroundColor: Color,
    val textColor: Color,
    val pressedAlpha: Float,
    val textSizeSp: Int,
    val showBackground: Boolean
)
