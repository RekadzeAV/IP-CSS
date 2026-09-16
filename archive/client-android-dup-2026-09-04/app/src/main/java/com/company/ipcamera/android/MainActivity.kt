package com.company.ipcamera.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.company.ipcamera.android.ui.screens.*
import com.company.ipcamera.android.ui.theme.IPCameraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IPCameraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(onNavigate = navController::navigate) }
        composable("cameras") { CameraListScreen(onNavigate = navController::navigate) }
        composable("camera/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            CameraDetailScreen(cameraId = id, onNavigate = navController::navigate)
        }
        composable("events") { EventsScreen() }
        composable("settings") { SettingsScreen() }
        composable("login") { LoginScreen(onNavigate = navController::navigate) }
    }
}
