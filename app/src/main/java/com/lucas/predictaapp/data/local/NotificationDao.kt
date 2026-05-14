package com.lucas.predictaapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lucas.predictaapp.data.model.Notification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY dateGroup ASC, time DESC")
    fun getAll(): Flow<List<Notification>>

    @Upsert
    suspend fun upsertAll(notifications: List<Notification>)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE notifications SET unread = 0 WHERE id = :id")
    suspend fun markRead(id: String)
}
