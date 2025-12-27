package com.company.ipcamera.android.ui.screens.video

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.company.ipcamera.android.ui.components.ExoVideoPlayer
import com.company.ipcamera.android.ui.viewmodel.VideoViewViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoViewScreen(
    cameraId: String,
    onBackClick: () -> Unit,
    viewModel: VideoViewViewModel = koinViewModel(key = cameraId) { VideoViewViewModel(get(), cameraId) }
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.camera?.name ?: "Video View") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                uiState.camera == null -> {
                    Text(
                        text = "Camera not found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Video Player
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp)
                        ) {
                            if (uiState.rtspUrl != null) {
                                ExoVideoPlayer(
                                    videoUrl = uiState.rtspUrl,
                                    autoPlay = uiState.isPlaying,
                                    modifier = Modifier.fillMaxSize(),
                                    onError = { error ->
                                        // Обработка ошибки
                                    }
                                )
                            } else {
                                // Показываем placeholder, если URL еще не загружен
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoading) {
                                        CircularProgressIndicator()
                                    } else {
                                        Text("Loading stream URL...")
                                    }
                                }
                            }
                        }

                        // Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = { 
                                if (!uiState.streamActive) {
                                    viewModel.startStream()
                                }
                                viewModel.togglePlayback() 
                            }) {
                                Icon(
                                    if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (uiState.isPlaying) "Pause" else "Play"
                                )
                            }
                            IconButton(onClick = { 
                                viewModel.stopStream()
                                viewModel.toggleRecording() 
                            }) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = if (uiState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

