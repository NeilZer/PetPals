// Screen.kt
package com.example.petpals.ui

sealed class Screen(val route: String, val title: String) {
    object Login : Screen("login", "Login")
    object SignUp : Screen("signup", "Sign Up")
    object Feed : Screen("feed", "Feed")
    object NewPost : Screen("newPost", "New Post")
    object Map : Screen("map", "Map")
    object Profile : Screen("profile", "Profile")
    object EditProfile : Screen("editProfile", "Edit Profile") // ✅ הוספנו
    object Statistics : Screen("statistics", "Statistics")
    object PostDetail : Screen("postDetail", "Post Detail")

}
