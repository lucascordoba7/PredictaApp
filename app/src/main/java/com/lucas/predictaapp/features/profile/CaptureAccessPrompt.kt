package com.lucas.predictaapp.features.profile

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.lucas.predictaapp.capture.PaymentNotificationListener
import com.lucas.predictaapp.capture.PaymentScreenReader
import com.lucas.predictaapp.ui.components.PredictaDialog
import kotlinx.coroutines.delay

/** Lo que puede frenar la captura automática (mismos nombres que en Perfil). */
private enum class CaptureProblem(val title: String, val text: String, val settingsAction: String) {
    NOTIFICATIONS_OFF(
        title = "Captura automática apagada",
        text = "Predicta no puede leer los avisos de Mercado Pago y los bancos, así que esos pagos " +
            "no se van a cargar solos. Activá Predicta en Acceso a notificaciones.",
        settingsAction = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
    ),
    SCREEN_OFF(
        title = "Lectura de comprobantes apagada",
        text = "Las transferencias que hagas desde Mercado Pago o Santander no se van a cargar solas. " +
            "Activá \"Predicta · comprobantes\" en Accesibilidad.",
        settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
    ),
    SCREEN_STUCK(
        title = "Lectura de comprobantes trabada",
        text = "Figura activada, pero Android no la está corriendo (pasa después de actualizar o " +
            "forzar la detención de la app). En Accesibilidad, apagá \"Predicta · comprobantes\" " +
            "y volvé a prenderla.",
        settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
    ),
}

private fun captureProblems(context: Context): List<CaptureProblem> = buildList {
    if (!PaymentNotificationListener.isEnabled(context)) {
        add(CaptureProblem.NOTIFICATIONS_OFF)
    } else {
        // El listener sí se puede reconectar sin el usuario.
        PaymentNotificationListener.rebindIfStuck(context)
    }
    when {
        !PaymentScreenReader.isEnabled(context) -> add(CaptureProblem.SCREEN_OFF)
        !PaymentScreenReader.isRunning -> add(CaptureProblem.SCREEN_STUCK)
    }
}

/**
 * Avisa al abrir la app si la captura está apagada o trabada: el sistema o el usuario pueden
 * desactivarla sin que la app se entere, y Ajustes puede mostrar la lectura de comprobantes
 * activada aunque Android no la esté corriendo. Sin esto los pagos dejan de cargarse en
 * silencio. Se re-chequea al volver de Ajustes (con un margen para que el sistema conecte
 * los servicios). "Ahora no" lo calla hasta la próxima apertura.
 */
@Composable
fun CaptureAccessPrompt() {
    val context = LocalContext.current
    var dismissed by rememberSaveable { mutableStateOf(false) }
    var problems by remember { mutableStateOf(emptyList<CaptureProblem>()) }
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    LaunchedEffect(resumes) {
        if (resumes == 0) return@LaunchedEffect
        delay(SERVICE_BIND_GRACE_MS)
        problems = captureProblems(context)
    }

    val problem = problems.firstOrNull()
    if (dismissed || problem == null) return
    PredictaDialog(
        title = problem.title,
        text = problem.text,
        confirmLabel = if (problem == CaptureProblem.SCREEN_STUCK) "Ir a Accesibilidad" else "Activar",
        dismissLabel = "Ahora no",
        onConfirm = { context.startActivity(Intent(problem.settingsAction)) },
        onDismiss = { dismissed = true },
    )
}

private const val SERVICE_BIND_GRACE_MS = 1500L
