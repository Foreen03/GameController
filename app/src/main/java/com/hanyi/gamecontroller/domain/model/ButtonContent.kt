package com.hanyi.gamecontroller.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ButtonContent(
    val type: String,
    val text: String? = null,
    val image: ButtonImage? = null
)
