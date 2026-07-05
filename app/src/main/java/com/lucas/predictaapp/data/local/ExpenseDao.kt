package com.lucas.predictaapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.model.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAll(): Flow<List<Expense>>

    /** Lectura relacional: cada gasto con su categoría resuelta (join por categoryId). */
    @Transaction
    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllWithCategory(): Flow<List<ExpenseWithCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Upsert
    suspend fun upsertAll(expenses: List<Expense>): List<Long>

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: Long)

    /** Mueve los gastos de una categoría a otra (usado al borrar una categoría → "Otros"). */
    @Query("UPDATE expenses SET categoryId = :toId WHERE categoryId = :fromId")
    suspend fun reassignCategory(fromId: Long, toId: Long)

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun count(): Int

    /** Ids de cargos ya generados por [subscriptionId] en el rango [fromMillis, toMillis). */
    @Query(
        "SELECT id FROM expenses WHERE subscriptionId = :subscriptionId " +
            "AND dateMillis >= :fromMillis AND dateMillis < :toMillis ORDER BY id ASC",
    )
    suspend fun subscriptionChargeIds(subscriptionId: String, fromMillis: Long, toMillis: Long): List<Long>

    /** Todos los gastos generados por suscripciones (para deduplicar por sub+mes). */
    @Query("SELECT * FROM expenses WHERE subscriptionId IS NOT NULL")
    suspend fun subscriptionChargesOnce(): List<Expense>

    /** Posible doble carga: mismo comercio y monto dentro del rango [fromMillis, toMillis). */
    @Query(
        "SELECT * FROM expenses WHERE merchant = :merchant COLLATE NOCASE AND amount = :amount " +
            "AND dateMillis >= :fromMillis AND dateMillis < :toMillis LIMIT 1",
    )
    suspend fun findDuplicate(merchant: String, amount: Int, fromMillis: Long, toMillis: Long): Expense?
}
