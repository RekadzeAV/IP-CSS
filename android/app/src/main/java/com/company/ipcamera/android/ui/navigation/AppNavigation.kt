package com.company.ipcamera.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.company.ipcamera.android.ui.screens.camera.CameraListScreen
import com.company.ipcamera.android.ui.screens.camera.CameraDetailScreen

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

        composable("camera_detail/{cameraId}") { backStackEntry ->
            val cameraId = backStackEntry.arguments?.getString("cameraId") ?: ""
            CameraDetailScreen(
                cameraId = cameraId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("camera_add") {
            // TODO: Implement add camera screen
            CameraListScreen(
                onCameraClick = { },
                onAddCameraClick = { }
            )
        }
    }
}

