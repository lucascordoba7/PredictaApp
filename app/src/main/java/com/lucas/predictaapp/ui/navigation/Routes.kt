package com.lucas.predictaapp.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Hoy")
    object Permito : Screen("permito", "Permito")
    object Chat : Screen("chat", "Chat")
    object Profile : Screen("profile", "Yo")
    object Subscriptions : Screen("subscriptions", "Suscripciones")
    object Notifications : Screen("notifications", "Notificaciones")
    object Categories : Screen("categories", "Categorías")
}

val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Permito,
    Screen.Chat,
    Screen.Profile,
)
