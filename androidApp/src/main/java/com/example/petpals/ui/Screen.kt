// Screen.kt
package com.example.petpals.ui


// שמות בעברית לתצוגה, מסלולים (routes) נשארים באנגלית
sealed class Screen(val route: String, val title: String) {
    object Login       : Screen("login",        "התחברות")
    object SignUp      : Screen("signup",       "הרשמה")
    object Feed        : Screen("feed",         "פיד")
    object Map         : Screen("map",          "מפה")
    object Profile     : Screen("profile",      "פרופיל")
    object EditProfile : Screen("edit_profile", "עריכת פרופיל")
    object NewPost     : Screen("new_post",     "פוסט חדש")
    object PostDetail  : Screen("post_detail",  "פוסט")
    object Statistics  : Screen("stats",        "סטטיסטיקה")
}

