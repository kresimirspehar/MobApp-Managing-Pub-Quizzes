package com.example.diplomski.views
import com.example.diplomski.R

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: Int
)

@Composable
fun BottomNavigationBar(navController: NavController, items: List<BottomNavItem>) {
    NavigationBar {
        val currentBackStack = navController.currentBackStackEntryAsState()
        val currentDestination = currentBackStack.value?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.name
                    )
                },
                label = { Text(text = item.name) },
                selected = currentDestination == item.route,
                onClick = {
                    if (currentDestination != item.route) {
                        navController.navigate(item.route)
                    }
                }
            )
        }
    }
}

@Composable
fun getUserBottomNavItems(): List<BottomNavItem> {
    return listOf(
        BottomNavItem("Quizzes", "client_home", R.drawable.baseline_event_24),
        BottomNavItem("My Quizzes", "client_registrations", R.drawable.baseline_access_time_filled_24),
        BottomNavItem("Profile", "client_profile", R.drawable.baseline_person_24)

    )
}

@Composable
fun getAdminBottomNavItems(): List<BottomNavItem> {
    return listOf(
        BottomNavItem("Quizzes", "admin_home", R.drawable.baseline_event_24),
        BottomNavItem("Profile", "admin_profile", R.drawable.baseline_person_24)
    )
}