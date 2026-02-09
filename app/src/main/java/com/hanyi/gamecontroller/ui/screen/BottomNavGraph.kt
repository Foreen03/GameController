package com.hanyi.gamecontroller.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hanyi.gamecontroller.domain.model.BleUiState
import com.hanyi.gamecontroller.ui.MainViewModel
import com.hanyi.gamecontroller.ui.navigation.AppNavigation
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun BottomNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: MainViewModel,
    uiState: BleUiState,
) {

    NavHost(navController = navController, startDestination = AppNavigation.Home.route){

        composable(AppNavigation.Home.route) {
            GamePadList(viewModel = viewModel) { selectedGamepad ->
                navController.navigate("gamepad/${selectedGamepad.gamepad.id}")
            }
        }

        composable(route = AppNavigation.Bluetooth.route) {
            BluetoothScreen(
                connectionState = uiState.connectionState,
                discoveredDevices = uiState.devices,
                onStartScan = viewModel::startScan,
                onStopScan = viewModel::stopScan,
                onDeviceClick = { device ->
                    viewModel.connect(device)
                },
                onDisconnect = viewModel::disconnect
            )
        }

        composable(route = "gamepad/{gamepadId}") { backStackEntry ->
            val gamepadId = backStackEntry.arguments?.getString("gamepadId")
            val gamepads by viewModel.gamepads.collectAsState()
            val gamepad = gamepads.find { it.gamepad.id == gamepadId }
            gamepad?.let {
                ControllerScreen(config = it, viewModel = viewModel)
            }
        }
    }

    val dialogState by viewModel.dialogState.collectAsState()
    val progressState by viewModel.progressState.collectAsState()
    val isReconnecting by viewModel.isReconnecting.collectAsState()

    if(dialogState.show){
        NotificationDialog(
            viewModel = viewModel,
            dialogState = dialogState
        )
    }

    if(progressState.show){
        NotificationDialog(
            viewModel = viewModel,
            dialogState = dialogState
        )
    }

    if(isReconnecting){
        ReconnectDialog(viewModel = viewModel)
    }
}