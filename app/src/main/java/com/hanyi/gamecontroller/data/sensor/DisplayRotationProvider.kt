package com.hanyi.gamecontroller.data.sensor

import android.content.Context
import android.view.Surface
import android.view.WindowManager

class DisplayRotationProvider(context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val rotation: Int
        get() = windowManager.defaultDisplay.rotation
}