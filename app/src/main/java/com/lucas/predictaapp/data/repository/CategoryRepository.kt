package com.lucas.predictaapp.data.repository

import com.lucas.predictaapp.data.local.AppDatabase
import com.lucas.predictaapp.data.local.CategoryDao
import com.lucas.predictaapp.data.model.Category
import com.lucas.predictaapp.data.model.CategoryType
import kotlinx.coroutines.flow.Flow

object CategoryRepository {
    private lateinit var dao: CategoryDao

    fun init(db: AppDatabase) {
        dao = db.categoryDao()
    }

    val categories: Flow<List<Category>> get() = dao.getAll()

    suspend fun add(name: String, emoji: String, color: String, type: CategoryType) {
        dao.insert(Category(name = name.trim(), emoji = emoji, color = color, type = type, isCustom = true))
    }

    suspend fun update(category: Category, name: String, emoji: String, color: String, type: CategoryType) {
        dao.update(category.copy(name = name.trim(), emoji = emoji, color = color, type = type))
    }

    suspend fun reorder(orderedList: List<Category>) {
        orderedList.forEachIndexed { index, cat ->
            dao.updateSortOrder(cat.id, index)
        }
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
