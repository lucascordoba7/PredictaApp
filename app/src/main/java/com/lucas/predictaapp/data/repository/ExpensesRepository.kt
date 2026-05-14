package com.lucas.predictaapp.data.repository

import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.ExpenseDao
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

object ExpensesRepository {
    private lateinit var dao: ExpenseDao

    fun init(db: AppDatabase) {
        dao = db.expenseDao()
    }

    val expenses: Flow<List<Expense>> get() = dao.getAll()

    suspend fun add(expense: Expense) {
        dao.insert(expense)
        try {
            SupabaseProvider.client?.from("expenses")?.upsert(expense)
        } catch (_: Exception) {}
    }

    suspend fun addExpense(expense: Expense) = add(expense)

    suspend fun addExpenses(newExpenses: List<Expense>) {
        dao.upsertAll(newExpenses)
        try {
            SupabaseProvider.client?.from("expenses")?.upsert(newExpenses)
        } catch (_: Exception) {}
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
        try {
            SupabaseProvider.client?.from("expenses")?.delete { filter { eq("id", id) } }
        } catch (_: Exception) {}
    }
}
