package com.hanyi.gamecontroller

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.hanyi.gamecontroller.data.ble.BleManager
import com.hanyi.gamecontroller.data.ble.BleRepository
import com.hanyi.gamecontroller.data.controller.CommandSender
import com.hanyi.gamecontroller.data.sensor.AccelerometerRepository
import com.hanyi.gamecontroller.data.sensor.SensorCoordinator
import com.hanyi.gamecontroller.data.sensor.StepDetectorRepository
import com.hanyi.gamecontroller.ui.MainViewModel
import com.hanyi.gamecontroller.ui.screen.MainScreen
import com.hanyi.gamecontroller.ui.theme.GameControllerTheme
import com.hanyi.gamecontroller.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private lateinit var bleManager: BleManager
    private lateinit var bleRepository: BleRepository
    private lateinit var viewModel: MainViewModel

    private lateinit var sensorCoordinator: SensorCoordinator
    private lateinit var stepDetectorRepository: StepDetectorRepository
    private lateinit var accelerometerRepository: AccelerometerRepository
    private lateinit var commandSender: CommandSender
    private var gson = Gson()

    private val requestBlePermissionsLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            viewModel.onBlePermissionResult(allGranted)

            if (allGranted) {
                checkBluetoothEnabled()
            } else {
                Toast.makeText(
                    this,
                    "BLE permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val requestActivityPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            viewModel.onActivityPermissionResult(
                granted = granted,
                shouldShowRationale =
                    PermissionHelper.shouldShowPermissionRationale(this)
            )
        }

    private val enableBluetoothLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bleManager = BleManager(this)
        bleRepository = BleRepository(bleManager)
        stepDetectorRepository = StepDetectorRepository(this)
        accelerometerRepository = AccelerometerRepository(this)
        commandSender = CommandSender(
            bleRepository = bleRepository,
            gson = gson
        )
        sensorCoordinator = SensorCoordinator(
            stepDetectorRepository,
            accelerometerRepository
        )
        viewModel = MainViewModel(
            bleRepository = bleRepository,
            sensorCoordinator = sensorCoordinator,
            commandSender = commandSender,
            stepRepo = stepDetectorRepository,
            accelRepo = accelerometerRepository
        )

        enableEdgeToEdge()

        setContent {
            GameControllerTheme {

                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(uiState.requestBlePermission) {
                    if (uiState.requestBlePermission) {
                        requestBlePermissions()
                    }
                }

                LaunchedEffect(uiState.requestActivityPermission) {
                    if (uiState.requestActivityPermission) {
                        requestActivityRecognitionPermission()
                    }
                }

                MainScreen(
                    viewModel = viewModel,
                    uiState = uiState
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager.cleanUp()
    }

    private fun requestBlePermissions() {
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }

        val hasPermissions = permissions.all {
            ActivityCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermissions) {
            requestBlePermissionsLauncher.launch(permissions)
        } else {
            viewModel.onBlePermissionResult(true)
            checkBluetoothEnabled()
        }
    }

    private fun requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestActivityPermissionLauncher.launch(
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        } else {
            // Permission not required
            viewModel.onActivityPermissionResult(
                granted = true,
                shouldShowRationale = false
            )
        }
    }

    private fun checkBluetoothEnabled() {
        if (!bleManager.isBluetoothEnabled()) {
            val intent =
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(intent)
        }
    }
}
