package com.hanyi.gamecontroller.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
){

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
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(gamepads) { gp ->
                Card(modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
                    .clickable { onSelectGamepad(gp) })
                {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = gp.gamepad.name)
                        Text(text = gp.gamepad.description)
                    }
                }
            }
        }
    }

}