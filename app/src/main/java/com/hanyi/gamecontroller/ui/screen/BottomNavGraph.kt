package com.hanyi.gamecontroller.ui.screen

import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hanyi.gamecontroller.domain.model.BleConnectionState
import com.hanyi.gamecontroller.domain.model.BleUiState
import com.hanyi.gamecontroller.ui.MainViewModel
import com.hanyi.gamecontroller.ui.navigation.AppNavigation

@Composable
fun BottomNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: MainViewModel,
    uiState: BleUiState,
) {

    NavHost(navController = navController, startDestination = AppNavigation.Home.route){

        composable(route = AppNavigation.Home.route) {
//            StepCounterScreen(
//                viewModel = viewModel,
//                onResume = { viewModel },
//                onPause = { onPause() },
//                onRequestPermission = { onRequestPermission() }
//            )
            ControllerScreen(viewModel = viewModel)
        }

        composable(route = AppNavigation.Bluetooth.route) {
            BluetoothScreen(
                connectionState = uiState.connectionState,
                discoveredDevices = uiState.devices,
                receivedData = uiState.lastReceivedData,
                onStartScan = viewModel::startScan,
                onStopScan = viewModel::stopScan,
                onDeviceClick = { device ->
                    viewModel.connect(device)
                },
                onSendData = { message ->
                    viewModel.sendData(message)
                },
                onReadData = {
                    viewModel.readData()
                },
                onDisconnect = viewModel::disconnect
            )
        }
    }

}