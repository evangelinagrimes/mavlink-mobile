package com.example.mavlinkapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mavlinkapplication.theme.ArduTargetTheme
import com.example.mavlinkapplication.ui.params.ParamScreen
import com.example.mavlinkapplication.ui.status.StatusScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Fullscreen: this is a field GCS, not a phone app — every pixel of the HUD
        // matters more than the status bar. Swipe from an edge to bring bars back briefly.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            ArduTargetTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "status") {
                    composable("status") {
                        StatusScreen(onNavigateToParams = { nav.navigate("params") })
                    }
                    composable("params") {
                        ParamScreen(onNavigateBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}
