package com.lucas.predictaapp

import android.app.Application
import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.data.repository.NotificationsRepository
import com.lucas.predictaapp.data.repository.SubscriptionsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PredictaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)
        ExpensesRepository.init(db)
        SubscriptionsRepository.init(db)
        NotificationsRepository.init(db)
        CategoryRepository.init(db)
        CoroutineScope(Dispatchers.IO).launch {
            ExpensesRepository.seedIfEmpty()
        }
    }
}
