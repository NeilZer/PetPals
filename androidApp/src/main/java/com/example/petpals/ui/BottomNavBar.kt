package com.example.petpals.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.petpals.R

data class BottomNavItem(
    val screen: Screen,
    val icon: Int
)

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem(Screen.Feed, R.drawable.ic_feed),
        BottomNavItem(Screen.Map, R.drawable.ic_map),
        BottomNavItem(Screen.Profile, R.drawable.ic_profile),
        BottomNavItem(Screen.Statistics, R.drawable.ic_stats),
    )

    NavigationBar {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.screen.route,
                onClick = { navController.navigate(item.screen.route) },
                icon = { Icon(painterResource(id = item.icon), contentDescription = item.screen.title) },
                label = { Text(item.screen.title) }
            )
        }
    }
}
