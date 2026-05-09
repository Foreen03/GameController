package com.hanyi.gamecontroller.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ComponentStyle(
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val pressedAlpha: Float? = null,
    val textSizeSp: Int? = null,
    val showBackground: Boolean? = null
)
