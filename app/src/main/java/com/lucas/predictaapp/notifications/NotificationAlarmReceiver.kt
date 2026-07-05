package com.lucas.predictaapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

/**
 * Recibe el alarm diario y delega el trabajo real (sync + reglas + posteo) a un
 * OneTimeWork: el receiver tiene ~10s de presupuesto y el worker necesita red.
 * Los alarms one-shot no se repiten solos, así que acá se re-arma el del día siguiente.
 * También re-arma ambos slots tras un reinicio (los alarms no sobreviven el reboot).
 */
class NotificationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationScheduler.schedule(context)
            return
        }
        if (intent.action != NotificationScheduler.ACTION_ALARM) return

        val slot = runCatching {
            NotifSlot.valueOf(intent.getStringExtra(NotificationWorker.KEY_SLOT).orEmpty())
        }.getOrDefault(NotifSlot.MORNING)

        WorkManager.getInstance(context).enqueueUniqueWork(
            "notif_run_${slot.name}",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(workDataOf(NotificationWorker.KEY_SLOT to slot.name))
                .build(),
        )
        NotificationScheduler.armAlarm(context, slot)
    }
}
