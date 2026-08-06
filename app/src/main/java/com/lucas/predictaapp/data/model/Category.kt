package com.lucas.predictaapp.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

enum class CategoryType { EXPENSE, INCOME }

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(
    tableName = "categories",
    // No pueden existir dos categorías con el mismo nombre (case-insensitive vía COLLATE NOCASE
    // se maneja en la app; acá garantizamos unicidad exacta y matamos la duplicación por seed doble).
    indices = [Index(value = ["name"], unique = true)],
)
data class Category(
    // @EncodeDefault(ALWAYS): kotlinx.serialization omite los campos que valen su default,
    // y en un upsert en lote PostgREST rellena con NULL las claves faltantes (no aplica el
    // default de Postgres) → violaba el NOT NULL de "sortOrder"/"isCustom". Forzamos que
    // siempre viajen en el JSON.
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val color: String,
    val type: CategoryType,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val isCustom: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val sortOrder: Int = 0,
    // Ver Expense.userId. En Supabase la unicidad del nombre es por ("userId", name).
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val userId: String = "",
)
