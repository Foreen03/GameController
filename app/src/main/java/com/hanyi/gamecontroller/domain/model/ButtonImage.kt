package com.hanyi.gamecontroller.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ButtonImage(
    val type: String,
    val value: String,
    val scaleType: String = "fit"
)
