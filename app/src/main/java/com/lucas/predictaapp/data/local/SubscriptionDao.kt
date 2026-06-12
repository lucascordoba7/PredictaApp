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

    @Upsert
    suspend fun upsertAll(subscriptions: List<Subscription>)

    @Upsert
    suspend fun upsert(subscription: Subscription)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun delete(id: String)
}
