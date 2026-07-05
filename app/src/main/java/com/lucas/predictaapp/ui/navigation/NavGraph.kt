package com.lucas.predictaapp.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lucas.predictaapp.features.chat.ChatScreen
import com.lucas.predictaapp.features.dashboard.DashboardScreen
import com.lucas.predictaapp.features.fixedexpenses.FixedExpensesScreen
import com.lucas.predictaapp.features.notifications.NotificationsScreen
import com.lucas.predictaapp.features.profile.CategoriesScreen
import com.lucas.predictaapp.features.profile.ProfileScreen
import com.lucas.predictaapp.features.subscriptions.SubscriptionsScreen
import com.lucas.predictaapp.features.transactions.TransactionsScreen
import androidx.compose.runtime.getValue

private const val TRANSITION_MS = 280

private val rootRoutes = setOf(
    Screen.Dashboard.route,
    Screen.Transactions.route,
    Screen.Chat.route,
    Screen.Profile.route,
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isRootToRoot(): Boolean =
    initialState.destination.route in rootRoutes &&
        targetState.destination.route in rootRoutes

private fun tabEnter(): EnterTransition =
    fadeIn(animationSpec = tween(TRANSITION_MS))

private fun tabExit(): ExitTransition =
    fadeOut(animationSpec = tween(TRANSITION_MS))

private fun detailEnter(): EnterTransition =
    slideInHorizontally(tween(TRANSITION_MS)) { full -> full / 6 } +
        fadeIn(tween(TRANSITION_MS))

private fun detailExit(): ExitTransition =
    fadeOut(tween(TRANSITION_MS / 2))

private fun detailPopEnter(): EnterTransition =
    fadeIn(tween(TRANSITION_MS))

private fun detailPopExit(): ExitTransition =
    slideOutHorizontally(tween(TRANSITION_MS)) { full -> full / 6 } +
        fadeOut(tween(TRANSITION_MS))

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
        enterTransition = { if (isRootToRoot()) tabEnter() else detailEnter() },
        exitTransition = { if (isRootToRoot()) tabExit() else detailExit() },
        popEnterTransition = { if (isRootToRoot()) tabEnter() else detailPopEnter() },
        popExitTransition = { if (isRootToRoot()) tabExit() else detailPopExit() },
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(Screen.Transactions.route) {
            TransactionsScreen()
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
        composable(Screen.Categories.route) {
            CategoriesScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.FixedExpenses.route) {
            FixedExpensesScreen(
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
