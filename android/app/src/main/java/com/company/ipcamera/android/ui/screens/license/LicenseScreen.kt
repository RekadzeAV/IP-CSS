package com.company.ipcamera.android.ui.screens.license

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.ipcamera.android.ui.viewmodel.LicenseViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(
    onBackClick: () -> Unit,
    viewModel: LicenseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("License") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                uiState.license?.let { license ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "License Type: ${license.type.name}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Status: ${license.status.name}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Max Cameras: ${license.maxCameras}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Max Users: ${license.maxUsers}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            license.expiresAt?.let {
                                Text(
                                    text = "Expires: ${java.util.Date(it)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } ?: run {
                    Text("No license found")
                }

                Divider()

                OutlinedTextField(
                    value = uiState.activationKey,
                    onValueChange = viewModel::updateActivationKey,
                    label = { Text("Activation Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = { viewModel.activateLicense() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isActivating && uiState.activationKey.isNotBlank()
                ) {
                    if (uiState.isActivating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Activate License")
                }

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

