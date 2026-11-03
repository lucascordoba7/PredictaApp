package com.lucas.predictaapp.features.profile

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.lucas.predictaapp.capture.PaymentNotificationListener
import com.lucas.predictaapp.capture.PaymentScreenReader
import com.lucas.predictaapp.ui.components.PredictaDialog

/** Los dos accesos que necesita la captura automática (mismos nombres que en Perfil). */
private enum class CaptureAccess(val title: String, val text: String, val settingsAction: String) {
    NOTIFICATIONS(
        title = "Captura automática apagada",
        text = "Predicta no puede leer los avisos de Mercado Pago y los bancos, así que esos pagos " +
            "no se van a cargar solos. Activá Predicta en Acceso a notificaciones.",
        settingsAction = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
    ),
    SCREEN(
        title = "Lectura de comprobantes apagada",
        text = "Las transferencias que hagas desde Mercado Pago o Santander no se van a cargar solas. " +
            "Activá \"Predicta · comprobantes\" en Accesibilidad.",
        settingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
    ),
    ;

    fun isEnabled(context: Context): Boolean = when (this) {
        NOTIFICATIONS -> PaymentNotificationListener.isEnabled(context)
        SCREEN -> PaymentScreenReader.isEnabled(context)
    }
}

/**
 * Avisa al abrir la app si alguno de los accesos de captura está apagado: el sistema o el
 * usuario pueden desactivarlos sin que la app se entere, y sin ellos los pagos dejan de
 * cargarse en silencio. Se re-chequea al volver de Ajustes, así que si faltaban los dos
 * pide el siguiente. "Ahora no" lo calla hasta la próxima apertura.
 */
@Composable
fun CaptureAccessPrompt() {
    val context = LocalContext.current
    var dismissed by rememberSaveable { mutableStateOf(false) }
    var missing by remember { mutableStateOf(emptyList<CaptureAccess>()) }
    LifecycleResumeEffect(Unit) {
        missing = CaptureAccess.entries.filterNot { it.isEnabled(context) }
        onPauseOrDispose { }
    }

    val access = missing.firstOrNull()
    if (dismissed || access == null) return
    PredictaDialog(
        title = access.title,
        text = access.text,
        confirmLabel = "Activar",
        dismissLabel = "Ahora no",
        onConfirm = { context.startActivity(Intent(access.settingsAction)) },
        onDismiss = { dismissed = true },
    )
}
