package com.example.petpals.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.petpals.ui.screens.*
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { navController.navigate(Screen.Feed.route) },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = { navController.navigate(Screen.Feed.route) }
            )
        }
        composable(Screen.Feed.route) { FeedScreen(navController) }
        composable(Screen.NewPost.route) { NewPostScreen(navController) }
        composable(Screen.Map.route) { MapScreen() }
        composable(Screen.Profile.route) { ProfileScreen(navController) }
        composable(Screen.EditProfile.route) {
            EditProfileScreen(onProfileUpdated = {
                navController.popBackStack(Screen.Profile.route, false)
            })
        }
        composable(Screen.Statistics.route) { StatisticsScreen(navController) }
    }
}
