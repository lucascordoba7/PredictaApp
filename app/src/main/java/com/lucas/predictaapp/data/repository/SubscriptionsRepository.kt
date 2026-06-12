package com.lucas.predictaapp.data.repository

import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.SubscriptionDao
import com.lucas.predictaapp.data.model.Subscription
import com.lucas.predictaapp.data.remote.SupabaseProvider
import com.lucas.predictaapp.data.remote.SyncErrors
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

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
}
