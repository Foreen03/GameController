package com.hanyi.gamecontroller.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanyi.gamecontroller.domain.model.BleConnectionState

@Composable
fun ConnectionStatusCard(connectionState: BleConnectionState) {
    val (statusText, statusColor) = when (connectionState) {
        BleConnectionState.DISCONNECTED -> "Disconnected" to Color(0xFFE53935)
        BleConnectionState.CONNECTING -> "Connecting..." to Color(0xFFFFB300)
        BleConnectionState.CONNECTED -> "Connected" to Color(0xFF43A047)
        BleConnectionState.DISCONNECTING -> "Disconnecting" to Color(0XFFFF6F00)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor
        )
    ) {
        Text(
            text = statusText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}