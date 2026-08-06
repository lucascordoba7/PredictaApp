package com.lucas.predictaapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.core.content.IntentCompat
import androidx.navigation.compose.rememberNavController
import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.UserPreferencesRepository
import com.lucas.predictaapp.data.remote.SyncErrors
import com.lucas.predictaapp.features.onboarding.OnboardingScreen
import com.lucas.predictaapp.features.quickactions.ManualExpenseEntry
import com.lucas.predictaapp.features.quickactions.QuickActionsSheet
import com.lucas.predictaapp.ui.navigation.BottomNavigationBar
import com.lucas.predictaapp.ui.navigation.Screen
import com.lucas.predictaapp.ui.navigation.PredictaNavGraph
import com.lucas.predictaapp.ui.navigation.bottomNavScreens
import com.lucas.predictaapp.ui.navigation.rememberCurrentRoute
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleLaunchIntent(intent)
        setContent {
            PredictaTheme {
                PredictaRoot()
            }
        }
    }

    // launchMode singleTask: widget/shortcut/share con la app ya abierta entran por acá.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLaunchIntent(intent)
    }

    /** Traducción Intent → LaunchAction (widget, app shortcut o share de otra app). */
    private fun handleLaunchIntent(intent: Intent?) {
        intent ?: return
        val action = when {
            intent.action == Intent.ACTION_SEND && intent.type == "text/plain" ->
                intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { LaunchAction.SharedText(it) }

            intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true ->
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.let { LaunchAction.SharedImage(it) }

            else -> LaunchActions.fromExtra(intent.getStringExtra(LaunchActions.EXTRA_ACTION))
        } ?: return

        LaunchActions.set(action)
        // Evita re-disparar la acción si la Activity se recrea con el mismo Intent.
        intent.removeExtra(LaunchActions.EXTRA_ACTION)
        intent.action = null
    }
}

private sealed class AppState {
    object Loading : AppState()
    object Onboarding : AppState()
    object Main : AppState()
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PredictaRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var appState by remember { mutableStateOf<AppState>(AppState.Loading) }

    // Permiso de notificaciones (Android 13+). En versiones previas ya viene concedido.
    val notifPermission = rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    LaunchedEffect(Unit) {
        if (!notifPermission.status.isGranted) notifPermission.launchPermissionRequest()
    }

    LaunchedEffect(Unit) {
        // Sin cuenta abierta en este dispositivo va al onboarding, que ahora ofrece
        // entrar con un email existente o crear una cuenta nueva. Ya no se siembra
        // ningún usuario por default: eso hacía que cualquier instalación se apropiara
        // de los datos de la cuenta única.
        if (UserPreferencesRepository.restoreSession(context)) {
            UserPreferencesRepository.pullProfile(context)
            appState = AppState.Main
        } else {
            appState = AppState.Onboarding
        }
    }

    // Errores de sync a Supabase → Toast en pantalla (además del log en Logcat).
    LaunchedEffect(Unit) {
        SyncErrors.events.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
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
    var showManualEntry by remember { mutableStateOf(false) }

    fun navigate(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Acciones de lanzamiento (widget/shortcut/share/QuickActions): el scaffold
    // resuelve la carga manual acá y lleva al chat las que consume ChatScreen.
    LaunchedEffect(Unit) {
        LaunchActions.pending.collect { action ->
            when (action) {
                LaunchAction.ManualExpense -> {
                    LaunchActions.clear()
                    showManualEntry = true
                }
                LaunchAction.VoiceChat,
                LaunchAction.ScanTicket,
                is LaunchAction.SharedText,
                is LaunchAction.SharedImage,
                -> navigate(Screen.Chat.route) // ChatScreen consume la acción del flow

                null -> Unit
            }
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
                    profileScore = 0,
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
            onNavigate = { route ->
                when (route) {
                    LaunchActions.ACTION_MANUAL -> showManualEntry = true
                    LaunchActions.ACTION_SCAN -> {
                        LaunchActions.set(LaunchAction.ScanTicket)
                        navigate(Screen.Chat.route)
                    }
                    else -> navigate(route)
                }
            },
        )
    }

    if (showManualEntry) {
        ManualExpenseEntry(onDone = { showManualEntry = false })
    }
}
