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

        CoroutineScope(Dispatchers.IO).launch {
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
