package com.example.petpals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.petpals.ui.BottomNavBar
import com.example.petpals.ui.NavGraph
import com.example.petpals.ui.Screen
import com.example.petpals.ui.theme.PetPalsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PetPalsTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

                    Scaffold(
                        bottomBar = {
                            // מציגים את ה־BottomNavBar רק אם לא נמצאים במסכי התחברות/הרשמה
                            if (currentRoute !in listOf(Screen.Login.route, Screen.SignUp.route)) {
                                BottomNavBar(navController)
                            }
                        }
                    ) { innerPadding ->
                        NavGraph(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
