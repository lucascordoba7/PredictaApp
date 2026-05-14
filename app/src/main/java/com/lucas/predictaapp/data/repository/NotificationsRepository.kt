package com.lucas.predictaapp.data.repository

import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.NotificationDao
import com.lucas.predictaapp.data.model.Notification
import com.lucas.predictaapp.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

object NotificationsRepository {
    private lateinit var dao: NotificationDao

    fun init(db: AppDatabase) {
        dao = db.notificationDao()
    }

    val notifications: Flow<List<Notification>> get() = dao.getAll()

    suspend fun dismiss(id: String) {
        dao.delete(id)
        try {
            SupabaseProvider.client?.from("notifications")?.delete { filter { eq("id", id) } }
        } catch (_: Exception) {}
    }

    suspend fun markRead(id: String) {
        dao.markRead(id)
    }
}
