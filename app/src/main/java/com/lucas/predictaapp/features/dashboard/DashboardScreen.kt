package com.lucas.predictaapp.features.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.local.UserPreferencesRepository
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.data.model.ExpenseWithCategory
import com.lucas.predictaapp.data.model.FixedExpense
import com.lucas.predictaapp.data.model.FixedExpenseStatus
import com.lucas.predictaapp.data.model.Subscription
import com.lucas.predictaapp.data.model.computeStatus
import com.lucas.predictaapp.data.model.effectiveBillingDay
import com.lucas.predictaapp.data.model.isChargedThisMonth
import com.lucas.predictaapp.data.model.toLocalDate
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.data.repository.FixedExpensesRepository
import com.lucas.predictaapp.data.repository.NotificationsRepository
import com.lucas.predictaapp.data.repository.SubscriptionsRepository
import com.lucas.predictaapp.data.repository.SyncManager
import com.lucas.predictaapp.features.dashboard.components.CategoryBreakdownCard
import com.lucas.predictaapp.features.dashboard.components.CategorySlice
import com.lucas.predictaapp.features.dashboard.components.EditExpenseSheet
import com.lucas.predictaapp.ui.components.HeaderIconButton
import com.lucas.predictaapp.features.dashboard.components.SpendComparison
import com.lucas.predictaapp.features.dashboard.components.SpendHeroCard
import com.lucas.predictaapp.features.dashboard.components.UpcomingPayment
import com.lucas.predictaapp.features.dashboard.components.UpcomingPaymentsCard
import com.lucas.predictaapp.features.dashboard.components.upcomingSummary
import com.lucas.predictaapp.ui.components.GlyphBadge
import com.lucas.predictaapp.features.transactions.TransactionsFilter
import com.lucas.predictaapp.ui.utils.fmtArs
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.components.DeleteExpenseDialog
import com.lucas.predictaapp.ui.components.DiceMark
import com.lucas.predictaapp.ui.components.Kicker
import com.lucas.predictaapp.ui.components.PredictaCard
import com.lucas.predictaapp.ui.components.PredictaPullRefresh
import com.lucas.predictaapp.ui.components.SectionHeader
import com.lucas.predictaapp.ui.components.TransactionRow
import com.lucas.predictaapp.ui.components.pageWash
import com.lucas.predictaapp.ui.navigation.Screen
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.theme.categoryColor
import com.lucas.predictaapp.ui.utils.isInMonth
import com.lucas.predictaapp.ui.utils.monthYearLabel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val ScreenPadding = 20.dp
private const val MIN_DAY_TO_COMPARE = 7

/**
 * Home: responde "¿cuánto gasté este mes?" con un solo número grande y deja abajo, en orden de
 * urgencia, lo que viene (pagos), lo último que pasó (movimientos) y en qué se va la plata.
 */
