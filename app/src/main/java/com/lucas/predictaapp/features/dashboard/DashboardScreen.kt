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
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.ui.theme.categoryColor
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.data.model.FixedExpenseStatus
import com.lucas.predictaapp.data.model.computeStatus
import com.lucas.predictaapp.data.repository.FixedExpensesRepository
import com.lucas.predictaapp.data.repository.NotificationsRepository
import com.lucas.predictaapp.data.repository.SubscriptionsRepository
import com.lucas.predictaapp.features.dashboard.components.AddGoalCard
import com.lucas.predictaapp.features.dashboard.components.AvailableNowCard
import com.lucas.predictaapp.features.dashboard.components.CategorySpendingCard
import com.lucas.predictaapp.features.dashboard.components.DashboardHeader
import com.lucas.predictaapp.features.dashboard.components.FixedExpensesCard
import com.lucas.predictaapp.features.dashboard.components.SectionLabel
import com.lucas.predictaapp.features.dashboard.components.TransactionsCard
import com.lucas.predictaapp.features.dashboard.components.ZombiesCard
import com.lucas.predictaapp.ui.components.PredictaPullRefresh
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.isCurrentMonth

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val expenses by ExpensesRepository.expenses.collectAsStateWithLifecycle(emptyList())
    val subscriptions by SubscriptionsRepository.subscriptions.collectAsStateWithLifecycle(emptyList())
    val notifications by NotificationsRepository.notifications.collectAsStateWithLifecycle(emptyList())
    val userSetup by UserPreferencesRepository.getUserSetup(context).collectAsStateWithLifecycle(null)
    val allCategories by CategoryRepository.categories.collectAsStateWithLifecycle(emptyList())
    val fixedExpenses by FixedExpensesRepository.fixedExpenses.collectAsStateWithLifecycle(emptyList())

    val expenseCategoryNames = remember(allCategories) {
        allCategories.filter { it.type == CategoryType.EXPENSE }.map { it.name }.toSet()
            .ifEmpty { null }
    }
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
    val totalFixedPaid = fixedExpenses.filter {
        it.computeStatus() == com.lucas.predictaapp.data.model.FixedExpenseStatus.PAGADO
    }.sumOf { it.amount }
    val totalFixedPending = fixedMonthly - totalFixedPaid

    val monthExpenses = expenses.filter { isCurrentMonth(it.dateMillis) }
    val totalSpent = monthExpenses.filter {
        expenseCategoryNames?.contains(it.category) ?: (it.category != "Ingreso")
    }.sumOf { it.amount }
    val availableNow = (income - fixedMonthly - totalSpent).coerceAtLeast(0)

    val zombies = subscriptions.filter { it.zombie }

    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    PredictaPullRefresh(
        modifier = Modifier
            .fillMaxSize()
            .background(PredictaColors.charcoal),
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
                DashboardHeader(name = name)
            }
        }
        item {
            StaggerCard(2, visible) {
                AvailableNowCard(
                    availableNow = availableNow,
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
                    expenseCategoryNames = expenseCategoryNames,
                )
            }
        }
        item {
            StaggerCard(4, visible) {
                FixedExpensesCard(
                    items = fixedExpenses,
                    onManageClick = { onNavigate(com.lucas.predictaapp.ui.navigation.Screen.FixedExpenses.route) },
                )
            }
        }
        item { StaggerCard(5, visible) { SectionLabel("Tus metas") } }
        item { StaggerCard(6, visible) { AddGoalCard() } }
        if (zombies.isNotEmpty()) {
            item { StaggerCard(7, visible) { SectionLabel("Predicta detectó") } }
            item {
                StaggerCard(8, visible) {
                    ZombiesCard(zombies = zombies, onCancel = {})
                }
            }
        }
        if (expenses.isNotEmpty()) {
            item { StaggerCard(9, visible) { SectionLabel("Actividad reciente") } }
            item {
                StaggerCard(10, visible) {
                    TransactionsCard(
                        expenses = expenses,
                        onDelete = { expense -> scope.launch { ExpensesRepository.delete(expense.id) } },
                        emojiFor = emojiFor,
                        colorFor = colorFor,
                    )
                }
            }
        }
    }
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
