package com.example.mavlinkapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
