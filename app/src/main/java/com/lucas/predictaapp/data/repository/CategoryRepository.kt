package com.lucas.predictaapp.data.repository

import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.CategoryDao
import com.lucas.predictaapp.data.local.Session
import com.lucas.predictaapp.data.model.Category
import com.lucas.predictaapp.data.model.CategoryType
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.data.remote.SupabaseProvider
import com.lucas.predictaapp.data.remote.SyncErrors
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow

object CategoryRepository {
    private lateinit var dao: CategoryDao

    fun init(db: AppDatabase) {
        dao = db.categoryDao()
    }

    val categories: Flow<List<Category>> get() = dao.getAll()

    /** Baja las categorías de Supabase a Room. Se llama al arrancar y en pull-to-refresh. */
    suspend fun pullFromRemote() {
        if (!Session.isActive) return
        try {
            val remote = SupabaseProvider.client?.from("categories")
                ?.select { filter { eq("userId", Session.userId) } }
                ?.decodeList<Category>()
                ?: return
            if (remote.isEmpty()) return
            // El índice único de `name` es local: si una categoría remota trae el mismo
            // nombre con otro id, el @Upsert de Room resolvería por PK, no encontraría la
            // fila y la perdería en silencio — y después los gastos que la referencian
            // revientan por FK. Borramos la homónima local antes de insertar la remota.
            val remoteIds = remote.map { it.id }.toSet()
            dao.getAllOnce()
                .filter { local -> local.id !in remoteIds && remote.any { it.name == local.name } }
                .forEach { stale ->
                    val replacement = remote.first { it.name == stale.name }
                    ExpensesRepository.reassignCategory(stale.id, replacement.id)
                    SubscriptionsRepository.reassignCategory(stale.id, replacement.id)
                    dao.delete(stale.id)
                }
            dao.upsertAll(remote)
        } catch (e: Exception) { SyncErrors.report("categories.pull", e) }
    }

    /**
     * Arranque: corre DESPUÉS de [pullFromRemote]. Cubre tres escenarios:
     *  - Nada local ni remoto (primer arranque real): sembramos los defaults y los subimos.
     *  - Local con datos pero nube vacía (migración a tabla remota): empujamos lo local.
     *  - Nube con datos: el pull ya rehidrató Room, no hacemos nada.
     */
    suspend fun bootstrap() {
        if (!Session.isActive) return
        val remoteEmpty = try {
            SupabaseProvider.client?.from("categories")
                ?.select { filter { eq("userId", Session.userId) } }
                ?.decodeList<Category>()?.isEmpty() ?: true
        } catch (e: Exception) { SyncErrors.report("categories.bootstrap", e); return }

        if (dao.countAll() == 0) {
            dao.insertAll(ExpenseCategories.seed.map { it.copy(userId = Session.userId) })
        }
        if (remoteEmpty) pushAll(dao.getAllOnce())
    }

    suspend fun add(name: String, emoji: String, color: String, type: CategoryType) {
        val category = Category(
            name = name.trim(), emoji = emoji, color = color, type = type,
            isCustom = true, userId = Session.userId,
        )
        // Room asigna el id real; -1 si el nombre ya existía (índice único → IGNORE).
        val newId = dao.insert(category)
        if (newId == -1L) return
        val synced = category.copy(id = newId)
        try {
            SupabaseProvider.client?.from("categories")?.upsert(synced)
        } catch (e: Exception) { SyncErrors.report("categories.add", e) }
    }

    suspend fun update(category: Category, name: String, emoji: String, color: String, type: CategoryType) {
        val updated = category.copy(
            name = name.trim(), emoji = emoji, color = color, type = type,
            userId = Session.userId,
        )
        dao.update(updated)
        try {
            SupabaseProvider.client?.from("categories")?.upsert(updated)
        } catch (e: Exception) { SyncErrors.report("categories.update", e) }
    }

    suspend fun reorder(orderedList: List<Category>) {
        val reordered = orderedList.mapIndexed { index, cat ->
            cat.copy(sortOrder = index, userId = Session.userId)
        }
        reordered.forEach { dao.updateSortOrder(it.id, it.sortOrder) }
        try {
            SupabaseProvider.client?.from("categories")?.upsert(reordered)
        } catch (e: Exception) { SyncErrors.report("categories.reorder", e) }
    }

    /**
     * Resuelve un nombre de categoría a su id. Si no existe, cae en "Otros" (gasto) u
     * "Otros ingresos" (income). Lo usa el alta desde IA/texto. Asume categorías sembradas.
     */
    suspend fun resolveId(name: String, income: Boolean): Long {
        dao.idByName(name.trim())?.let { return it }
        val fallback = if (income) "Otros ingresos" else "Otros"
        return dao.idByName(fallback) ?: dao.idByName("Otros") ?: 0L
    }

    suspend fun delete(id: Long) {
        // FK RESTRICT: antes de borrar la categoría, mover sus gastos (y suscripciones) a "Otros".
        val otros = dao.idByName("Otros")
        if (otros != null && otros != id) {
            ExpensesRepository.reassignCategory(id, otros)
            SubscriptionsRepository.reassignCategory(id, otros)
        }
        dao.delete(id)
        try {
            SupabaseProvider.client?.from("categories")?.delete {
                filter { eq("id", id); eq("userId", Session.userId) }
            }
        } catch (e: Exception) { SyncErrors.report("categories.delete", e) }
    }

    private suspend fun pushAll(list: List<Category>) {
        if (list.isEmpty()) return
        try {
            SupabaseProvider.client?.from("categories")
                ?.upsert(list.map { it.copy(userId = Session.userId) })
        } catch (e: Exception) { SyncErrors.report("categories.pushAll", e) }
    }
}
