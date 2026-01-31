package com.hanyi.gamecontroller.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GamepadConfig(
    val version: Int,
    val gamepad: GamepadMeta,
    val theme: GamepadTheme,
    val layout: Layout
)

@Serializable
data class GamepadMeta(
    val id: String,
    val name: String,
    val description: String,
    val orientation: String
)

@Serializable
data class GamepadTheme(
    val backgroundColor: String,
    val backgroundImage: BackgroundImageConfig? = null,
    val button: ButtonTheme
)

@Serializable
data class BackgroundImageConfig(
    val enabled: Boolean = false,
    val type: String,
    val value: String,
    val scaleType: String = "fill"
)

@Serializable
data class ButtonTheme(
    val color: String,
    val pressedAlpha: Float,
    val textColor: String,
    val textSizeSp: Int
)

@Serializable
data class Layout(
    val safeArea: SafeArea,
    val components: List<Component>
)

@Serializable
data class SafeArea(
    val top: Float,
    val bottom: Float,
    val left: Float,
    val right: Float
)

@Serializable
data class Component(
    val type: String,
    val id: String,
    val position: Position,
    val size: Size,
    val shape: String,
    val label: String,
    val command: String
)

@Serializable
data class Position(
    val x: Float,
    val y: Float
)

@Serializable
data class Size(
    val width: Float,
    val height: Float
)
