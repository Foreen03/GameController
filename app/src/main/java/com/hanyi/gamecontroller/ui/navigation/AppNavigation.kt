package com.hanyi.gamecontroller.ui.navigation

import MaterialSymbolsStadia_controller
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.vector.ImageVector
import com.hanyi.gamecontroller.ui.icon.BootstrapBluetooth

sealed class AppNavigation(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home: AppNavigation(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )

    object Bluetooth: AppNavigation(
        route = "bluetooth",
        title = "Bluetooth",
        icon = BootstrapBluetooth
    )
}