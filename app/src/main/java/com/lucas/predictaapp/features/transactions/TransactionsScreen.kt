package com.lucas.predictaapp.features.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.model.CategoryType
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.model.ExpenseWithCategory
import com.lucas.predictaapp.data.model.toLocalDate
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.data.repository.SyncManager
import com.lucas.predictaapp.features.dashboard.components.EditExpenseSheet
import com.lucas.predictaapp.ui.components.DeleteExpenseDialog
import com.lucas.predictaapp.ui.components.PredictaPullRefresh
import com.lucas.predictaapp.ui.components.TransactionRow
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs
import com.lucas.predictaapp.ui.utils.monthYearLabel
import java.time.YearMonth
import kotlinx.coroutines.launch

/**
 * Tab Actividad: historial completo de transacciones con búsqueda por texto,
 * filtro por categoría y agrupado por mes con subtotal. Tap edita, long-press borra.
 */
@Composable
fun TransactionsScreen() {
    val expenses by ExpensesRepository.expensesWithCategory.collectAsStateWithLifecycle(emptyList())
    val allCategories by CategoryRepository.categories.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var pendingDelete by remember { mutableStateOf<ExpenseWithCategory?>(null) }

    // Chips: solo categorías con movimientos, ordenadas por frecuencia de uso.
    val usedCategories = remember(expenses) {
        expenses.groupingBy { it.category }.eachCount().entries
            .sortedByDescending { it.value }
            .map { it.key }
    }

    val filtered = expenses.filter { e ->
        (selectedCategory == null || e.category == selectedCategory) &&
            (
                query.isBlank() ||
                    e.merchant.contains(query, ignoreCase = true) ||
                    e.category.contains(query, ignoreCase = true)
                )
    }
    val byMonth = filtered
        .sortedByDescending { it.dateMillis }
        .groupBy { YearMonth.from(it.dateMillis.toLocalDate()) }

    PredictaPullRefresh(
        modifier = Modifier
            .fillMaxSize()
            .background(PredictaColors.charcoal),
        onRefresh = { SyncManager.pullAll() },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        ) {
            item {
                Text(
                    text = "Transacciones",
                    style = PredictaTypography.titlePage.copy(fontSize = 26.sp),
                    color = PredictaColors.cream,
                    modifier = Modifier.padding(top = 18.dp, bottom = 14.dp, start = 2.dp),
                )
            }

            item {
                SearchField(query = query, onQueryChange = { query = it })
            }

            if (usedCategories.isNotEmpty()) {
                item {
                    CategoryChips(
                        categories = usedCategories,
                        selected = selectedCategory,
                        emojiFor = { name -> expenses.firstOrNull { it.category == name }?.emoji ?: "💸" },
                        onSelect = { selectedCategory = if (selectedCategory == it) null else it },
                    )
                }
            }

            if (expenses.isEmpty()) {
                item { EmptyNote("Todavía no registraste movimientos.\nContale un gasto al chat para arrancar.") }
            } else if (filtered.isEmpty()) {
                item { EmptyNote("Sin resultados para esa búsqueda.") }
            }

            byMonth.forEach { (month, items) ->
                item(key = "header_$month") {
                    MonthHeader(month = month, items = items)
                }
                items(items, key = { it.id }) { item ->
                    Column {
                        TransactionRow(
                            item = item,
                            onClick = { editingExpense = item.expense },
                            onLongClick = { pendingDelete = item },
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(PredictaColors.line),
                        )
                    }
                }
            }
        }
    }

    editingExpense?.let { expense ->
        EditExpenseSheet(
            expense = expense,
            categories = allCategories.filter { it.type == CategoryType.EXPENSE },
            onSave = { updated ->
                scope.launch { ExpensesRepository.update(updated) }
                editingExpense = null
            },
            onDismiss = { editingExpense = null },
        )
    }

    pendingDelete?.let { item ->
        DeleteExpenseDialog(
            item = item,
            onConfirm = {
                scope.launch { ExpensesRepository.delete(item.id) }
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val shape = RoundedCornerShape(PredictaDimensions.Radius.lg)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(PredictaColors.surfaceHigh)
            .border(1.dp, PredictaColors.lineStrong, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = PredictaColors.cream35,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = PredictaTypography.body.copy(color = PredictaColors.cream),
            cursorBrush = SolidColor(PredictaColors.amber),
            singleLine = true,
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Buscar comercio o categoría",
                        style = PredictaTypography.body,
                        color = PredictaColors.cream35,
                    )
                }
                inner()
            },
        )
        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Limpiar búsqueda",
                tint = PredictaColors.cream60,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<String>,
    selected: String?,
    emojiFor: (String) -> String,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
    ) {
        items(categories, key = { it }) { name ->
            val isSelected = name == selected
            Text(
                text = "${emojiFor(name)} $name",
                style = PredictaTypography.small.copy(
                    color = if (isSelected) PredictaColors.onAmber else PredictaColors.cream60,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) PredictaColors.amber else PredictaColors.surfaceHigh)
                    .border(
                        1.dp,
                        if (isSelected) PredictaColors.amber else PredictaColors.lineStrong,
                        RoundedCornerShape(999.dp),
                    )
                    .clickable { onSelect(name) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, items: List<ExpenseWithCategory>) {
    val spent = items.filter { !it.isIncome }.sumOf { it.amount }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 8.dp, start = 2.dp, end = 2.dp),
    ) {
        Text(
            text = monthYearLabel(month).uppercase(),
            style = PredictaTypography.monoCap.copy(
                letterSpacing = 1.4.sp,
                color = PredictaColors.cream35,
            ),
            modifier = Modifier.weight(1f),
        )
        if (spent > 0) {
            Text(
                text = "− $ ${spent.fmtArs()}",
                style = PredictaTypography.monoCap.copy(color = PredictaColors.cream60),
            )
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text = text,
        style = PredictaTypography.small,
        color = PredictaColors.cream35,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
    )
}
