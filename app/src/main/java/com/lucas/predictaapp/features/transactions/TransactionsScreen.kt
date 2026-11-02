package com.lucas.predictaapp.features.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.data.model.ExpenseWithCategory
import com.lucas.predictaapp.data.model.toLocalDate
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.data.repository.SyncManager
import com.lucas.predictaapp.features.dashboard.components.EditExpenseSheet
import com.lucas.predictaapp.ui.components.ChoiceChip
import com.lucas.predictaapp.ui.components.DeleteExpenseDialog
import com.lucas.predictaapp.ui.components.EmptyState
import com.lucas.predictaapp.ui.components.HeaderIconButton
import com.lucas.predictaapp.ui.components.HeroCard
import com.lucas.predictaapp.ui.components.Kicker
import com.lucas.predictaapp.ui.components.PredictaCard
import com.lucas.predictaapp.ui.components.PredictaPullRefresh
import com.lucas.predictaapp.ui.components.RowDivider
import com.lucas.predictaapp.ui.components.ScreenTopBar
import com.lucas.predictaapp.ui.components.SearchField
import com.lucas.predictaapp.ui.components.TransactionRow
import com.lucas.predictaapp.ui.components.pageWash
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs
import com.lucas.predictaapp.ui.utils.isInMonth
import com.lucas.predictaapp.ui.utils.monthYearLabel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val ScreenPadding = 20.dp
private val dayFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "AR"))

/**
 * Tab Actividad: los movimientos del mes agrupados por día, con resumen arriba, búsqueda y
 * filtro por categoría. Buscar recorre todo el historial, no solo el mes. Tap edita, long-press borra.
 */
