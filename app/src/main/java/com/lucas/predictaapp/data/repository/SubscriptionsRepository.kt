package com.lucas.predictaapp.data.repository

import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.SubscriptionDao
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.model.Subscription
import com.lucas.predictaapp.data.model.chargeDateMillis
import com.lucas.predictaapp.data.model.currentMonthKey
import com.lucas.predictaapp.data.model.isChargeDue
import com.lucas.predictaapp.data.remote.SupabaseProvider
import com.lucas.predictaapp.data.remote.SyncErrors
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

object SubscriptionsRepository {
    private lateinit var dao: SubscriptionDao

    fun init(db: AppDatabase) {
        dao = db.subscriptionDao()
    }

    val subscriptions: Flow<List<Subscription>> get() = dao.getAll()

    /** Baja las suscripciones de Supabase a Room. Se llama al arrancar para rehidratar tras reinstalar. */
    suspend fun pullFromRemote() {
        try {
            val remote = SupabaseProvider.client?.from("subscriptions")?.select()?.decodeList<Subscription>()
                ?: return
            if (remote.isNotEmpty()) dao.upsertAll(remote)
        } catch (e: Exception) { SyncErrors.report("subscriptions.pull", e) }
    }

    suspend fun upsert(sub: Subscription) {
        dao.upsert(sub)
        try {
            SupabaseProvider.client?.from("subscriptions")?.upsert(sub)
        } catch (e: Exception) { SyncErrors.report("subscriptions.upsert", e) }
    }

    suspend fun cancel(id: String) {
        dao.delete(id)
        try {
            SupabaseProvider.client?.from("subscriptions")?.delete { filter { eq("id", id) } }
        } catch (e: Exception) { SyncErrors.report("subscriptions.delete", e) }
    }

    /**
     * Genera el cargo del mes para cada suscripción vencida (activa, no cobrada este mes y
     * con el día de cobro ya pasado). Crea un Expense real linkeado a la sub y marca el mes
     * para no duplicar. Idempotente: se puede llamar en cada arranque/refresh.
     */
    suspend fun generateDueCharges() {
        val monthKey = currentMonthKey()
        val month = YearMonth.now()
        val due = dao.getAllOnce().filter { it.isChargeDue() }
        for (sub in due) {
            // Guard: si la categoría quedó colgada (0 o borrada), cae en "Otros" para no romper el FK.
            val categoryId = sub.categoryId.takeIf { it > 0 }
                ?: CategoryRepository.resolveId("Suscripciones", income = false)
            ExpensesRepository.add(
                Expense(
                    merchant = sub.service,
                    categoryId = categoryId,
                    amount = sub.monthly,
                    dateMillis = sub.chargeDateMillis(month),
                    subscriptionId = sub.id,
                ),
            )
            upsert(sub.copy(lastChargedMonthKey = monthKey))
        }
    }

    /** Reasigna la categoría de las suscripciones (al borrar la categoría origen → "Otros"). */
    suspend fun reassignCategory(fromId: Long, toId: Long) {
        dao.reassignCategory(fromId, toId)
        try {
            SupabaseProvider.client?.from("subscriptions")
                ?.update({ set("categoryId", toId) }) { filter { eq("categoryId", fromId) } }
        } catch (e: Exception) { SyncErrors.report("subscriptions.reassignCategory", e) }
    }
}
