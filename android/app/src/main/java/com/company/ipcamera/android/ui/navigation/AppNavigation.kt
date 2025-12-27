package com.company.ipcamera.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.company.ipcamera.android.ui.screens.camera.*
import com.company.ipcamera.android.ui.screens.events.*
import com.company.ipcamera.android.ui.screens.recordings.RecordingsScreen
import com.company.ipcamera.android.ui.screens.settings.SettingsScreen
import com.company.ipcamera.android.ui.screens.license.LicenseScreen
import com.company.ipcamera.android.ui.screens.video.VideoViewScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "camera_list"
    ) {
        composable("camera_list") {
            CameraListScreen(
                onCameraClick = { cameraId ->
                    navController.navigate("camera_detail/$cameraId")
                },
                onAddCameraClick = {
                    navController.navigate("camera_add")
                }
            )
        }

        composable(
            route = "camera_detail/{cameraId}",
            arguments = listOf(navArgument("cameraId") { type = NavType.StringType })
        ) { backStackEntry ->
            val cameraId = backStackEntry.arguments?.getString("cameraId") ?: ""
            CameraDetailScreen(
                cameraId = cameraId,
                onBackClick = {
                    navController.popBackStack()
                },
                onViewVideo = {
                    navController.navigate("video_view/$cameraId")
                }
            )
        }

        composable("camera_add") {
            CameraAddScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "video_view/{cameraId}",
            arguments = listOf(navArgument("cameraId") { type = NavType.StringType })
        ) { backStackEntry ->
            val cameraId = backStackEntry.arguments?.getString("cameraId") ?: ""
            VideoViewScreen(
                cameraId = cameraId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("recordings") {
            RecordingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onRecordingClick = { recordingId ->
                    navController.navigate("recording_playback/$recordingId")
                }
            )
        }

        composable(
            route = "recording_playback/{recordingId}",
            arguments = listOf(navArgument("recordingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recordingId = backStackEntry.arguments?.getString("recordingId") ?: ""
            // TODO: Implement RecordingPlaybackScreen
            RecordingsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onRecordingClick = { }
            )
        }

        composable("events") {
            EventsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onEventClick = { eventId ->
                    navController.navigate("event_detail/$eventId")
                }
            )
        }

        composable(
            route = "event_detail/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            EventDetailScreen(
                eventId = eventId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("license") {
            LicenseScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

