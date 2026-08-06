package com.lucas.predictaapp.data.repository

import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.NotificationDao
import com.lucas.predictaapp.data.local.Session
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

    /** Inserta una notificación en el feed in-app (y la sube). La usa el motor de notificaciones. */
    suspend fun add(notification: Notification) {
        val owned = notification.copy(userId = Session.userId)
        dao.upsertAll(listOf(owned))
        try {
            SupabaseProvider.client?.from("notifications")?.upsert(owned)
        } catch (e: Exception) { SyncErrors.report("notifications.add", e) }
    }

    /** Baja las notificaciones de Supabase a Room. Se llama al arrancar para rehidratar tras reinstalar. */
    suspend fun pullFromRemote() {
        if (!Session.isActive) return
        try {
            val remote = SupabaseProvider.client?.from("notifications")
                ?.select { filter { eq("userId", Session.userId) } }
                ?.decodeList<Notification>()
                ?: return
            if (remote.isNotEmpty()) dao.upsertAll(remote)
        } catch (e: Exception) { SyncErrors.report("notifications.pull", e) }
    }

    suspend fun dismiss(id: String) {
        dao.delete(id)
        try {
            SupabaseProvider.client?.from("notifications")?.delete {
                filter { eq("id", id); eq("userId", Session.userId) }
            }
        } catch (e: Exception) { SyncErrors.report("notifications.delete", e) }
    }

    suspend fun markRead(id: String) {
        dao.markRead(id)
    }
}
