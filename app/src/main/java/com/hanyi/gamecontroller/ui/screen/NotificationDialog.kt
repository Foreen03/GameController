package com.hanyi.gamecontroller.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hanyi.gamecontroller.domain.model.NotificationDialogState
import com.hanyi.gamecontroller.ui.MainViewModel

@Composable
fun NotificationDialog(viewModel: MainViewModel, dialogState: NotificationDialogState) {

    if (!dialogState.show) return

    AlertDialog(
        onDismissRequest = {
            // Only allow dismissal if NOT in progress mode
            if (dialogState.progress == null) {
                viewModel.dismissDialog()
            }
        },
        title = { Text(text = dialogState.title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Always show the message (e.g., "50%" or "Success!")
                Text(
                    text = dialogState.message,
                    style = MaterialTheme.typography.bodyLarge
                )

                // If progress exists, show the bar
                if (dialogState.progress != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { dialogState.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = ProgressIndicatorDefaults.linearColor,
                        trackColor = ProgressIndicatorDefaults.linearTrackColor,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )
                }
            }
        },
        confirmButton = {
            if (dialogState.progress == null) {
                Button(onClick = { viewModel.dismissDialog() }) {
                    Text("OK")
                }
            }
        }
    )
}