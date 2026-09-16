package com.company.ipcamera.desktop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            Snackbar(
                action = snackbarData.visuals.actionLabel?.let { label ->
                    {
                        TextButton(onClick = { snackbarData.performAction() }) {
                            Text(label)
                        }
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text(snackbarData.visuals.message)
            }
        }
    )
}

@Composable
fun rememberAppSnackbarHostState(): SnackbarHostState {
    return remember { SnackbarHostState() }
}
