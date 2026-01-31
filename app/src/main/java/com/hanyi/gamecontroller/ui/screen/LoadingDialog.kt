package com.hanyi.gamecontroller.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingDialog(
    progress: Float,
    onDismissRequest: () -> Unit = {} // Default to empty to block dismissal
) {
    if (progress > 0f && progress < 1f) {
        AlertDialog(
            onDismissRequest = {
                // Intentionally empty: Prevent user from dismissing
                // the dialog by clicking outside while transfer is active.
            },
            title = { Text(text = "Downloading Layout...") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = ProgressIndicatorDefaults.linearColor,
                        trackColor = ProgressIndicatorDefaults.linearTrackColor,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Please wait while the layout is updated.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                // No buttons needed for an automatic progress dialog
            },
            dismissButton = {
                // No cancel button needed
            }
        )
    }
}