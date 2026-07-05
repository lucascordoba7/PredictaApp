package com.lucas.predictaapp.features.quickactions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.model.CategoryType
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.features.dashboard.components.EditExpenseSheet
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs
import kotlinx.coroutines.launch

/**
 * Alta manual rápida: monto + categoría + nombre, sin IA ni red. Es el camino
 * corto para el gasto obvio y el fallback cuando el chat/extractor no está.
 * Si detecta una posible doble carga (mismo comercio y monto en el día) pide
 * confirmación antes de insertar.
 */
@Composable
fun ManualExpenseEntry(onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val allCategories by CategoryRepository.categories.collectAsStateWithLifecycle(emptyList())
    var duplicateCandidate by remember { mutableStateOf<Expense?>(null) }

    EditExpenseSheet(
        expense = remember { Expense(merchant = "", categoryId = 0, amount = 0) },
        categories = allCategories.filter { it.type == CategoryType.EXPENSE },
        title = "Registrar gasto",
        onSave = { new ->
            scope.launch {
                val dup = ExpensesRepository.findSameDayDuplicate(new.merchant, new.amount, new.dateMillis)
                if (dup != null) {
                    duplicateCandidate = new
                } else {
                    ExpensesRepository.add(new)
                    onDone()
                }
            }
        },
        onDismiss = onDone,
    )

    duplicateCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { duplicateCandidate = null },
            containerColor = PredictaColors.surface,
            title = {
                Text("¿Gasto duplicado?", style = PredictaTypography.cardTitle, color = PredictaColors.cream)
            },
            text = {
                Text(
                    text = "Ya registraste ${candidate.merchant} por $${candidate.amount.fmtArs()} hoy. ¿Lo registro igual?",
                    style = PredictaTypography.small,
                    color = PredictaColors.cream60,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        ExpensesRepository.add(candidate)
                        duplicateCandidate = null
                        onDone()
                    }
                }) { Text("Registrar igual", color = PredictaColors.amber) }
            },
            dismissButton = {
                TextButton(onClick = { duplicateCandidate = null }) {
                    Text("Cancelar", color = PredictaColors.cream60)
                }
            },
        )
    }
}
