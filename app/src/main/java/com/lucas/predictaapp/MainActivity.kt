package com.lucas.predictaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.UserPreferencesRepository
import com.lucas.predictaapp.features.onboarding.OnboardingScreen
import com.lucas.predictaapp.features.quickactions.QuickActionsSheet
import com.lucas.predictaapp.ui.navigation.BottomNavigationBar
import com.lucas.predictaapp.ui.navigation.PredictaNavGraph
import com.lucas.predictaapp.ui.navigation.bottomNavScreens
import com.lucas.predictaapp.ui.navigation.rememberCurrentRoute
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PredictaTheme {
                PredictaRoot()
            }
        }
    }
}

private sealed class AppState {
    object Loading : AppState()
    object Onboarding : AppState()
    object Main : AppState()
}

@Composable
fun PredictaRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var appState by remember { mutableStateOf<AppState>(AppState.Loading) }

    LaunchedEffect(Unit) {
        val done = UserPreferencesRepository.isOnboardingDone(context).first()
        appState = if (done) AppState.Main else AppState.Onboarding
    }

    fun signOut() {
        scope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(context).clearAllTables()
            }
            UserPreferencesRepository.reset(context)
            appState = AppState.Onboarding
        }
    }

    AnimatedContent(
        targetState = appState,
        transitionSpec = {
            fadeIn(tween(400)) togetherWith fadeOut(tween(300))
        },
        label = "app_root",
    ) { state ->
        when (state) {
            is AppState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PredictaColors.charcoal),
            )

            is AppState.Onboarding -> OnboardingScreen(
                onComplete = { appState = AppState.Main },
            )

            is AppState.Main -> PredictaAppScaffold(onSignOut = ::signOut)
        }
    }
}

@Composable
fun PredictaAppScaffold(onSignOut: () -> Unit = {}) {
    val navController = rememberNavController()
    val currentRoute = rememberCurrentRoute(navController)
    val showBottomBar = currentRoute in bottomNavScreens.map { it.route }
    var showQuickActions by remember { mutableStateOf(false) }

    fun navigate(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(PredictaColors.charcoal),
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { navigate(it) },
                    onFabClick = { showQuickActions = true },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            PredictaNavGraph(navController = navController, onSignOut = onSignOut)
        }
    }

    if (showQuickActions) {
        QuickActionsSheet(
            onDismiss = { showQuickActions = false },
            onNavigate = { route -> navigate(route) },
        )
    }
}
