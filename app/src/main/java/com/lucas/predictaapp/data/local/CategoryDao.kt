package com.lucas.predictaapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.lucas.predictaapp.data.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, type ASC, name ASC")
    fun getAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, type ASC, name ASC")
    suspend fun getAllOnce(): List<Category>

    @Query("SELECT id FROM categories WHERE name = :name LIMIT 1")
    suspend fun idByName(name: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<Category>)

    /**
     * Pull desde Supabase: @Upsert (UPDATE por PK, sin DELETE) para no disparar el
     * ON DELETE del FK de expenses. REPLACE haría delete+insert y rompería la relación.
     */
    @Upsert
    suspend fun upsertAll(categories: List<Category>)

    /** Devuelve el rowId insertado, o -1 si se ignoró por nombre duplicado. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category): Long

    @androidx.room.Update
    suspend fun update(category: Category)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countAll(): Int

    @Query("UPDATE categories SET sortOrder = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)

    @Query("DELETE FROM categories WHERE id = :id AND isCustom = 1")
    suspend fun delete(id: Long)
}
