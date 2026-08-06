package com.lucas.predictaapp.data.repository

import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.FixedExpenseDao
import com.lucas.predictaapp.data.local.Session
import com.lucas.predictaapp.data.model.FixedExpense
import com.lucas.predictaapp.data.model.currentMonthKey
import com.lucas.predictaapp.data.remote.SupabaseProvider
import com.lucas.predictaapp.data.remote.SyncErrors
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

object FixedExpensesRepository {
    private lateinit var dao: FixedExpenseDao

    fun init(db: AppDatabase) {
        dao = db.fixedExpenseDao()
    }

    val fixedExpenses: Flow<List<FixedExpense>> get() = dao.getAll()

    /** Baja los gastos fijos de Supabase a Room. Se llama al arrancar para rehidratar tras reinstalar. */
    suspend fun pullFromRemote() {
        if (!Session.isActive) return
        try {
            val remote = SupabaseProvider.client?.from("fixed_expenses")
                ?.select { filter { eq("userId", Session.userId) } }
                ?.decodeList<FixedExpense>()
                ?: return
            if (remote.isNotEmpty()) dao.upsertAll(remote)
        } catch (e: Exception) { SyncErrors.report("fixed_expenses.pull", e) }
    }

    suspend fun upsert(item: FixedExpense) {
        // Room asigna el id real al insertar; sincronizamos esa copia (no el id=0 entrante),
        // así delete/togglePaid después matchean la misma fila en la nube.
        val owned = item.copy(userId = Session.userId)
        val rowId = dao.upsert(owned)
        val synced = if (owned.id == 0L) owned.copy(id = rowId) else owned
        try {
            SupabaseProvider.client?.from("fixed_expenses")?.upsert(synced)
        } catch (e: Exception) { SyncErrors.report("fixed_expenses.upsert", e) }
    }

    suspend fun delete(id: Long) {
        dao.softDelete(id)
        try {
            SupabaseProvider.client?.from("fixed_expenses")?.delete {
                filter { eq("id", id); eq("userId", Session.userId) }
            }
        } catch (e: Exception) { SyncErrors.report("fixed_expenses.delete", e) }
    }

    suspend fun togglePaid(item: FixedExpense) {
        val newKey = if (item.paidMonthKey == currentMonthKey()) "" else currentMonthKey()
        dao.setPaidMonthKey(item.id, newKey)
        try {
            SupabaseProvider.client?.from("fixed_expenses")
                ?.update({ set("paidMonthKey", newKey) }) {
                    filter { eq("id", item.id); eq("userId", Session.userId) }
                }
        } catch (e: Exception) { SyncErrors.report("fixed_expenses.togglePaid", e) }
    }
}
