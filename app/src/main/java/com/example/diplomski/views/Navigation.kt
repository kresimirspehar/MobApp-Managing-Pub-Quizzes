package com.example.diplomski.views

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentBackStack = navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack.value?.destination?.route

    val showBottomBar = when (currentDestination) {
        "client_home", "admin_home", "add_quiz", "client_profile", "admin_profile" -> true
        else -> false
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                if (currentDestination?.startsWith("client") == true) {
                    BottomNavigationBar(navController = navController, items = getUserBottomNavItems())
                } else if (currentDestination?.startsWith("admin") == true) {
                    BottomNavigationBar(navController = navController, items = getAdminBottomNavItems())
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "register", // Početna destinacija je registracija
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("register") { RegisterScreen(navController) { } }
            composable("login") { LoginScreen(navController) { } }
            composable("client_home") { HomeScreen() }
            composable("admin_home") { AdminHomeScreen(navController) }
            composable("add_quiz") { AddQuizScreen(navController) }
            composable("client_profile") { ProfileScreen() }
            composable("admin_profile") { ProfileScreen() }
        }
    }
}
