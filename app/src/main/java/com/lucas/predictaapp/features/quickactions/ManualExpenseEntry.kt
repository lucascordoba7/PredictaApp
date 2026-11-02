package com.lucas.predictaapp.features.quickactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.features.dashboard.components.EditExpenseSheet
import com.lucas.predictaapp.ui.components.PredictaDialog
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
        categories = allCategories,
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
        PredictaDialog(
            title = "¿Ya lo cargaste?",
            text = "Hoy ya hay un ${candidate.merchant} por $${candidate.amount.fmtArs()}. ¿Lo registro igual?",
            confirmLabel = "Registrar igual",
            onConfirm = {
                scope.launch {
                    ExpensesRepository.add(candidate)
                    duplicateCandidate = null
                    onDone()
                }
            },
            onDismiss = { duplicateCandidate = null },
        )
    }
}
