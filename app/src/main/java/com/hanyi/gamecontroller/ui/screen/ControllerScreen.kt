package com.hanyi.gamecontroller.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanyi.gamecontroller.ui.MainViewModel
import com.hanyi.gamecontroller.ui.icon.Pause
import kotlinx.coroutines.flow.map

@Composable
fun ControllerScreen(
    viewModel: MainViewModel
) {
    val steps by viewModel.steps.collectAsState()
    val accel by viewModel.accel.collectAsState()
    val isPaused by viewModel.uiState
        .map { it.isPaused }
        .collectAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        PauseButton(
            isPause = isPaused,
            onTogglePause = {
                if (isPaused) {
                    viewModel.sendResumeCommand()
                } else {
                    viewModel.sendPauseCommand()
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        StepCounterSection(
            steps = steps,
            error = null,
            onReset = { /* optional */ }
        )

        Spacer(modifier = Modifier.height(32.dp))

        AccelerometerSection(
            x = accel.first,
            y = accel.second,
            error = null
        )
    }
}

@Composable
fun StepCounterSection(
    steps: Int,
    error: String?,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ){
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Step Counter",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    Text(
                        text = "$steps",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "steps",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = onReset) {
                        Text("Reset Steps")
                    }
                }
            }
        }
    }
}

@Composable
fun AccelerometerSection(
    x: Float,
    y: Float,
    error: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Accelerometer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                AccelerometerRow("X", x)
                AccelerometerRow("Y", y)
            }
        }
    }
}

@Composable
fun AccelerometerRow(label: String, value: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "%.2f".format(value),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


@Composable
fun PauseButton(
    isPause: Boolean,
    onTogglePause: () -> Unit
) {
    IconButton(onClick = onTogglePause) {
        Icon(
            imageVector = if (isPause) Icons.Filled.PlayArrow else Pause,
            contentDescription = if (isPause) "Resume" else "Pause"
        )
    }
}


