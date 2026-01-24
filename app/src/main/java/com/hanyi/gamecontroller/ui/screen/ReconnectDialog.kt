package com.hanyi.gamecontroller.ui.screen

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.hanyi.gamecontroller.ui.MainViewModel

@Composable
fun ReconnectDialog(viewModel: MainViewModel) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(text = "Disconnected") },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(text = "Reconnecting to PC Server...")
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.disconnect() }) {
                Text("Cancel")
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}