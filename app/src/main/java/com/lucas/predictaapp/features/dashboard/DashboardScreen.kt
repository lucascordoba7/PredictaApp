package com.lucas.predictaapp.features.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import com.lucas.predictaapp.features.onboarding.PredictaLogoDice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import com.lucas.predictaapp.data.local.UserPreferencesRepository
import com.lucas.predictaapp.data.model.CategoryType
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.ui.theme.categoryColor
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.data.model.FixedExpenseStatus
import com.lucas.predictaapp.data.model.computeStatus
import com.lucas.predictaapp.data.repository.FixedExpensesRepository
import com.lucas.predictaapp.data.repository.NotificationsRepository
import com.lucas.predictaapp.data.repository.SubscriptionsRepository
import com.lucas.predictaapp.data.repository.SyncManager
import com.lucas.predictaapp.features.dashboard.components.AddGoalCard
import com.lucas.predictaapp.features.dashboard.components.AvailableNowCard
import com.lucas.predictaapp.features.dashboard.components.CategorySpendingCard
import com.lucas.predictaapp.features.dashboard.components.DashboardHeader
import com.lucas.predictaapp.features.dashboard.components.EditExpenseSheet
import com.lucas.predictaapp.features.dashboard.components.FixedExpensesCard
import com.lucas.predictaapp.features.dashboard.components.SectionLabel
import com.lucas.predictaapp.features.dashboard.components.TransactionsCard
import com.lucas.predictaapp.features.dashboard.components.SubscriptionsCard
import com.lucas.predictaapp.ui.components.PredictaPullRefresh
import androidx.compose.ui.text.style.TextAlign
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.isInMonth
import com.lucas.predictaapp.ui.utils.monthYearLabel
import java.time.YearMonth

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

    val name = userSetup?.name ?: ""
    val income = userSetup?.income ?: 0
    val fixedMonthly = fixedExpenses.sumOf { it.amount }

    // Mes navegado: el dashboard muestra los datos del mes seleccionado (hoy por defecto).
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    val viewingCurrent = selectedMonth == YearMonth.now()

    // Los gastos fijos solo tienen estado (pagado/pendiente) del mes en curso; en meses
    // pasados el dashboard muestra únicamente los movimientos registrados.
    val totalFixedPaid = if (viewingCurrent) {
        fixedExpenses.filter { it.computeStatus() == FixedExpenseStatus.PAGADO }.sumOf { it.amount }
    } else 0
    val totalFixedPending = if (viewingCurrent) fixedMonthly - totalFixedPaid else 0

    val monthExpenses = expenses.filter { isInMonth(it.dateMillis, selectedMonth) }
    val totalSpent = monthExpenses.filter { !it.isIncome }.sumOf { it.amount }
    val monthSpend = totalSpent + if (viewingCurrent) fixedMonthly else 0

    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    var editingExpense by remember { mutableStateOf<Expense?>(null) }

    PredictaPullRefresh(
        modifier = Modifier
            .fillMaxSize()
            .background(PredictaColors.charcoal),
        onRefresh = { SyncManager.pullAll() },
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PredictaColors.charcoal),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            StaggerCard(0, visible) {
                TopBar(
                    notificationCount = notifications.count { it.unread },
                    onNotificationsClick = { onNavigate(com.lucas.predictaapp.ui.navigation.Screen.Notifications.route) },
                )
            }
        }
        item {
            StaggerCard(1, visible) {
                DashboardHeader(
                    name = name,
                    month = selectedMonth,
                    onPrevMonth = { selectedMonth = selectedMonth.minusMonths(1) },
                    onNextMonth = {
                        if (selectedMonth < YearMonth.now()) selectedMonth = selectedMonth.plusMonths(1)
                    },
                    onResetMonth = { selectedMonth = YearMonth.now() },
                )
            }
        }
        item {
            StaggerCard(2, visible) {
                AvailableNowCard(
                    monthSpend = monthSpend,
                    income = income,
                    totalSpent = totalSpent,
                    totalFixedPaid = totalFixedPaid,
                    totalFixedPending = totalFixedPending,
                )
            }
        }
        item {
            StaggerCard(3, visible) {
                CategorySpendingCard(
                    expenses = monthExpenses,
                    income = income,
                    emojiFor = emojiFor,
                    colorFor = colorFor,
                )
            }
        }
        // Cards de estado presente (fijos, metas, subs): solo tienen sentido en el mes en curso.
        if (viewingCurrent) {
            item {
                StaggerCard(4, visible) {
                    FixedExpensesCard(
                        items = fixedExpenses,
                        onManageClick = { onNavigate(com.lucas.predictaapp.ui.navigation.Screen.FixedExpenses.route) },
                    )
                }
            }
            item { StaggerCard(5, false) { SectionLabel("Tus metas") } }
            item { StaggerCard(6, false) { AddGoalCard() } }
            if (subscriptions.isNotEmpty()) {
                item {
                    StaggerCard(8, visible) {
                        SubscriptionsCard(
                            subscriptions = subscriptions,
                            onManageClick = { onNavigate(com.lucas.predictaapp.ui.navigation.Screen.Subscriptions.route) },
                        )
                    }
                }
            }
        }
        if (monthExpenses.isNotEmpty()) {
            item {
                StaggerCard(9, visible) {
                    SectionLabel(if (viewingCurrent) "Actividad reciente" else "Movimientos de ${monthYearLabel(selectedMonth).lowercase()}")
                }
            }
            item {
                StaggerCard(10, visible) {
                    TransactionsCard(
                        // El home muestra un vistazo; el historial completo vive en Actividad.
                        expenses = monthExpenses.sortedByDescending { it.dateMillis }.take(5),
                        onDelete = { expense -> scope.launch { ExpensesRepository.delete(expense.id) } },
                        onEdit = { editingExpense = it },
                        onSeeAll = { onNavigate(com.lucas.predictaapp.ui.navigation.Screen.Transactions.route) },
                        emojiFor = emojiFor,
                        colorFor = colorFor,
                    )
                }
            }
        } else if (!viewingCurrent) {
            item {
                Text(
                    text = "Sin movimientos en ${monthYearLabel(selectedMonth).lowercase()}",
                    style = PredictaTypography.small,
                    color = PredictaColors.cream35,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = TextAlign.Center,
                )
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
}

@Composable
private fun TopBar(notificationCount: Int, onNotificationsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PredictaLogoDice(size = 28.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "predicta",
            style = PredictaTypography.bodyTight.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = PredictaColors.amber,
                letterSpacing = (-0.5).sp,
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
        BadgedBox(
            badge = {
                if (notificationCount > 0) {
                    Badge(containerColor = PredictaColors.coral) {
                        Text(text = notificationCount.toString(), color = PredictaColors.cream, fontSize = 9.sp)
                    }
                }
            },
        ) {
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notificaciones",
                    tint = PredictaColors.cream60,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun StaggerCard(index: Int, visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 360, delayMillis = index * 55)) +
            slideInVertically(
                animationSpec = tween(durationMillis = 360, delayMillis = index * 55),
                initialOffsetY = { it / 3 },
            ),
    ) {
        content()
    }
}