@Composable
fun TransactionsScreen() {
    val expenses by ExpensesRepository.expensesWithCategory.collectAsStateWithLifecycle(emptyList())
    val allCategories by CategoryRepository.categories.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()

    var month by remember { mutableStateOf(YearMonth.now()) }
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var pendingDelete by remember { mutableStateOf<ExpenseWithCategory?>(null) }

    // Llegada con filtro (categoría tocada en la home): mes y categoría de una.
    val incoming = TransactionsFilter.pending
    LaunchedEffect(incoming) {
        TransactionsFilter.consume()?.let {
            month = it.month
            selectedCategory = it.category
            query = ""
        }
    }

    val emojiFor: (String) -> String = remember(allCategories) {
        val map = allCategories.associate { it.name to it.emoji }
        val fn: (String) -> String = { name -> map[name] ?: ExpenseCategories.emojiFor(name) }
        fn
    }

    val searching = query.isNotBlank()
    val scope0 = if (searching) expenses else expenses.filter { isInMonth(it.dateMillis, month) }

    // Chips: solo categorías con movimientos en lo que se está mirando, por frecuencia de uso.
    val usedCategories = remember(scope0) {
        scope0.groupingBy { it.category }.eachCount().entries
            .sortedByDescending { it.value }
            .map { it.key }
    }

    val filtered = scope0.filter { e ->
        (selectedCategory == null || e.category == selectedCategory) &&
            (!searching || e.merchant.contains(query, ignoreCase = true) || e.category.contains(query, ignoreCase = true))
    }
    val byDay = filtered
        .sortedByDescending { it.dateMillis }
        .groupBy { it.dateMillis.toLocalDate() }

    PredictaPullRefresh(
        modifier = Modifier
            .fillMaxSize()
            .pageWash(),
        onRefresh = { SyncManager.pullAll() },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "top") {
                ScreenTopBar(
                    title = "Actividad",
                    modifier = Modifier.padding(horizontal = ScreenPadding),
                ) {
                    HeaderIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior", { month = month.minusMonths(1); selectedCategory = null })
                    HeaderIconButton(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        "Mes siguiente",
                        { if (month < YearMonth.now()) { month = month.plusMonths(1); selectedCategory = null } },
                        enabled = month < YearMonth.now(),
                    )
                }
            }

            item(key = "summary") {
                SummaryCard(
                    title = if (searching) "Resultados" else monthYearLabel(month).substringBefore(' '),
                    items = filtered,
                    modifier = Modifier.padding(horizontal = ScreenPadding),
                )
            }

            item(key = "search") {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Buscar comercio o categoría",
                    modifier = Modifier.padding(horizontal = ScreenPadding),
                )
            }

            if (usedCategories.size > 1) {
                item(key = "chips") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = ScreenPadding),
                    ) {
                        item(key = "all") {
                            ChoiceChip(text = "Todas", selected = selectedCategory == null, onClick = { selectedCategory = null })
                        }
                        items(usedCategories, key = { it }) { name ->
                            ChoiceChip(
                                text = name,
                                leading = emojiFor(name),
                                selected = name == selectedCategory,
                                onClick = { selectedCategory = if (selectedCategory == name) null else name },
                            )
                        }
                    }
                }
            }

            when {
                expenses.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        emoji = "🧾",
                        title = "Todavía no hay movimientos",
                        body = "Cargá tu primer gasto con el + o contáselo al chat.",
                    )
                }
                filtered.isEmpty() -> item(key = "none") {
                    EmptyState(
                        emoji = "🔍",
                        title = if (searching) "Sin resultados" else "Nada en ${monthYearLabel(month).lowercase()}",
                        body = if (searching) "Probá con otro comercio o categoría." else "Mirá otro mes con las flechas de arriba.",
                    )
                }
            }

            byDay.forEach { (day, items) ->
                item(key = "day_$day") {
                    DayGroup(
                        day = day,
                        items = items,
                        emojiFor = emojiFor,
                        onEdit = { editingExpense = it.expense },
                        onDelete = { pendingDelete = it },
                        modifier = Modifier.padding(horizontal = ScreenPadding).animateItem(),
                    )
                }
            }
        }
    }

    editingExpense?.let { expense ->
        EditExpenseSheet(
            expense = expense,
            categories = allCategories,
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

/** Resumen en la hero: lo gastado, cuántos movimientos y los ingresos si hubo. */
@Composable
private fun SummaryCard(title: String, items: List<ExpenseWithCategory>, modifier: Modifier = Modifier) {
    val spent = items.filter { !it.isIncome }.sumOf { it.amount }
    val income = items.filter { it.isIncome }.sumOf { it.amount }
    val count = items.size
    val dim = Color.White.copy(alpha = 0.85f)
    HeroCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "$title · $count ${if (count == 1) "movimiento" else "movimientos"}",
                    style = PredictaTypography.label.copy(fontSize = 13.sp),
                    color = dim,
                )
                Text(
                    text = "$${spent.fmtArs()}",
                    style = PredictaTypography.display.copy(fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-1).sp),
                    color = Color.White,
                )
            }
            if (income > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Ingresos", style = PredictaTypography.label.copy(fontSize = 13.sp), color = dim)
                    Text("+$${income.fmtArs()}", style = PredictaTypography.rowAmount, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DayGroup(
    day: LocalDate,
    items: List<ExpenseWithCategory>,
    emojiFor: (String) -> String,
    onEdit: (ExpenseWithCategory) -> Unit,
    onDelete: (ExpenseWithCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val net = items.sumOf { if (it.isIncome) it.amount else -it.amount }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Kicker(dayLabel(day), modifier = Modifier.weight(1f))
            Text(
                text = (if (net >= 0) "+" else "−") + "$${kotlin.math.abs(net).fmtArs()}",
                style = PredictaTypography.label.copy(fontSize = 12.sp),
                color = if (net > 0) PredictaColors.success else PredictaColors.kicker,
            )
        }
        PredictaCard(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { i, item ->
                TransactionRow(
                    item = item,
                    onClick = { onEdit(item) },
                    onLongClick = { onDelete(item) },
                    emojiFor = emojiFor,
                    contentPadding = PaddingValues(vertical = 13.dp),
                    showDate = false,
                )
                if (i < items.lastIndex) RowDivider()
            }
        }
    }
}

private fun dayLabel(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> buildString {
            append(day.format(dayFormatter))
            if (day.year != today.year) append(" ${day.year}")
        }
    }
}
