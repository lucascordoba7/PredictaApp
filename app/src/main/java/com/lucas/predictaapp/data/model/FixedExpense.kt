package com.lucas.predictaapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.Calendar

@Serializable
@Entity(tableName = "fixed_expenses")
data class FixedExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Int,
    val dueDayOfMonth: Int,
    val active: Boolean = true,
    val paidMonthKey: String = "",  // "YYYY-MM", se resetea automático cada mes
    val userId: String = "",        // ver Expense.userId
)

enum class FixedExpenseStatus { PAGADO, PENDIENTE, VENCIDO }

fun currentMonthKey(): String {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    return "$year-${month.toString().padStart(2, '0')}"
}

fun FixedExpense.computeStatus(
    todayDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
    monthKey: String = currentMonthKey(),
): FixedExpenseStatus = when {
    paidMonthKey == monthKey -> FixedExpenseStatus.PAGADO
    todayDay <= dueDayOfMonth -> FixedExpenseStatus.PENDIENTE
    else -> FixedExpenseStatus.VENCIDO
}
