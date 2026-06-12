package com.lucas.predictaapp.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.local.UserPreferencesRepository
import com.lucas.predictaapp.data.model.Personality
import com.lucas.predictaapp.data.model.UserProfile
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.data.repository.FixedExpensesRepository
import com.lucas.predictaapp.data.repository.MIN_EXPENSES_FOR_PERSONALITY
import com.lucas.predictaapp.data.repository.NotificationsRepository
import com.lucas.predictaapp.data.repository.PersonalityRepository
import com.lucas.predictaapp.data.repository.SubscriptionsRepository
import com.lucas.predictaapp.ui.navigation.Screen
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.isCurrentMonth

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val context = LocalContext.current
    val userSetup by UserPreferencesRepository.getUserSetup(context).collectAsStateWithLifecycle(null)
    val expenses by ExpensesRepository.expensesWithCategory.collectAsStateWithLifecycle(emptyList())
    val subscriptions by SubscriptionsRepository.subscriptions.collectAsStateWithLifecycle(emptyList())
    val notifications by NotificationsRepository.notifications.collectAsStateWithLifecycle(emptyList())
    val fixedExpenses by FixedExpensesRepository.fixedExpenses.collectAsStateWithLifecycle(emptyList())
    val scrollState = rememberScrollState()
    var showSignOutDialog by remember { mutableStateOf(false) }

    val income = userSetup?.income ?: 0
    val fixedMonthly = fixedExpenses.sumOf { it.amount }
    val monthExpenses = remember(expenses) { expenses.filter { isCurrentMonth(it.dateMillis) } }
    val totalSpent = remember(monthExpenses) { monthExpenses.filter { !it.isIncome }.sumOf { it.amount } }
    val availableNow = (income - fixedMonthly - totalSpent).coerceAtLeast(0)

    val user = UserProfile(
        name = userSetup?.name ?: "",
        email = userSetup?.email ?: "",
        monthIncome = income,
        monthSpent = totalSpent,
        previousMonthSpent = 0,
        monthProjected = 0,
        score = 0,
        fixedMonthly = fixedMonthly,
        availableNow = availableNow,
        daysToPayday = 0,
        personality = Personality(name = "", description = "", weeklySpike = ""),
    )

    var personalityState by remember {
        mutableStateOf<PersonalityBannerState>(PersonalityBannerState.Loading)
    }

    LaunchedEffect(monthExpenses.size, totalSpent) {
        if (monthExpenses.size < MIN_EXPENSES_FOR_PERSONALITY) {
            personalityState = PersonalityBannerState.Insufficient(
                transactionCount = monthExpenses.size,
                requiredCount = MIN_EXPENSES_FOR_PERSONALITY,
            )
        } else {
            personalityState = PersonalityBannerState.Loading
            personalityState = runCatching {
                PersonalityRepository.analyze(monthExpenses, income, fixedMonthly)
            }.fold(
                onSuccess = { PersonalityBannerState.Ready(it) },
                onFailure = { PersonalityBannerState.Insufficient(monthExpenses.size, MIN_EXPENSES_FOR_PERSONALITY) },
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(PredictaColors.charcoal)
            .verticalScroll(scrollState)
            .padding(horizontal = PredictaDimensions.Spacing.screenPadding),
    ) {
        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.lg))

        ProfileHeader(user = user)

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.base))

        ProfileStats(user = user)

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.base))

        PersonalityBanner(
            state = personalityState,
            onPress = null,
        )

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.lg))

        ProfileMenuSection(
            title = "Finanzas",
            items = listOf(
                MenuItem(
                    icon = Icons.Default.Receipt,
                    label = "Gastos Fijos",
                    subtitle = "${fixedExpenses.size} configurados",
                    route = Screen.FixedExpenses.route,
                ),
                MenuItem(
                    icon = Icons.Default.CreditCard,
                    label = "Suscripciones",
                    subtitle = "${subscriptions.size} activas",
                    route = Screen.Subscriptions.route,
                ),
                MenuItem(
                    icon = Icons.Default.Category,
                    label = "Categorías",
                    route = Screen.Categories.route,
                ),
                MenuItem(
                    icon = Icons.Default.Star,
                    label = "Score crediticio",
                    disabled = true,
                ),
            ),
            onItemClick = { item -> item.route?.let { onNavigate(it) } },
        )

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.base))

        ProfileMenuSection(
            title = "General",
            items = listOf(
                MenuItem(
                    icon = Icons.Default.Notifications,
                    label = "Notificaciones",
                    badge = notifications.count { it.unread }.takeIf { it > 0 }?.toString(),
                    route = Screen.Notifications.route,
                ),
                MenuItem(
                    icon = Icons.Default.Person,
                    label = "Datos personales",
                    disabled = true,
                ),
                MenuItem(
                    icon = Icons.Default.Settings,
                    label = "Configuración",
                    disabled = true,
                ),
            ),
            onItemClick = { item -> item.route?.let { onNavigate(it) } },
        )

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.lg))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
                .background(PredictaColors.coralSoft)
                .clickable { showSignOutDialog = true }
                .padding(vertical = PredictaDimensions.Spacing.base),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Cerrar sesión",
                style = PredictaTypography.bodyTight,
                color = PredictaColors.coral,
            )
        }

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.xxl))
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = PredictaColors.surface,
            title = {
                Text(
                    text = "¿Cerrar sesión?",
                    style = PredictaTypography.cardTitle,
                    color = PredictaColors.cream,
                )
            },
            text = {
                Text(
                    text = "Tus datos quedan en este dispositivo. Para volver a entrar vas a necesitar registrarte de nuevo.",
                    style = PredictaTypography.small,
                    color = PredictaColors.cream60,
                )
            },
            confirmButton = {
                TextButton(onClick = { showSignOutDialog = false; onSignOut() }) {
                    Text("Sí, cerrar", color = PredictaColors.coral)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancelar", color = PredictaColors.cream60)
                }
            },
        )
    }
}
