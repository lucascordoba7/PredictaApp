package com.lucas.predictaapp.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lucas.predictaapp.data.local.UserPreferencesRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.data.repository.FixedExpensesRepository
import com.lucas.predictaapp.data.repository.SubscriptionsRepository
import com.lucas.predictaapp.data.repository.SyncManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Corre en background (con la app cerrada). Lee el slot (mañana/noche), baja datos frescos,
 * evalúa las reglas de patrón y postea las notificaciones que correspondan.
 */
class NotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val slot = runCatching {
            NotifSlot.valueOf(inputData.getString(KEY_SLOT) ?: NotifSlot.MORNING.name)
        }.getOrDefault(NotifSlot.MORNING)

        return try {
            // Best-effort: datos frescos desde Supabase + generar cargos de suscripción del mes.
            // La sesión se rehidrata acá explícitamente: si el proceso arrancó para correr
            // este worker, la corrutina de PredictaApp.onCreate puede no haber terminado, y
            // sin Session.userId los pull no traen nada.
            runCatching {
                UserPreferencesRepository.restoreSession(applicationContext)
                SyncManager.pullAll()
            }

            val ctx = applicationContext
            val today = LocalDate.now()
            val expenses = ExpensesRepository.expensesWithCategory.first()
            val subs = SubscriptionsRepository.subscriptions.first()
            val fixed = FixedExpensesRepository.fixedExpenses.first()
            val setup = UserPreferencesRepository.getUserSetup(ctx).first()

            val notifs = NotificationRules.evaluate(
                slot = slot,
                today = today,
                expenses = expenses,
                subscriptions = subs,
                fixedExpenses = fixed,
                income = setup?.income ?: 0,
                paydayDay = setup?.paydayDay ?: 1,
            )
            notifs.forEach { SystemNotifier.post(ctx, it) }
            Result.success()
        } catch (e: Exception) {
            // No reintentar agresivo: la próxima corrida diaria vuelve a evaluar.
            Result.success()
        }
    }

    companion object {
        const val KEY_SLOT = "slot"
    }
}
