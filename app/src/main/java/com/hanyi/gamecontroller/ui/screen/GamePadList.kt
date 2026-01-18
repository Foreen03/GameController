package com.hanyi.gamecontroller.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hanyi.gamecontroller.domain.model.GamepadConfig
import com.hanyi.gamecontroller.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePadList(
    viewModel: MainViewModel,
    onSelectGamepad: (GamepadConfig) -> Unit
) {
    val gamepads by viewModel.gamepads.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GamePad List") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(gamepads) { gp ->
                GamepadItem(
                    gamepadConfig = gp,
                    onClick = { onSelectGamepad(gp) }
                )
            }
        }
    }
}

