package com.hanyi.gamecontroller.ui.screen

import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.hanyi.gamecontroller.domain.model.GamepadConfig
import com.hanyi.gamecontroller.ui.MainViewModel
import com.hanyi.gamecontroller.ui.icon.LucideEye
import com.hanyi.gamecontroller.ui.icon.LucideEyeOff
import kotlinx.coroutines.flow.map
import java.io.File

@Composable
fun ControllerScreen(
    config: GamepadConfig,
    viewModel: MainViewModel
) {
    var isSystemBarVisible by remember { mutableStateOf(false) }

    SystemBar(isVisible = isSystemBarVisible)

    val isPaused by remember(viewModel.uiState) {
        viewModel.uiState.map { it.isPaused }
    }.collectAsState(initial = true)

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.sendPauseCommand()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle Orientation
    when (config.gamepad.orientation) {
        "landscape" -> LockScreenOrientation(orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        "portrait" -> LockScreenOrientation(orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    val backgroundColor = remember(config.theme.backgroundColor) {
        try {
            Color(config.theme.backgroundColor.toColorInt())
        } catch (e: Exception) {
            Color.DarkGray // Fallback if hex code is invalid
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {

        val bgConfig = config.theme.backgroundImage
        if (bgConfig?.enabled == true) {
            val contentScale = when (bgConfig.scaleType) {
                "fit" -> ContentScale.Fit
                "crop" -> ContentScale.Crop
                else -> ContentScale.FillBounds
            }

            when (bgConfig.type) {
                "file" -> {
                    AsyncImage(
                        model = File(bgConfig.value),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale
                    )
                }
                "url" -> {
                    AsyncImage(
                        model = bgConfig.value,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale
                    )
                }
                "base64" -> {
                    val bitmap = remember(bgConfig.value) {
                        try {
                            val decodedBytes = Base64.decode(bgConfig.value, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size).asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    bitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "Background",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = contentScale
                        )
                    }
                }
            }
        }

        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val safeArea = config.layout.safeArea
        val usableWidth = screenWidth * (1f - safeArea.left - safeArea.right)
        val usableHeight = screenHeight * (1f - safeArea.top - safeArea.bottom)

        // System Buttons
        config.layout.systemComponents?.forEach { systemComponent ->
            val offsetX = systemComponent.position.x * screenWidth.value
            val offsetY = systemComponent.position.y * screenHeight.value
            SystemButton(
                systemComponent = systemComponent,
                theme = config.theme.button,
                isPaused = isPaused,
                isSystemBarVisible = isSystemBarVisible,
                onPause = { viewModel.sendPauseCommand() },
                onResume = { viewModel.sendResumeCommand() },
                onScreenshot = { viewModel.sendScreenshotCommand() },
                onToggleSystemBar = { isSystemBarVisible = !isSystemBarVisible },
                modifier = Modifier.absoluteOffset(x = offsetX.dp, y = offsetY.dp),
                usableWidth = usableWidth,
                usableHeight = usableHeight
            )
        }

        // Gamepad Buttons
        config.layout.components.forEach { component ->
            when (component.type) {
                "button" -> GameButton(
                    component = component,
                    safeArea = safeArea,
                    usableWidth = usableWidth,
                    usableHeight = usableHeight,
                    theme = config.theme.button,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    onDown = { viewModel.setButton(it, true) },
                    onUp = { viewModel.setButton(it, false) }
                )
            }
        }
    }
}