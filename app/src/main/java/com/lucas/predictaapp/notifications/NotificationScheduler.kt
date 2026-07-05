package com.lucas.predictaapp.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.WorkManager
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Programa dos corridas diarias del motor de notificaciones:
 *  - MAÑANA (~10:00): cobros, gastos fijos por vencer, resumen semanal, picos de categoría.
 *  - NOCHE  (~20:30): "te olvidaste de registrar" y el recordatorio de hábito.
 *
 * Usa AlarmManager (no PeriodicWork): WorkManager es deferrable y bajo Doze/restricciones
 * de batería podía posponer las corridas días enteros hasta que se abriera la app.
 * El alarm despierta el dispositivo a la hora pactada y dispara [NotificationAlarmReceiver],
 * que encola el worker y re-arma el alarm del día siguiente.
 */
object NotificationScheduler {
    const val ACTION_ALARM = "com.lucas.predictaapp.action.NOTIF_ALARM"

    // Nombres de los periodic works del esquema anterior; se cancelan al migrar.
    private const val LEGACY_MORNING_WORK = "notif_morning"
    private const val LEGACY_EVENING_WORK = "notif_evening"

    fun schedule(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(LEGACY_MORNING_WORK)
            cancelUniqueWork(LEGACY_EVENING_WORK)
        }
        armAlarm(context, NotifSlot.MORNING)
        armAlarm(context, NotifSlot.EVENING)
    }

    /** Arma (o re-arma, reemplazando) el alarm one-shot del próximo slot. */
    fun armAlarm(context: Context, slot: NotifSlot) {
        val (hour, minute) = when (slot) {
            NotifSlot.MORNING -> 10 to 0
            NotifSlot.EVENING -> 20 to 30
        }
        val triggerAt = nextTriggerMillis(hour, minute)
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return

        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(NotificationWorker.KEY_SLOT, slot.name)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            slot.ordinal, // requestCode distinto por slot: un alarm vivo para cada uno
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // Exacto si el sistema lo permite (auto-concedido en API 31-32, opt-in del usuario
        // en 33+); si no, setAndAllowWhileIdle igual perfora Doze con minutos de margen —
        // nunca más los días de demora del PeriodicWork.
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    /** Millis del próximo hour:minute (hoy si todavía no pasó, si no mañana). */
    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour, minute)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
