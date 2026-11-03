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

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): Expense?

    /** Movimientos con ese monto dentro de [fromMillis, toMillis), sin importar el comercio. */
    @Query(
        "SELECT * FROM expenses WHERE amount = :amount " +
            "AND dateMillis >= :fromMillis AND dateMillis < :toMillis",
    )
    suspend fun findByAmountInRange(amount: Int, fromMillis: Long, toMillis: Long): List<Expense>

    /** Categorías de gasto usadas con [merchant], la más frecuente primero ("Otros" no cuenta). */
    @Query(
        "SELECT c.name FROM expenses e JOIN categories c ON c.id = e.categoryId " +
            "WHERE e.merchant = :merchant COLLATE NOCASE AND c.type = 'EXPENSE' AND c.name != 'Otros' " +
            "GROUP BY c.id ORDER BY COUNT(*) DESC LIMIT :limit",
    )
    suspend fun topCategoriesFor(merchant: String, limit: Int): List<String>

    /** Categorías de gasto más usadas desde [sinceMillis] ("Otros" no cuenta). */
    @Query(
        "SELECT c.name FROM expenses e JOIN categories c ON c.id = e.categoryId " +
            "WHERE e.dateMillis >= :sinceMillis AND c.type = 'EXPENSE' AND c.name != 'Otros' " +
            "GROUP BY c.id ORDER BY COUNT(*) DESC LIMIT :limit",
    )
    suspend fun topCategoriesSince(sinceMillis: Long, limit: Int): List<String>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapture(capture: CapturedCounterparty)

    /**
     * Nombre y categoría actuales de la última captura de ese destinatario. Una que quedó en
     * "Otros" (el usuario no contestó) solo cuenta si no hay otra.
     */
    @Query(
        "SELECT e.merchant AS merchant, c.name AS category FROM captured_counterparties k " +
            "JOIN expenses e ON e.id = k.expenseId JOIN categories c ON c.id = e.categoryId " +
            "WHERE k.counterpartyKey = :key AND c.type = 'EXPENSE' " +
            "ORDER BY CASE WHEN c.name = 'Otros' THEN 1 ELSE 0 END, k.capturedAt DESC LIMIT 1",
    )
    suspend fun lastCaptureFor(key: String): RememberedCapture?

    @Query("SELECT expenseId FROM captured_counterparties WHERE counterpartyKey = :key")
    suspend fun capturedIdsFor(key: String): List<Long>
}
