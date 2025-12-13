package com.hanyi.gamecontroller.ui.screen

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hanyi.gamecontroller.domain.model.BleConnectionState
import com.hanyi.gamecontroller.data.ble.BleManager
import com.hanyi.gamecontroller.domain.model.BleUiState
import com.hanyi.gamecontroller.ui.MainViewModel
import com.hanyi.gamecontroller.ui.navigation.AppNavigation

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    uiState: BleUiState,
) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomBar(navController) }
    ) { innerPadding ->
        BottomNavGraph(
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            viewModel = viewModel,
            uiState = uiState
        )
    }
}


@Composable
fun BottomBar(navController: NavHostController) {

    val screens = listOf(
        AppNavigation.Home,
        AppNavigation.Bluetooth
    )

    NavigationBar(
        containerColor = Color(0xFF0F9D58)
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination?.route
        screens.forEach { navItem ->
            NavigationBarItem(
                selected = currentDestination == navItem.route,
                onClick = {
                    navController.navigate(navItem.route)
                },
                icon = {
                    Icon(imageVector = navItem.icon, contentDescription = navItem.title)
                },
                label = {
                    Text(text = navItem.title)
                },
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = Color(0xFF195334)
                )
            )
        }
    }
}