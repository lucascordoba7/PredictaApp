package com.lucas.predictaapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lucas.predictaapp.data.model.Subscription
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY service ASC")
    fun getAll(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions")
    suspend fun getAllOnce(): List<Subscription>

    @Upsert
    suspend fun upsertAll(subscriptions: List<Subscription>)

    @Upsert
    suspend fun upsert(subscription: Subscription)

    /** Reasigna la categoría de las suscripciones (al borrar la categoría origen). */
    @Query("UPDATE subscriptions SET categoryId = :toId WHERE categoryId = :fromId")
    suspend fun reassignCategory(fromId: Long, toId: Long)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun delete(id: String)
}
