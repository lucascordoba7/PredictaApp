package com.lucas.predictaapp.data.repository

import android.content.Context
import com.lucas.predictaapp.data.local.AppDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Punto único para rehidratar todo desde Supabase. Lo usa el arranque
 * (PredictaApp.onCreate) y el pull-to-refresh de las pantallas.
 */
object SyncManager {
    // Serializa los syncs: arranque, NotificationWorker y pull-to-refresh pueden
    // solaparse, y dos generateDueCharges concurrentes duplican los cargos del mes.
    private val syncMutex = Mutex()

    suspend fun pullAll() = syncMutex.withLock {
        // Categorías PRIMERO: los gastos tienen FK a categories, deben existir antes del upsert.
        CategoryRepository.pullFromRemote()
        ExpensesRepository.pullFromRemote()
        SubscriptionsRepository.pullFromRemote()
        FixedExpensesRepository.pullFromRemote()
        NotificationsRepository.pullFromRemote()
        // Con las suscripciones ya sincronizadas, generar los cargos del mes que correspondan.
        SubscriptionsRepository.generateDueCharges()
        // Purga cargos duplicados de corridas concurrentes previas (local y nube).
        SubscriptionsRepository.dedupeCharges()
    }

    /**
     * Arranque de una cuenta recién abierta (login o alta). Vacía Room antes de traer:
     * lo que hubiera quedado es de otra cuenta, y mezclarlo produciría ids colisionados
     * y gastos huérfanos. Después siembra categorías si la cuenta es nueva.
     */
    suspend fun hydrateForCurrentAccount(context: Context) {
        AppDatabase.getInstance(context).clearAllTables()
        pullAll()
        CategoryRepository.bootstrap()
    }
}
