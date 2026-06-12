package com.lucas.predictaapp.data.repository

import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.ExpenseDao
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.remote.SupabaseProvider
import com.lucas.predictaapp.data.remote.SyncErrors
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

object ExpensesRepository {
    private lateinit var dao: ExpenseDao

    fun init(db: AppDatabase) {
        dao = db.expenseDao()
    }

    val expenses: Flow<List<Expense>> get() = dao.getAll()

    /** Baja los gastos de Supabase a Room. Se llama al arrancar para rehidratar tras reinstalar. */
    suspend fun pullFromRemote() {
        try {
            val remote = SupabaseProvider.client?.from("expenses")?.select()?.decodeList<Expense>()
                ?: return
            if (remote.isNotEmpty()) dao.upsertAll(remote)
        } catch (e: Exception) { SyncErrors.report("expenses.pull", e) }
    }

    suspend fun add(expense: Expense) {
        // Room asigna el id real al insertar; sincronizamos esa copia (no el id=0 entrante).
        val newId = dao.insert(expense)
        val synced = expense.copy(id = newId)
        try {
            SupabaseProvider.client?.from("expenses")?.upsert(synced)
        } catch (e: Exception) { SyncErrors.report("expenses.upsert", e) }
    }

    suspend fun addExpense(expense: Expense) = add(expense)

    suspend fun addExpenses(newExpenses: List<Expense>) {
        val newIds = dao.upsertAll(newExpenses)
        // upsertAll devuelve -1 para filas actualizadas (no insertadas); en ese caso
        // conservamos el id original del objeto.
        val synced = newExpenses.mapIndexed { i, e ->
            val rowId = newIds.getOrNull(i) ?: -1L
            if (rowId != -1L) e.copy(id = rowId) else e
        }
        try {
            SupabaseProvider.client?.from("expenses")?.upsert(synced)
        } catch (e: Exception) { SyncErrors.report("expenses.upsertAll", e) }
    }

    /** Edita un gasto existente (id real conocido). Actualiza Room y la nube. */
    suspend fun update(expense: Expense) {
        dao.upsertAll(listOf(expense))
        try {
            SupabaseProvider.client?.from("expenses")?.upsert(expense)
        } catch (e: Exception) { SyncErrors.report("expenses.update", e) }
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
        try {
            SupabaseProvider.client?.from("expenses")?.delete { filter { eq("id", id) } }
        } catch (e: Exception) { SyncErrors.report("expenses.delete", e) }
    }
}
