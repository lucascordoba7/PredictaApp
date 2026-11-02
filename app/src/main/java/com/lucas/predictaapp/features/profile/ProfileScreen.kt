package com.lucas.predictaapp.features.profile

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.capture.PaymentNotificationListener
import com.lucas.predictaapp.capture.PaymentScreenReader
import com.lucas.predictaapp.data.local.UserPreferencesRepository
import com.lucas.predictaapp.data.model.CategoryType
import com.lucas.predictaapp.data.model.Personality
import com.lucas.predictaapp.data.model.UserProfile
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.data.repository.FixedExpensesRepository
import com.lucas.predictaapp.data.repository.MIN_EXPENSES_FOR_PERSONALITY
import com.lucas.predictaapp.data.repository.NotificationsRepository
import com.lucas.predictaapp.data.repository.PersonalityRepository
import com.lucas.predictaapp.data.repository.SubscriptionsRepository
import com.lucas.predictaapp.ui.components.PredictaDialog
import com.lucas.predictaapp.ui.components.SecondaryButton
import com.lucas.predictaapp.ui.components.pageWash
import com.lucas.predictaapp.ui.navigation.Screen
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.utils.fmtArs
import com.lucas.predictaapp.ui.utils.isCurrentMonth
import com.lucas.predictaapp.ui.utils.monthYearLabel
import java.time.YearMonth

// Rutas ficticias: abren los Ajustes de Android en vez de navegar.
private const val CAPTURE_SETTINGS = "capture_settings"
private const val SCREEN_READER_SETTINGS = "screen_reader_settings"

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
    // Se re-chequea al volver de Ajustes, donde el usuario habilita el acceso.
    var captureEnabled by remember { mutableStateOf(PaymentNotificationListener.isEnabled(context)) }
    var screenReaderEnabled by remember { mutableStateOf(PaymentScreenReader.isEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        captureEnabled = PaymentNotificationListener.isEnabled(context)
        screenReaderEnabled = PaymentScreenReader.isEnabled(context)
        onPauseOrDispose { }
    }
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

    val categories by CategoryRepository.categories.collectAsStateWithLifecycle(emptyList())
    val subsMonthly = subscriptions.filter { it.active }.sumOf { it.monthly }
    val expenseCats = categories.count { it.type == CategoryType.EXPENSE }
    val incomeCats = categories.count { it.type == CategoryType.INCOME }

    Column(
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier
            .fillMaxSize()
            .pageWash()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 32.dp),
    ) {
        ProfileHeader(name = user.name, email = user.email)

        ProfileMonthCard(
            monthLabel = monthYearLabel(YearMonth.now()).substringBefore(' '),
            income = user.monthIncome,
            spent = user.monthSpent + user.fixedMonthly,
            available = user.availableNow,
        )

        PersonalityBanner(state = personalityState)

        ProfileMenuSection(
            title = "Finanzas",
            items = listOf(
                MenuItem(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    label = "Gastos fijos",
                    subtitle = if (fixedExpenses.isEmpty()) "Alquiler, expensas, servicios…"
                    else "${fixedExpenses.size} ${plural(fixedExpenses.size, "configurado", "configurados")} · $${fixedMonthly.fmtArs()}/mes",
                    route = Screen.FixedExpenses.route,
                ),
                MenuItem(
                    icon = Icons.Outlined.Autorenew,
                    label = "Suscripciones",
                    subtitle = if (subscriptions.isEmpty()) "Netflix, Spotify, el gimnasio…"
                    else "${subscriptions.count { it.active }} ${plural(subscriptions.count { it.active }, "activa", "activas")} · $${subsMonthly.fmtArs()}/mes",
                    route = Screen.Subscriptions.route,
                ),
                MenuItem(
                    icon = Icons.Outlined.Category,
                    label = "Categorías",
                    subtitle = "$expenseCats de gasto · $incomeCats de ingreso",
                    route = Screen.Categories.route,
                ),
                MenuItem(
                    icon = Icons.Outlined.AutoAwesome,
                    label = "Captura automática",
                    subtitle = "Lee avisos de Mercado Pago y bancos",
                    route = CAPTURE_SETTINGS,
                    status = if (captureEnabled) MenuStatus.ACTIVE else MenuStatus.INACTIVE,
                ),
                MenuItem(
                    icon = Icons.Outlined.Visibility,
                    label = "Lectura de comprobantes",
                    subtitle = "Desde la pantalla de pago",
                    route = SCREEN_READER_SETTINGS,
                    status = if (screenReaderEnabled) MenuStatus.ACTIVE else MenuStatus.INACTIVE,
                ),
                MenuItem(
                    icon = Icons.Outlined.StarOutline,
                    label = "Score crediticio",
                    subtitle = "Tu situación en el BCRA",
                    status = MenuStatus.SOON,
                ),
            ),
            onItemClick = { item ->
                when (item.route) {
                    CAPTURE_SETTINGS -> context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                    )
                    SCREEN_READER_SETTINGS -> context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                    )
                    null -> Unit
                    else -> onNavigate(item.route)
                }
            },
        )

        ProfileMenuSection(
            title = "General",
            items = listOf(
                MenuItem(
                    icon = Icons.Outlined.Notifications,
                    label = "Notificaciones",
                    badge = notifications.count { it.unread }.takeIf { it > 0 }?.toString(),
                    route = Screen.Notifications.route,
                ),
                MenuItem(
                    icon = Icons.Outlined.Person,
                    label = "Datos personales",
                    status = MenuStatus.SOON,
                ),
                MenuItem(
                    icon = Icons.Outlined.Settings,
                    label = "Configuración",
                    status = MenuStatus.SOON,
                ),
            ),
            onItemClick = { item -> item.route?.let { onNavigate(it) } },
        )

        SecondaryButton(
            text = "Cerrar sesión",
            onClick = { showSignOutDialog = true },
            leadingIcon = Icons.AutoMirrored.Outlined.Logout,
            content = PredictaColors.danger,
        )
    }

    if (showSignOutDialog) {
        PredictaDialog(
            title = "¿Cerrar sesión?",
            text = "Se borran los datos de este teléfono. Lo sincronizado queda en tu cuenta y vuelve cuando entrás con el mismo mail.",
            confirmLabel = "Cerrar sesión",
            onConfirm = { showSignOutDialog = false; onSignOut() },
            onDismiss = { showSignOutDialog = false },
            destructive = true,
        )
    }
}

private fun plural(n: Int, one: String, many: String) = if (n == 1) one else many
