package com.example.petpals.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun StatisticsScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Monthly Statistics", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // כאן אפשר להוסיף גרפים וסטטיסטיקות אמיתיות
        Text("Total Walks: 12")
        Text("Total Distance: 25 km")
        Text("Most Active Pet: Rex 🐶")
    }
}