@Composable
fun DashboardScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val expenses by ExpensesRepository.expensesWithCategory.collectAsStateWithLifecycle(emptyList())
    val subscriptions by SubscriptionsRepository.subscriptions.collectAsStateWithLifecycle(emptyList())
    val notifications by NotificationsRepository.notifications.collectAsStateWithLifecycle(emptyList())
    val userSetup by UserPreferencesRepository.getUserSetup(context).collectAsStateWithLifecycle(null)
    val allCategories by CategoryRepository.categories.collectAsStateWithLifecycle(emptyList())
    val fixedExpenses by FixedExpensesRepository.fixedExpenses.collectAsStateWithLifecycle(emptyList())

    val emojiFor: (String) -> String = remember(allCategories) {
        val map = allCategories.associate { it.name to it.emoji }
        val fn: (String) -> String = { name -> map[name] ?: ExpenseCategories.emojiFor(name) }
        fn
    }
    val colorFor: (String) -> Color = remember(allCategories) {
        val map = allCategories.associate { cat ->
            cat.name to runCatching { Color(android.graphics.Color.parseColor(cat.color)) }.getOrNull()
        }
        val fn: (String) -> Color = { name -> map[name] ?: categoryColor(name) }
        fn
    }

    val name = userSetup?.name.orEmpty()

    // Mes navegado: la home muestra el mes seleccionado (hoy por defecto).
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    val viewingCurrent = selectedMonth == YearMonth.now()

    // Los gastos fijos solo existen como configuración del mes en curso; en meses pasados la home
    // muestra únicamente los movimientos registrados.
    val fixedMonthly = if (viewingCurrent) fixedExpenses.sumOf { it.amount } else 0
    val monthExpenses = expenses.filter { isInMonth(it.dateMillis, selectedMonth) }
    val variableSpent = monthExpenses.filter { !it.isIncome }.sumOf { it.amount }
    val monthSpend = variableSpent + fixedMonthly
    val comparison = spendComparison(expenses, selectedMonth, variableSpent)
    val upcomingFixed = if (viewingCurrent) upcomingFixed(fixedExpenses) else emptyList()
    val upcomingSubs = if (viewingCurrent) upcomingSubscriptions(subscriptions) else emptyList()
    val slices = categorySlices(monthExpenses, emojiFor)
    val recent = monthExpenses.sortedByDescending { it.dateMillis }.take(3)

    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var pendingDelete by remember { mutableStateOf<ExpenseWithCategory?>(null) }

    PredictaPullRefresh(
        modifier = Modifier
            .fillMaxSize()
            .pageWash(),
        onRefresh = { SyncManager.pullAll() },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item(key = "top") {
                Stagger(0, visible) {
                    Column(Modifier.padding(horizontal = ScreenPadding), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        TopBar(
                            hasUnread = notifications.any { it.unread },
                            onNotificationsClick = { onNavigate(Screen.Notifications.route) },
                        )
                        Greeting(
                            name = name,
                            month = selectedMonth,
                            canGoNext = !viewingCurrent,
                            onPrev = { selectedMonth = selectedMonth.minusMonths(1) },
                            onNext = { if (selectedMonth < YearMonth.now()) selectedMonth = selectedMonth.plusMonths(1) },
                        )
                    }
                }
            }
            item(key = "hero") {
                Stagger(1, visible) {
                    SpendHeroCard(
                        monthSpend = monthSpend,
                        fixed = fixedMonthly,
                        variable = variableSpent,
                        comparison = comparison,
                        label = if (viewingCurrent) "Gastaste este mes" else "Gastaste en ${monthYearLabel(selectedMonth).lowercase()}",
                        modifier = Modifier.padding(horizontal = ScreenPadding),
                    )
                }
            }
            if (slices.isNotEmpty()) {
                item(key = "categories") {
                    Stagger(2, visible) {
                        Column(Modifier.padding(horizontal = ScreenPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column {
                                SectionHeader(title = "En qué se va")
                                // Los fijos no tienen categoría: el desglose es del día a día.
                                Text(
                                    text = "Día a día · $${variableSpent.fmtArs()}",
                                    style = PredictaTypography.label,
                                    color = PredictaColors.inkSecondary,
                                )
                            }
                            CategoryBreakdownCard(
                                slices = slices,
                                onCategoryClick = { name ->
                                    TransactionsFilter.open(name, selectedMonth)
                                    onNavigate(Screen.Transactions.route)
                                },
                                onOthersClick = {
                                    TransactionsFilter.open(null, selectedMonth)
                                    onNavigate(Screen.Transactions.route)
                                },
                            )
                        }
                    }
                }
            }
            if (upcomingFixed.isNotEmpty() || upcomingSubs.isNotEmpty()) {
                item(key = "upcoming") {
                    Stagger(3, visible) {
                        Column(Modifier.padding(horizontal = ScreenPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column {
                                SectionHeader(
                                    title = "Por pagar este mes",
                                    actionLabel = if (fixedExpenses.isNotEmpty()) "Ver fijos" else null,
                                    onAction = { onNavigate(Screen.FixedExpenses.route) },
                                )
                                Text(
                                    text = upcomingSummary(upcomingFixed + upcomingSubs),
                                    style = PredictaTypography.label,
                                    color = if (upcomingFixed.any { it.overdue }) PredictaColors.danger else PredictaColors.inkSecondary,
                                )
                            }
                            UpcomingPaymentsCard(
                                fixed = upcomingFixed,
                                subscriptions = upcomingSubs,
                                fixedAllPaid = upcomingFixed.isEmpty() && fixedExpenses.any { it.active },
                                monthShort = monthShort(selectedMonth),
                                onTogglePaid = { fixed -> scope.launch { FixedExpensesRepository.togglePaid(fixed) } },
                                onOpenFixed = { onNavigate(Screen.FixedExpenses.route) },
                                onOpenSubscriptions = { onNavigate(Screen.Subscriptions.route) },
                            )
                        }
                    }
                }
            } else if (viewingCurrent && fixedExpenses.isNotEmpty()) {
                item(key = "allPaid") {
                    Stagger(3, visible) {
                        AllPaidNote(
                            monthName = monthYearLabel(selectedMonth).substringBefore(' ').lowercase(),
                            onClick = { onNavigate(Screen.FixedExpenses.route) },
                            modifier = Modifier.padding(horizontal = ScreenPadding),
                        )
                    }
                }
            }
            item(key = "recent") {
                Stagger(4, visible) {
                    Column(Modifier.padding(horizontal = ScreenPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionHeader(
                            title = if (viewingCurrent) "Últimos movimientos" else "Movimientos de ${monthYearLabel(selectedMonth).lowercase()}",
                            actionLabel = if (recent.isNotEmpty()) "Ver todo" else null,
                            onAction = { onNavigate(Screen.Transactions.route) },
                        )
                        PredictaCard(modifier = Modifier.fillMaxWidth()) {
                            if (recent.isEmpty()) {
                                Text(
                                    text = if (viewingCurrent) "Todavía no cargaste nada este mes. Probá con el + o dictándole un gasto a Predicta."
                                    else "Sin movimientos en ${monthYearLabel(selectedMonth).lowercase()}.",
                                    style = PredictaTypography.body,
                                    color = PredictaColors.inkSecondary,
                                    modifier = Modifier.padding(vertical = 16.dp),
                                )
                            }
                            recent.forEachIndexed { i, item ->
                                TransactionRow(
                                    item = item,
                                    onClick = { editingExpense = item.expense },
                                    onLongClick = { pendingDelete = item },
                                    emojiFor = emojiFor,
                                    colorFor = colorFor,
                                    contentPadding = PaddingValues(vertical = 13.dp),
                                )
                                if (i < recent.lastIndex) HorizontalDivider(thickness = 1.dp, color = PredictaColors.separator)
                            }
                        }
                    }
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
                scope.launch { ExpensesRepository.delete(item.expense.id) }
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun TopBar(hasUnread: Boolean, onNotificationsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DiceMark(size = 34.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = "predicta",
            style = PredictaTypography.title.copy(fontSize = PredictaTypography.heading.fontSize * 1.22f),
            color = PredictaColors.ink,
        )
        Spacer(Modifier.weight(1f))
        HeaderIconButton(
            icon = Icons.Outlined.Notifications,
            contentDescription = if (hasUnread) "Notificaciones, hay nuevas" else "Notificaciones",
            onClick = onNotificationsClick,
            badge = hasUnread,
        )
    }
}

@Composable
private fun Greeting(
    name: String,
    month: YearMonth,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Kicker(monthYearLabel(month))
            Text(
                text = if (name.isBlank()) "Hola" else "Hola, $name",
                style = PredictaTypography.title,
                color = PredictaColors.ink,
            )
        }
        HeaderIconButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior", onPrev)
        Spacer(Modifier.width(6.dp))
        HeaderIconButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente", onNext, enabled = canGoNext)
    }
}

@Composable
private fun Stagger(index: Int, visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 360, delayMillis = index * 55)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 360, delayMillis = index * 55),
                initialOffsetY = { it / 4 },
            ),
    ) {
        content()
    }
}

