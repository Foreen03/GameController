package com.hanyi.gamecontroller.ui.screen

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.hanyi.gamecontroller.domain.model.ButtonTheme
import com.hanyi.gamecontroller.domain.model.Component
import com.hanyi.gamecontroller.domain.model.SafeArea
import com.hanyi.gamecontroller.ui.theme.resolveButtonStyle
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun GameButton(
    component: Component,
    theme: ButtonTheme,
    safeArea: SafeArea,
    usableWidth: Dp,
    usableHeight: Dp,
    screenWidth: Dp,
    screenHeight: Dp,
    onDown: (String) -> Unit,
    onUp: (String) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val resolvedStyle = resolveButtonStyle(component, theme)

    val width = (component.size.width * usableWidth.value).dp
    val height = (component.size.height * usableHeight.value).dp
    val diameter = minOf(width, height)
    val buttonWidth = if (component.shape == "circle") diameter else width
    val buttonHeight = if (component.shape == "circle") diameter else height
    val centerX = safeArea.left * screenWidth.value +
            component.position.x * usableWidth.value

    val centerY = safeArea.top * screenHeight.value +
            component.position.y * usableHeight.value

    val offsetX = (centerX - buttonWidth.value / 2f).dp
    val offsetY = (centerY - buttonHeight.value / 2f).dp

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed)
            resolvedStyle.backgroundColor.copy(alpha = resolvedStyle.pressedAlpha)
        else
            resolvedStyle.backgroundColor
    )

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .absoluteOffset(x = offsetX, y = offsetY)
            .size(buttonWidth, buttonHeight)
            .clip(
                when (component.shape) {
                    "circle" -> CircleShape
                    else -> RoundedCornerShape(8.dp) // you can add RoundedCornerShape later
                }
            )
            .background(if (resolvedStyle.showBackground) backgroundColor else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onDown(component.command)
                        tryAwaitRelease()
                        isPressed = false
                        onUp(component.command)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        when (component.content.type) {
            "text" -> {
                component.content.text?.let {
                    Text(
                        text = it,
                        color = resolvedStyle.textColor,
                        fontSize = resolvedStyle.textSizeSp.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            "image" -> {
                component.content.image?.let { image ->

                    val bitmap = remember(image.value) {

                        try {
                            if (image.value.startsWith("data:image")) {

                                val base64 = image.value
                                    .substringAfter("base64,")
                                    .replace("\n", "")

                                val bytes = Base64.decode(
                                    base64,
                                    Base64.DEFAULT
                                )

                                BitmapFactory.decodeByteArray(
                                    bytes,
                                    0,
                                    bytes.size
                                )

                            } else {
                                null
                            }

                        } catch (e: Exception) {
                            Log.e("IMG", "Base64 decode failed", e)
                            null
                        }
                    }

                    if (bitmap != null) {

                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = component.id,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (isPressed) resolvedStyle.pressedAlpha else 1f),
                            contentScale = when (image.scaleType) {
                                "fit" -> ContentScale.Fit
                                "fill" -> ContentScale.FillBounds
                                "crop" -> ContentScale.Crop
                                else -> ContentScale.Fit
                            }
                        )

                    } else {

                        AsyncImage(
                            model = image.value,
                            contentDescription = component.id,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (isPressed) resolvedStyle.pressedAlpha else 1f),
                            contentScale = when (image.scaleType) {
                                "fit" -> ContentScale.Fit
                                "fill" -> ContentScale.FillBounds
                                "crop" -> ContentScale.Crop
                                else -> ContentScale.Fit
                            }
                        )
                    }
                }
            }
            "image_text" -> {

                component.content.image?.let { image ->

                    val bitmap = remember(image.value) {

                        try {

                            if (image.value.startsWith("data:image")) {

                                val base64 = image.value
                                    .substringAfter("base64,")
                                    .replace("\n", "")

                                val bytes = Base64.decode(
                                    base64,
                                    Base64.DEFAULT
                                )

                                BitmapFactory.decodeByteArray(
                                    bytes,
                                    0,
                                    bytes.size
                                )

                            } else {
                                null
                            }

                        } catch (e: Exception) {
                            Log.e("IMG", "Base64 decode failed", e)
                            null
                        }
                    }

                    if (bitmap != null) {

                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = component.id,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (isPressed) resolvedStyle.pressedAlpha else 1f),
                            contentScale = when (image.scaleType) {
                                "fit" -> ContentScale.Fit
                                "fill" -> ContentScale.FillBounds
                                "crop" -> ContentScale.Crop
                                else -> ContentScale.Fit
                            }
                        )

                    } else {

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(image.value)
                                .crossfade(true)
                                .build(),
                            imageLoader = remember {
                                ImageLoader.Builder(context)
                                    .components {
                                        add(SvgDecoder.Factory())
                                    }
                                    .build()
                            },
                            contentDescription = component.id,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (isPressed) resolvedStyle.pressedAlpha else 1f),
                            contentScale = when (image.scaleType) {
                                "fit" -> ContentScale.Fit
                                "fill" -> ContentScale.FillBounds
                                "crop" -> ContentScale.Crop
                                else -> ContentScale.Fit
                            }
                        )
                    }
                }

                component.content.text?.let {
                    Text(
                        text = it,
                        color = resolvedStyle.textColor,
                        fontSize = resolvedStyle.textSizeSp.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
