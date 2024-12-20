package com.example.diplomski.views

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "register") {
        composable("login") { LoginScreen(navController) { } }
        composable("register") { RegisterScreen(navController) {  } }
        composable("client_home") { HomeScreen() }
        composable("admin_home") { AdminHomeScreen(navController)}
        composable("add_quiz") { AddQuizScreen(navController) }
    }
}