package com.roshnab.aasra

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.roshnab.aasra.auth.AuthScreen
import com.roshnab.aasra.screens.HomeScreen

@Composable
fun AasraNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "auth") {

        composable("auth") {
            AuthScreen(
                onAuthSuccess = {
                    // Navigate to Home when login succeeds
                    navController.navigate("home") {
                        // This clears the "Back Stack" so the user
                        // cannot go back to the Login screen by pressing Back.
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen()
        }
    }
}