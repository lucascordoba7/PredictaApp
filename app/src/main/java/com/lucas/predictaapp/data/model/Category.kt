package com.lucas.predictaapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class CategoryType { EXPENSE, INCOME }

@Serializable
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val color: String,
    val type: CategoryType,
    val isCustom: Boolean = false,
    val sortOrder: Int = 0,
)
