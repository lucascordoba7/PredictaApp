package com.lucas.predictaapp.data.repository

/**
 * Punto único para rehidratar todo desde Supabase. Lo usa el arranque
 * (PredictaApp.onCreate) y el pull-to-refresh de las pantallas.
 */
object SyncManager {
    suspend fun pullAll() {
        // Categorías PRIMERO: los gastos tienen FK a categories, deben existir antes del upsert.
        CategoryRepository.pullFromRemote()
        ExpensesRepository.pullFromRemote()
        SubscriptionsRepository.pullFromRemote()
        FixedExpensesRepository.pullFromRemote()
        NotificationsRepository.pullFromRemote()
    }
}
