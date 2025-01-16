package com.example.diplomski.views

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.diplomski.views.admin.AcceptedTeamsScreen
import com.example.diplomski.views.admin.AddQuizScreen
import com.example.diplomski.views.admin.AdminHomeScreen
import com.example.diplomski.views.admin.AdminProfileScreen
import com.example.diplomski.views.admin.EditQuizScreen
import com.example.diplomski.views.admin.QuizRegistrationsScreen
import com.example.diplomski.views.client.ClientProfileScreen
import com.example.diplomski.views.client.ClientRegistrationsScreen
import com.example.diplomski.views.client.HomeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentBackStack = navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack.value?.destination?.route

    val showBottomBar = when (currentDestination) {
        "client_home", "admin_home", "add_quiz", "client_profile", "admin_profile", "client_registrations" -> true
        else -> false
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                when {
                    currentDestination?.startsWith("client") == true -> {
                        BottomNavigationBar(navController = navController, items = getUserBottomNavItems())
                    }
                    currentDestination?.startsWith("admin") == true -> {
                        BottomNavigationBar(navController = navController, items = getAdminBottomNavItems())
                    }
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
            composable("client_profile") { ClientProfileScreen(navController) }
            composable("admin_profile") { AdminProfileScreen(navController) }
            composable("client_registrations") { ClientRegistrationsScreen() }
            composable("quiz_registrations/{quizId}/{quizName}/{quizDateTime}") { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString("quizId").orEmpty()
                val quizName = backStackEntry.arguments?.getString("quizName").orEmpty()
                val quizDateTime = backStackEntry.arguments?.getString("quizDateTime").orEmpty()
                QuizRegistrationsScreen(navController, quizId, quizName, quizDateTime)
            }
            composable("edit_quiz/{quizId}") { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString("quizId").orEmpty()
                EditQuizScreen(navController, quizId)
            }
            composable("accepted_teams/{quizId}") { backStackEntry ->
                val quizId = backStackEntry.arguments?.getString("quizId").orEmpty()
                AcceptedTeamsScreen(navController, quizId)
            }
        }
    }
}
