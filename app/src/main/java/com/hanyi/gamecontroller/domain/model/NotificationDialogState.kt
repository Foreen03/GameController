package com.hanyi.gamecontroller.domain.model

data class NotificationDialogState(
    val show: Boolean = false,
    val title: String = "",
    val message: String = "",
    val progress: Float? = null
)