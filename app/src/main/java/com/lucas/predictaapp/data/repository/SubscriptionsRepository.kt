package com.lucas.predictaapp.data.repository

import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.SubscriptionDao
import com.lucas.predictaapp.data.model.Subscription
import com.lucas.predictaapp.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

object SubscriptionsRepository {
    private lateinit var dao: SubscriptionDao

    fun init(db: AppDatabase) {
        dao = db.subscriptionDao()
    }

    val subscriptions: Flow<List<Subscription>> get() = dao.getAll()

    suspend fun cancel(id: String) {
        dao.delete(id)
        try {
            SupabaseProvider.client?.from("subscriptions")?.delete { filter { eq("id", id) } }
        } catch (_: Exception) {}
    }
}
