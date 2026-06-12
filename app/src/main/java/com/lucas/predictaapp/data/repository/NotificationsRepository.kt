package com.lucas.predictaapp.data.repository

import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.NotificationDao
import com.lucas.predictaapp.data.model.Notification
import com.lucas.predictaapp.data.remote.SupabaseProvider
import com.lucas.predictaapp.data.remote.SyncErrors
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

object NotificationsRepository {
    private lateinit var dao: NotificationDao

    fun init(db: AppDatabase) {
        dao = db.notificationDao()
    }

    val notifications: Flow<List<Notification>> get() = dao.getAll()

    /** Baja las notificaciones de Supabase a Room. Se llama al arrancar para rehidratar tras reinstalar. */
    suspend fun pullFromRemote() {
        try {
            val remote = SupabaseProvider.client?.from("notifications")?.select()?.decodeList<Notification>()
                ?: return
            if (remote.isNotEmpty()) dao.upsertAll(remote)
        } catch (e: Exception) { SyncErrors.report("notifications.pull", e) }
    }

    suspend fun dismiss(id: String) {
        dao.delete(id)
        try {
            SupabaseProvider.client?.from("notifications")?.delete { filter { eq("id", id) } }
        } catch (e: Exception) { SyncErrors.report("notifications.delete", e) }
    }

    suspend fun markRead(id: String) {
        dao.markRead(id)
    }
}
