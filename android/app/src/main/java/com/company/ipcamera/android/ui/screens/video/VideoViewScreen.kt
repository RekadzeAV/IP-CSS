package com.company.ipcamera.android.ui.screens.video

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.company.ipcamera.android.media.PictureInPictureManager
import com.company.ipcamera.android.ui.components.ExoVideoPlayer
import com.company.ipcamera.android.ui.viewmodel.StreamProtocol
import com.company.ipcamera.android.ui.viewmodel.StreamQuality
import com.company.ipcamera.android.ui.viewmodel.VideoViewViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoViewScreen(
    cameraId: String,
    onBackClick: () -> Unit,
    viewModel: VideoViewViewModel = koinViewModel(
        key = cameraId,
        parameters = { parametersOf(cameraId) }
    )
) {
    val qualityOptions = remember {
        listOf(
            StreamQuality.LOW to "360p",
            StreamQuality.MEDIUM to "720p",
            StreamQuality.HIGH to "1080p",
            StreamQuality.ULTRA to "1080p+",
            StreamQuality.QHD1440 to "2K",
            StreamQuality.UHD4K to "4K",
            StreamQuality.QHD1440_H264 to "2K H264",
            StreamQuality.UHD4K_H264 to "4K H264"
        )
    }
    val uiState by viewModel.uiState.collectAsState()
    val localFrameAnalyticsEnabled = remember(viewModel) {
        viewModel.isLocalFrameAnalyticsEnabled()
    }
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val pipManager = remember { activity?.let { PictureInPictureManager(it) } }
    val isPipSupported = pipManager?.isPictureInPictureSupported() ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.camera?.name ?: "Video View") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isPipSupported && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        IconButton(
                            onClick = {
                                pipManager?.enterPictureInPictureMode()
                            }
                        ) {
                            Icon(
                                Icons.Default.PictureInPicture,
                                contentDescription = "Enter Picture-in-Picture"
                            )
                        }
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.streamProtocol == StreamProtocol.RTSP,
                                onClick = { viewModel.setStreamProtocol(StreamProtocol.RTSP) },
                                label = { Text("RTSP") }
                            )
                            FilterChip(
                                selected = uiState.streamProtocol == StreamProtocol.HLS,
                                onClick = { viewModel.setStreamProtocol(StreamProtocol.HLS) },
                                label = { Text("HLS") }
                            )
                        }
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(end = 16.dp)
                        ) {
                            items(qualityOptions) { (quality, label) ->
                                FilterChip(
                                    selected = uiState.streamQuality == quality,
                                    onClick = { viewModel.setStreamQuality(quality) },
                                    label = { Text(label) }
                                )
                            }
                        }
                        // Video Player
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(16.dp)
                        ) {
                            val liveUrl = when (uiState.streamProtocol) {
                                StreamProtocol.HLS -> uiState.hlsUrl
                                StreamProtocol.RTSP -> uiState.rtspUrl
                            }
                            if (liveUrl != null) {
                                key(uiState.streamProtocol, liveUrl) {
                                    ExoVideoPlayer(
                                        videoUrl = liveUrl,
                                        hlsFallbackUrl = uiState.hlsUrl?.takeIf { uiState.streamProtocol == StreamProtocol.RTSP },
                                        autoPlay = uiState.isPlaying,
                                        modifier = Modifier.fillMaxSize(),
                                        enableBackgroundPlayback = true,
                                        mediaTitle = uiState.camera?.name ?: "Camera Stream",
                                        mediaSubtitle = "Live View",
                                        localFrameAnalyticsEnabled = localFrameAnalyticsEnabled,
                                        onLocalAnalyticsRgbFrame = if (localFrameAnalyticsEnabled) {
                                            { w, h, rgb -> viewModel.submitLocalAnalyticsFrame(w, h, rgb) }
                                        } else {
                                            null
                                        },
                                        onError = { error ->
                                            // Обработка ошибки
                                        }
                                    )
                                }
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

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Observation Summary",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Computed px/m: ${
                                        uiState.observationSummary?.computedPixelsPerMeter?.let { String.format("%.2f", it) } ?: "N/A"
                                    }"
                                )
                                Text(
                                    text = "Target min px/m: ${
                                        uiState.observationSummary?.targetPixelsPerMeterMin?.let { String.format("%.2f", it) } ?: "N/A"
                                    }"
                                )
                                val warning = uiState.observationSummary?.lowResolutionWarning == true
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = {
                                        Text(
                                            if (warning) "Low resolution warning" else "Resolution OK"
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

