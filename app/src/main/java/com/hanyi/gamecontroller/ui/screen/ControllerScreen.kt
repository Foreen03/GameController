package com.hanyi.gamecontroller.ui.screen

import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hanyi.gamecontroller.domain.model.GamepadConfig
import com.hanyi.gamecontroller.ui.MainViewModel
import kotlinx.coroutines.flow.map
import androidx.core.graphics.toColorInt
import com.hanyi.gamecontroller.ui.icon.LucideEye
import com.hanyi.gamecontroller.ui.icon.LucideEyeOff

@Composable
fun ControllerScreen(
    config: GamepadConfig,
    viewModel: MainViewModel
) {

    var isSystemBarVisible by remember { mutableStateOf(false) }

    SystemBar(isVisible = isSystemBarVisible)

    val isPaused by viewModel.uiState
        .map { it.isPaused }
        .collectAsState(initial = true)
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

    when (config.gamepad.orientation) {
        "landscape" -> LockScreenOrientation(orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        "portrait" -> LockScreenOrientation(orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(config.theme.backgroundColor.toColorInt()))
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        val safeArea = config.layout.safeArea
        val usableWidth = screenWidth * (1f - safeArea.left - safeArea.right)
        val usableHeight = screenHeight * (1f - safeArea.top - safeArea.bottom)

        val pauseBackground = Color(config.theme.button.color.toColorInt())

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = { isSystemBarVisible = !isSystemBarVisible }) {
                Icon(
                    imageVector = if (isSystemBarVisible) LucideEye else LucideEyeOff,
                    contentDescription = "Toggle System Bar",
                    tint = Color.Black.copy(alpha = 0.7f)
                )
            }
        }

        PauseButton(
            isPause = isPaused,
            onTogglePause = {
                if (isPaused) viewModel.sendResumeCommand()
                else viewModel.sendPauseCommand()
            },
            modifier = Modifier
                .absoluteOffset(
                    y = (safeArea.top * screenHeight.value).dp
                )
                .align(Alignment.TopCenter)
                .clip(CircleShape)
                .background(pauseBackground)
        )

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
