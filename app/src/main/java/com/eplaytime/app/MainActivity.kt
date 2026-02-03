package com.eplaytime.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.eplaytime.app.ui.screens.HomeScreen
import com.eplaytime.app.ui.screens.SplashScreen
import com.eplaytime.app.ui.theme.PlayTimeTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eplaytime.app.ui.viewmodel.MusicViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val PERMISSION_CODE = 100
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // LAYER 1: THE "GOD" BRAIN
            // Initialize ONCE inside setContent
            val sharedViewModel: MusicViewModel = hiltViewModel()
            
            PlayTimeTheme {
                MainScreen(sharedViewModel = sharedViewModel)
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    sharedViewModel: MusicViewModel
) {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(
            onSplashComplete = { showSplash = false }
        )
    } else {
        MainNavigation(sharedViewModel)
    }
}

@Composable
private fun MainNavigation(sharedViewModel: MusicViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = sharedViewModel,
                onNavigateToScheduler = { navController.navigate("scheduler") },
                onNavigateToPlayer = { navController.navigate("player") },
                onNavigateToAbout = { navController.navigate("about") },
                onNavigateToQueue = { navController.navigate("queue") },
                onNavigateToFolder = { path -> 
                    val encodedPath = Uri.encode(path)
                    navController.navigate("folders?path=$encodedPath") 
                }
            )
        }

        composable("scheduler") {
            com.eplaytime.app.ui.screens.SchedulerScreen(
                onNavigateBack = { navController.popBackStack() },
                musicViewModel = sharedViewModel
            )
        }

        composable("player") {
            com.eplaytime.app.ui.screens.PlayerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToQueue = { navController.navigate("queue") },
                viewModel = sharedViewModel
            )
        }

        composable("folders?path={path}", 
            arguments = listOf(androidx.navigation.navArgument("path") { 
                nullable = true 
                defaultValue = null 
                type = androidx.navigation.NavType.StringType
            })
        ) { backStackEntry ->
            val path = backStackEntry.arguments?.getString("path")
            com.eplaytime.app.ui.screens.FolderScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = sharedViewModel,
                initialFolderPath = path
            )
        }

        composable("about") {
            com.eplaytime.app.ui.screens.AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("queue") {
            com.eplaytime.app.ui.screens.QueueScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = sharedViewModel
            )
        }
    }
}