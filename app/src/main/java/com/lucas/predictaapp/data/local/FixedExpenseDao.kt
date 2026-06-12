package com.lucas.predictaapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lucas.predictaapp.data.model.FixedExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedExpenseDao {
    @Query("SELECT * FROM fixed_expenses WHERE active = 1 ORDER BY dueDayOfMonth ASC")
    fun getAll(): Flow<List<FixedExpense>>

    @Upsert
    suspend fun upsert(item: FixedExpense): Long

    @Upsert
    suspend fun upsertAll(items: List<FixedExpense>)

    @Query("UPDATE fixed_expenses SET active = 0 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("UPDATE fixed_expenses SET paidMonthKey = :monthKey WHERE id = :id")
    suspend fun setPaidMonthKey(id: Long, monthKey: String)
}