/**
 * Gasto del día a día de [month] contra el mes anterior. En el mes en curso compara a la misma
 * altura (hasta el mismo día); en meses pasados compara meses completos. Null si no hay base.
 */
private fun spendComparison(
    all: List<ExpenseWithCategory>,
    month: YearMonth,
    variableSpent: Int,
): SpendComparison? {
    if (variableSpent == 0) return null
    val previous = month.minusMonths(1)
    val cutDay = if (month == YearMonth.now()) LocalDate.now().dayOfMonth else Int.MAX_VALUE
    // Los primeros días del mes la comparación es puro ruido ("100% menos" con un café cargado).
    if (cutDay < MIN_DAY_TO_COMPARE) return null
    val previousSpent = all
        .filter { !it.isIncome && isInMonth(it.dateMillis, previous) && it.dateMillis.toLocalDate().dayOfMonth <= cutDay }
        .sumOf { it.amount }
    if (previousSpent == 0) return null
    // Redondear nunca debe decir "100% menos" si se gastó algo.
    val pct = ((variableSpent - previousSpent) * 100.0 / previousSpent).roundToInt().coerceAtLeast(-99)
    val monthName = previous.month.getDisplayName(TextStyle.FULL, Locale("es", "AR"))
    return SpendComparison(pct = pct, previousMonthName = monthName)
}

