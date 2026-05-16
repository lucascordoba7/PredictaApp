package com.lucas.predictaapp.data.repository

import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.FixedExpenseDao
import com.lucas.predictaapp.data.model.FixedExpense
import com.lucas.predictaapp.data.model.currentMonthKey
import com.lucas.predictaapp.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

object FixedExpensesRepository {
    private lateinit var dao: FixedExpenseDao

    fun init(db: AppDatabase) {
        dao = db.fixedExpenseDao()
    }

    val fixedExpenses: Flow<List<FixedExpense>> get() = dao.getAll()

    suspend fun upsert(item: FixedExpense) {
        dao.upsert(item)
        try {
            SupabaseProvider.client?.from("fixed_expenses")?.upsert(item)
        } catch (_: Exception) {}
    }

    suspend fun delete(id: Long) {
        dao.softDelete(id)
        try {
            SupabaseProvider.client?.from("fixed_expenses")
                ?.delete { filter { eq("id", id) } }
        } catch (_: Exception) {}
    }

    suspend fun togglePaid(item: FixedExpense) {
        val newKey = if (item.paidMonthKey == currentMonthKey()) "" else currentMonthKey()
        dao.setPaidMonthKey(item.id, newKey)
        try {
            SupabaseProvider.client?.from("fixed_expenses")
                ?.update({ set("paidMonthKey", newKey) }) { filter { eq("id", item.id) } }
        } catch (_: Exception) {}
    }
}
