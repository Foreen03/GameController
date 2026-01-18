package com.hanyi.gamecontroller.ui.screen

import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun SystemBar(isVisible: Boolean) {
    val view = LocalView.current
    val window = (view.context as? android.app.Activity)?.window ?: return
    val currentIsVisible = rememberUpdatedState(isVisible)

    DisposableEffect(Unit) {
        if (!currentIsVisible.value) {
            hideSystemBars(window, view)
        }

        onDispose {
            showSystemBars(window, view)
        }
    }

    DisposableEffect(isVisible) {
        if (isVisible) {
            showSystemBars(window, view)
        } else {
            hideSystemBars(window, view)
        }
        onDispose {}
    }
}

private fun hideSystemBars(window: Window, view: View) {
    val windowInsetsController = WindowCompat.getInsetsController(window, view)
    windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
}

private fun showSystemBars(window: Window, view: View) {
    val windowInsetsController = WindowCompat.getInsetsController(window, view)
    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
}
