package com.lucas.predictaapp.data.repository

import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.ExpenseDao
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.model.ExpenseSource
import com.lucas.predictaapp.data.remote.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

object ExpensesRepository {
    private lateinit var dao: ExpenseDao

    fun init(db: AppDatabase) {
        dao = db.expenseDao()
    }

    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        val day = 86_400_000L
        val now = System.currentTimeMillis()
        dao.upsertAll(listOf(
            Expense(merchant = "Don Julio",  category = "Salidas",       amount = 38500, whenLabel = "ayer 22:48",  source = ExpenseSource.WHATSAPP_IMAGE, dateMillis = now - day),
            Expense(merchant = "Uber",       category = "Transporte",    amount = 8400,  whenLabel = "ayer 23:46",  source = ExpenseSource.WHATSAPP_IMAGE, dateMillis = now - day),
            Expense(merchant = "Farmacity",  category = "Farmacia",      amount = 4870,  whenLabel = "hace 2d",     source = ExpenseSource.WHATSAPP_TEXT,  dateMillis = now - 2 * day),
            Expense(merchant = "Rappi",      category = "Delivery",      amount = 6200,  whenLabel = "hace 2d",     source = ExpenseSource.WHATSAPP_TEXT,  dateMillis = now - 2 * day),
            Expense(merchant = "Coto",       category = "Supermercado",  amount = 12450, whenLabel = "hace 3d",     source = ExpenseSource.WHATSAPP_IMAGE, dateMillis = now - 3 * day),
        ))
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
