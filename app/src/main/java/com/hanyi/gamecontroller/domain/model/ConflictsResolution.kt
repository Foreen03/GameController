package com.hanyi.gamecontroller.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ConflictsResolution(
    val name: String,
    val mode: String,
    val commands: List<String>,
    val priority: List<String>
)
