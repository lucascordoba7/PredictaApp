package com.lucas.predictaapp.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            // RESTRICT: no se borra una categoría con gastos sin antes reasignarlos.
            // El flujo de borrado los mueve a "Otros" (ver CategoryRepository.delete).
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId")],
)
data class Expense(
    // @EncodeDefault(ALWAYS): en un upsert en lote PostgREST rellena con NULL las claves
    // omitidas (no aplica el default de Postgres), así que forzamos a que los campos con
    // default siempre viajen en el JSON.
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchant: String,
    val categoryId: Long,
    val amount: Int,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val source: ExpenseSource = ExpenseSource.MANUAL,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val dateMillis: Long = System.currentTimeMillis(),
)

/**
 * Lectura relacional: un gasto junto a su categoría (join por categoryId).
 * Los accessors delegados dejan que el código que antes leía `expense.category`/`amount`
 * siga funcionando, ahora resolviendo el nombre/estilo desde la categoría real.
 */
data class ExpenseWithCategory(
    @Embedded val expense: Expense,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val categoryEntity: Category?,
) {
    val id: Long get() = expense.id
    val merchant: String get() = expense.merchant
    val amount: Int get() = expense.amount
    val dateMillis: Long get() = expense.dateMillis
    val category: String get() = categoryEntity?.name ?: "Otros"
    val isIncome: Boolean get() = categoryEntity?.type == CategoryType.INCOME
    val emoji: String get() = categoryEntity?.emoji ?: "💸"
    val color: String get() = categoryEntity?.color ?: "#636E72"
}

@Serializable
enum class ExpenseSource {
    WHATSAPP_IMAGE,
    WHATSAPP_TEXT,
    WHATSAPP_AUDIO,
    MANUAL,
}

@Serializable
data class ExpenseLineItem(
    val label: String,
    val amount: Int,
)

@Serializable
sealed class ExpenseExtraction {
    @Serializable
    data class Expense(
        val merchant: String,
        val category: String,
        val amount: Int,
        val items: List<ExpenseLineItem> = emptyList(),
        val dateMillis: Long = System.currentTimeMillis(),
        val whenLabel: String = "hoy",
    ) : ExpenseExtraction()

    @Serializable
    data class Income(
        val merchant: String,
        val category: String = "Ingreso",
        val amount: Int,
        val dateMillis: Long = System.currentTimeMillis(),
        val whenLabel: String = "hoy",
    ) : ExpenseExtraction()

    @Serializable
    data class MultiExpense(
        val expenses: List<Expense>,
    ) : ExpenseExtraction()

    @Serializable
    data class Subscription(
        val service: String,
        val monthly: Int,
    ) : ExpenseExtraction()

    @Serializable
    data class Clarify(
        val reason: String,
        val question: String,
        val suggestions: List<ExpenseSuggestion> = emptyList(),
    ) : ExpenseExtraction()

    @Serializable
    data object Unknown : ExpenseExtraction()
}

@Serializable
data class ExpenseSuggestion(
    val label: String,
    val rewrittenIntent: String,
)
