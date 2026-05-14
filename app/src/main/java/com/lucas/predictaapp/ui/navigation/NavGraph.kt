package com.lucas.predictaapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lucas.predictaapp.features.chat.ChatScreen
import com.lucas.predictaapp.features.dashboard.DashboardScreen
import com.lucas.predictaapp.features.notifications.NotificationsScreen
import com.lucas.predictaapp.features.permito.PermitoScreen
import com.lucas.predictaapp.features.profile.ProfileScreen
import com.lucas.predictaapp.features.subscriptions.SubscriptionsScreen
import androidx.compose.runtime.getValue

@Composable
fun PredictaNavGraph(
    navController: NavHostController,
    onSignOut: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier,
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(Screen.Permito.route) {
            PermitoScreen()
        }
        composable(Screen.Chat.route) {
            ChatScreen()
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigate = { route -> navController.navigate(route) },
                onSignOut = onSignOut,
            )
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Subscriptions.route) {
            SubscriptionsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
fun rememberCurrentRoute(navController: NavHostController): String {
    val backStackEntry by navController.currentBackStackEntryAsState()
    return backStackEntry?.destination?.route ?: Screen.Dashboard.route
}
