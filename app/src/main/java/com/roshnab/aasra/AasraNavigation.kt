package com.roshnab.aasra

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel // Import for viewModel()
import com.google.firebase.auth.FirebaseAuth
import com.roshnab.aasra.auth.AuthScreen
import com.roshnab.aasra.data.ProfileViewModel
import com.roshnab.aasra.screens.DonationScreen
import com.roshnab.aasra.screens.EditProfileScreen
import com.roshnab.aasra.screens.HelpSupportScreen
import com.roshnab.aasra.screens.HomeScreen
import com.roshnab.aasra.screens.LocationPickerScreen
import com.roshnab.aasra.screens.ProfileScreen
import com.roshnab.aasra.screens.ReportScreen
import com.roshnab.aasra.screens.SplashScreen

@Composable
fun AasraNavigation(
    isDarkTheme: Boolean = false,
    onThemeChanged: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen(navController = navController)
        }

        composable("auth") {
            AuthScreen(onAuthSuccess = {
                navController.navigate("home") {
                    popUpTo("auth") { inclusive = true }
                }
            })
        }

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

        composable("donation") {
            DonationScreen(onBackClick = { navController.popBackStack() })
        }

        composable("profile") {
            val profileViewModel: ProfileViewModel = viewModel()

            ProfileScreen(
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    try { FirebaseAuth.getInstance().signOut() } catch (e: Exception) {}
                    navController.navigate("auth") { popUpTo(0) { inclusive = true } }
                },
                onAddLocationClick = {
                    navController.navigate("location_picker")
                },
                onEditProfileClick = { navController.navigate("edit_profile") },
                isDarkTheme = isDarkTheme,
                onThemeChanged = onThemeChanged,
                onSupportClick = { navController.navigate("help_support") }, // <--- Add this
                viewModel = profileViewModel
            )
        }

        composable("help_support") {
            HelpSupportScreen(onBackClick = { navController.popBackStack() })
        }

        composable("edit_profile") {
            val profileViewModel: ProfileViewModel = viewModel(
                viewModelStoreOwner = navController.getBackStackEntry("profile")
            )

            EditProfileScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = profileViewModel
            )
        }

        composable("location_picker") {
            val profileViewModel: ProfileViewModel = viewModel(
                viewModelStoreOwner = navController.getBackStackEntry("profile")
            )

            LocationPickerScreen(
                onBackClick = { navController.popBackStack() },
                onLocationSelected = { name, lat, lng ->
                    profileViewModel.addSafeLocation(name, lat, lng)
                    navController.popBackStack()
                }
            )
        }

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