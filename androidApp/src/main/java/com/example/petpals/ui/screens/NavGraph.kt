package com.example.petpals.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.petpals.ui.screens.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth

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

        composable(Screen.Feed.route) {
            FeedScreen(navController)
        }

        composable(Screen.NewPost.route) {
            NewPostScreen(navController)
        }

        // מסך מפה רגיל - מעביר navController
        composable(Screen.Map.route) {
            MapScreen(navController = navController)
        }

        // מסך מפה עם מיקום ספציפי
        composable(
            route = "${Screen.Map.route}?lat={lat}&lng={lng}",
            arguments = listOf(
                navArgument("lat") {
                    type = NavType.FloatType
                    defaultValue = 0f
                },
                navArgument("lng") {
                    type = NavType.FloatType
                    defaultValue = 0f
                }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getFloat("lat") ?: 0f
            val lng = backStackEntry.arguments?.getFloat("lng") ?: 0f
            val selectedLocation = if (lat != 0f && lng != 0f) {
                LatLng(lat.toDouble(), lng.toDouble())
            } else null

            MapScreen(navController = navController, selectedPostLocation = selectedLocation)
        }

        composable(Screen.Profile.route) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId != null) {
                ProfileScreen(navController = navController, userId = currentUserId)
            } else {
                // טיפול במקרה שאין משתמש מחובר, למשל ניווט ל־login
                navController.navigate(Screen.Login.route)
            }
        }



        composable(Screen.EditProfile.route) {
            EditProfileScreen(onProfileUpdated = {
                navController.popBackStack(Screen.Profile.route, false)
            })
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen(navController)
        }

        // מסך פרטי פוסט עם פרמטר postId
        composable(
            route = "${Screen.PostDetail.route}/{postId}",
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PostDetailScreen(navController, postId)
        }
    }
}
