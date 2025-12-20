package com.hanyi.gamecontroller.ui.icon

/*
The MIT License (MIT)

Copyright (c) 2019-2024 The Bootstrap Authors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

*/
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val BootstrapBluetooth: ImageVector
    get() {
        if (_BootstrapBluetooth != null) return _BootstrapBluetooth!!

        _BootstrapBluetooth = ImageVector.Builder(
            name = "bluetooth",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(8.543f, 3.948f)
                lineToRelative(1.316f, 1.316f)
                lineTo(8.543f, 6.58f)
                close()
                moveToRelative(0f, 8.104f)
                lineToRelative(1.316f, -1.316f)
                lineTo(8.543f, 9.42f)
                close()
                moveToRelative(-1.41f, -4.043f)
                lineTo(4.275f, 5.133f)
                lineToRelative(0.827f, -0.827f)
                lineTo(7.377f, 6.58f)
                verticalLineTo(1.128f)
                lineToRelative(4.137f, 4.136f)
                lineTo(8.787f, 8.01f)
                lineToRelative(2.745f, 2.745f)
                lineToRelative(-4.136f, 4.137f)
                verticalLineTo(9.42f)
                lineToRelative(-2.294f, 2.274f)
                lineToRelative(-0.827f, -0.827f)
                close()
                moveTo(7.903f, 16f)
                curveToRelative(3.498f, 0f, 5.904f, -1.655f, 5.904f, -8.01f)
                curveToRelative(0f, -6.335f, -2.406f, -7.99f, -5.903f, -7.99f)
                reflectiveCurveTo(2f, 1.655f, 2f, 8.01f)
                curveTo(2f, 14.344f, 4.407f, 16f, 7.904f, 16f)
                close()
            }
        }.build()

        return _BootstrapBluetooth!!
    }

private var _BootstrapBluetooth: ImageVector? = null

