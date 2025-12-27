package com.company.ipcamera.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.company.ipcamera.desktop.ui.navigation.Navigation
import com.company.ipcamera.desktop.ui.theme.AppTheme

@Composable
fun App() {
    AppTheme {
        Navigation(
            modifier = Modifier.fillMaxSize()
        )
    }
}

