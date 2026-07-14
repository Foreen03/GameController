package com.hanyi.gamecontroller.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hanyi.gamecontroller.domain.model.BleUiState
import com.hanyi.gamecontroller.ui.MainViewModel
import com.hanyi.gamecontroller.ui.navigation.AppNavigation

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    uiState: BleUiState,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route
    val screensWithBottomBar = listOf(
        AppNavigation.Home.route,
        AppNavigation.Bluetooth.route
    )
    val isBottomBar = currentDestination in screensWithBottomBar

    Scaffold(
        // Hide bottom bar when navigate into game pad
        bottomBar = {
            if(isBottomBar){
                BottomBar(navController, currentDestination)
            }
        }
    ) { innerPadding ->
        BottomNavGraph(
            modifier = Modifier.padding(PaddingValues(bottom = innerPadding.calculateBottomPadding())),
            navController = navController,
            viewModel = viewModel,
            uiState = uiState
        )
    }
}


@Composable
fun BottomBar(navController: NavHostController, currentDestination: String?) {

    val screens = listOf(
        AppNavigation.Home,
        AppNavigation.Bluetooth
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary
    ) {
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
                    indicatorColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}