package com.lucas.predictaapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

/**
 * Canales de notificación (Android 8+). Agrupar por propósito deja que el usuario
 * silencie categorías desde Ajustes de Android sin perder las demás.
 */
object NotificationChannels {
    const val COBROS = "cobros"          // suscripciones + gastos fijos por vencer
    const val PATRONES = "patrones"      // picos de categoría, hábitos
    const val RECORDATORIOS = "recordatorios" // te olvidaste de registrar
    const val RESUMENES = "resumenes"    // resumen semanal

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val channels = listOf(
            NotificationChannel(
                COBROS, "Cobros y vencimientos", NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Suscripciones y gastos fijos por vencer" },
            NotificationChannel(
                PATRONES, "Patrones de gasto", NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Picos de gasto y hábitos detectados" },
            NotificationChannel(
                RECORDATORIOS, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Cuando hace varios días que no registrás gastos" },
            NotificationChannel(
                RESUMENES, "Resúmenes", NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Tu resumen semanal de gastos" },
        )
        manager.createNotificationChannels(channels)
    }
}
