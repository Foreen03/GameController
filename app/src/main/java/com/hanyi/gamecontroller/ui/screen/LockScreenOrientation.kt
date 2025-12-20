package com.hanyi.gamecontroller.ui.screen

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current

    val activity = context as? Activity

    DisposableEffect(activity) {
        if (activity == null) {
            onDispose { }
            return@DisposableEffect onDispose { }
        }

        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = orientation

        onDispose {
            activity.requestedOrientation = originalOrientation
        }
    }
}
