package com.lucas.predictaapp.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Programa dos corridas diarias del motor de notificaciones:
 *  - MAÑANA (~10:00): cobros, gastos fijos por vencer, resumen semanal, picos de categoría.
 *  - NOCHE  (~20:30): "te olvidaste de registrar" y el recordatorio de hábito.
 */
object NotificationScheduler {
    private const val MORNING_WORK = "notif_morning"
    private const val EVENING_WORK = "notif_evening"

    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            MORNING_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicAt(10, 0, NotifSlot.MORNING),
        )
        wm.enqueueUniquePeriodicWork(
            EVENING_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicAt(20, 30, NotifSlot.EVENING),
        )
    }

    private fun periodicAt(hour: Int, minute: Int, slot: NotifSlot) =
        PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(minutesUntil(hour, minute), TimeUnit.MINUTES)
            .setInputData(workDataOf(NotificationWorker.KEY_SLOT to slot.name))
            .build()

    /** Minutos desde ahora hasta el próximo hour:minute (hoy o mañana). */
    private fun minutesUntil(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour, minute)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return ChronoUnit.MINUTES.between(now, next).coerceAtLeast(1)
    }
}