/** "nov" para la hoja de calendario de los pagos. */
private fun monthShort(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.SHORT, Locale("es", "AR")).trimEnd('.').take(3)

/** Cuando ya está todo pago: una línea de confirmación en vez de que la sección desaparezca. */
@Composable
private fun AllPaidNote(monthName: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PredictaCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlyphBadge(size = 36.dp, container = PredictaColors.successSoft) {
                Icon(Icons.Default.Check, contentDescription = null, tint = PredictaColors.success, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "No te queda nada por pagar en $monthName",
                style = PredictaTypography.label.copy(fontSize = 15.sp),
                color = PredictaColors.ink,
            )
        }
    }
}

/** Gastos fijos activos sin pagar, del más urgente (vencidos primero) al más lejano. */
private fun upcomingFixed(fixed: List<FixedExpense>): List<UpcomingPayment> {
    val today = LocalDate.now().dayOfMonth
    return fixed
        .filter { it.active && it.computeStatus() != FixedExpenseStatus.PAGADO }
        .map { UpcomingPayment(key = "f${it.id}", name = it.name, amount = it.amount, dueDay = it.dueDayOfMonth, daysUntil = it.dueDayOfMonth - today, fixed = it) }
        .sortedBy { it.daysUntil }
}

/** Suscripciones que todavía no se cobraron este mes (se cobran solas el día de cobro). */
private fun upcomingSubscriptions(subscriptions: List<Subscription>): List<UpcomingPayment> {
    val today = LocalDate.now().dayOfMonth
    return subscriptions
        .filter { it.active && !it.isChargedThisMonth() && it.effectiveBillingDay() >= today }
        .map { UpcomingPayment(key = "s${it.id}", name = it.service, amount = it.monthly, dueDay = it.effectiveBillingDay(), daysUntil = it.effectiveBillingDay() - today) }
        .sortedBy { it.daysUntil }
}

private fun categorySlices(monthExpenses: List<ExpenseWithCategory>, emojiFor: (String) -> String): List<CategorySlice> {
    val spent = monthExpenses.filter { !it.isIncome }
    val total = spent.sumOf { it.amount }
    if (total == 0) return emptyList()
    return spent
        .groupBy { it.category }
        .map { (name, items) -> name to items.sumOf { it.amount } }
        .sortedByDescending { it.second }
        .map { (name, amount) -> CategorySlice(name, emojiFor(name), amount, (amount * 100.0 / total).roundToInt()) }
}
