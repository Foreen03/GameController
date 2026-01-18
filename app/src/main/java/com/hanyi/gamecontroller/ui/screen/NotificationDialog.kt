package com.hanyi.gamecontroller.ui.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hanyi.gamecontroller.domain.model.NotificationDialogState
import com.hanyi.gamecontroller.ui.MainViewModel

@Composable
fun NotificationDialog(viewModel: MainViewModel, dialogState: NotificationDialogState) {

    AlertDialog(
        onDismissRequest = { viewModel.dismissDialog() },
        title = { Text(text = dialogState.title) },
        text = { Text(text = dialogState.message) },
        confirmButton = {
            Button(onClick = { viewModel.dismissDialog() }) {
                Text("OK")
            }
        }
    )
}