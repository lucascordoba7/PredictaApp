package com.lucas.predictaapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchant: String,
    val category: String,
    val amount: Int,
    val whenLabel: String = "ahora",
    val source: ExpenseSource = ExpenseSource.MANUAL,
    val dateMillis: Long = System.currentTimeMillis(),
)

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
