package com.example.diplomski.views

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "register") {
        composable("login") { LoginScreen(navController) { navController.navigate("home") } }
        composable("register") { RegisterScreen(navController) { navController.navigate("login") } }
        composable("home") { HomeScreen() }
    }
}