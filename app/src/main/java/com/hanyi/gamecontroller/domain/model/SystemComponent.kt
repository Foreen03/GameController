package com.hanyi.gamecontroller.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SystemComponent(
    val type: String,
    val id: String,
    val position: SystemPosition,
    val style: ComponentStyle? = null,
    val size: Size? = null,
    val shape: String? = null
)
