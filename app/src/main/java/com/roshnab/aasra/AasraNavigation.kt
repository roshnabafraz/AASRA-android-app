package com.roshnab.aasra.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.roshnab.aasra.screens.DonationScreen
import com.roshnab.aasra.screens.HomeScreen
import com.roshnab.aasra.screens.ProfileScreen
import com.roshnab.aasra.screens.ReportScreen
import com.google.firebase.auth.FirebaseAuth
import com.roshnab.aasra.auth.AuthScreen

@Composable
fun AasraNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "auth") {

        // 1. AUTH SCREEN
        composable("auth") {
            AuthScreen(onAuthSuccess = {
                navController.navigate("home") {
                    popUpTo("auth") { inclusive = true }
                }
            })
        }

        // 2. HOME SCREEN
        composable("home") {
            HomeScreen(
                onReportClick = { lat, lng ->
                    navController.navigate("report/${lat.toFloat()}/${lng.toFloat()}")
                },
                onDonationClick = {
                    navController.navigate("donation")
                },
                onProfileClick = {
                    navController.navigate("profile")
                }
            )
        }

        // 3. DONATION SCREEN
        composable("donation") {
            DonationScreen(onBackClick = { navController.popBackStack() })
        }

        // 4. PROFILE SCREEN (Fixed Logout Logic)
        composable("profile") {
            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    // --- FIX IS HERE: SIGN OUT FIRST ---
                    try {
                        FirebaseAuth.getInstance().signOut()
                    } catch (e: Exception) {
                        // Prevents crash if Firebase isn't set up yet
                    }

                    // THEN Navigate to Auth and clear history
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 5. REPORT SCREEN
        composable(
            route = "report/{lat}/{lng}",
            arguments = listOf(
                navArgument("lat") { type = NavType.FloatType },
                navArgument("lng") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
            val lng = backStackEntry.arguments?.getFloat("lng")?.toDouble() ?: 0.0

            ReportScreen(
                latitude = lat,
                longitude = lng,
                onBackClick = { navController.popBackStack() },
                onSubmitClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}