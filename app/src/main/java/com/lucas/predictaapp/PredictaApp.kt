package com.lucas.predictaapp

import android.app.Application
import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.UserPreferencesRepository
import com.lucas.predictaapp.data.model.FixedExpense
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.data.repository.FixedExpensesRepository
import com.lucas.predictaapp.data.repository.NotificationsRepository
import com.lucas.predictaapp.data.repository.SubscriptionsRepository
import com.lucas.predictaapp.data.repository.SyncManager
import com.lucas.predictaapp.notifications.NotificationChannels
import com.lucas.predictaapp.notifications.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PredictaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        ExpensesRepository.init(db)
        SubscriptionsRepository.init(db)
        NotificationsRepository.init(db)
        CategoryRepository.init(db)
        FixedExpensesRepository.init(db)

        // Notificaciones: crear canales y programar las corridas diarias (mañana/noche).
        NotificationChannels.ensureChannels(this)
        NotificationScheduler.schedule(this)

        CoroutineScope(Dispatchers.IO).launch {
            // La sesión primero: los repos filtran por Session.userId, así que sin cuenta
            // abierta no hay nada que sincronizar (y sincronizar sería traerse datos ajenos).
            if (!UserPreferencesRepository.restoreSession(this@PredictaApp)) return@launch

            // Rehidratar desde Supabase antes del seed: cubre reinstalación (Room vacío,
            // datos vivos en la nube). Si no hay claves/red, los pull son no-op silenciosos.
            SyncManager.pullAll()

            // Siembra/empuja categorías una vez que el pull rehidrató lo que hubiera en la nube.
            CategoryRepository.bootstrap()

            val setup = UserPreferencesRepository.getUserSetup(this@PredictaApp).first()
            if (setup != null && setup.fixedMonthly > 0) {
                val existing = FixedExpensesRepository.fixedExpenses.first()
                if (existing.isEmpty()) {
                    FixedExpensesRepository.upsert(
                        FixedExpense(
                            name = "Gastos fijos",
                            amount = setup.fixedMonthly,
                            dueDayOfMonth = 1,
                        ),
                    )
                }
            }
        }
    }
}
