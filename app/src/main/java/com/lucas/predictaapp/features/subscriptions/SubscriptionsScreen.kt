package com.lucas.predictaapp.features.subscriptions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.model.Category
import com.lucas.predictaapp.data.model.CategoryType
import com.lucas.predictaapp.data.model.Subscription
import com.lucas.predictaapp.data.model.ZOMBIE_THRESHOLD_DAYS
import com.lucas.predictaapp.data.model.daysSinceLastUsed
import com.lucas.predictaapp.data.model.effectiveBillingDay
import com.lucas.predictaapp.data.model.isChargedThisMonth
import com.lucas.predictaapp.data.model.isZombie
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.SubscriptionsRepository
import com.lucas.predictaapp.data.repository.SyncManager
import com.lucas.predictaapp.ui.components.AnimatedAmount
import com.lucas.predictaapp.ui.components.ChoiceChip
import com.lucas.predictaapp.ui.components.EmptyState
import com.lucas.predictaapp.ui.components.GlyphBadge
import com.lucas.predictaapp.ui.components.HeaderIconButton
import com.lucas.predictaapp.ui.components.HeroCard
import com.lucas.predictaapp.ui.components.Kicker
import com.lucas.predictaapp.ui.components.PickerRow
import com.lucas.predictaapp.ui.components.PredictaCard
import com.lucas.predictaapp.ui.components.PredictaDialog
import com.lucas.predictaapp.ui.components.PredictaPullRefresh
import com.lucas.predictaapp.ui.components.PredictaSheet
import com.lucas.predictaapp.ui.components.PredictaTextField
import com.lucas.predictaapp.ui.components.PrimaryButton
import com.lucas.predictaapp.ui.components.RowDivider
import com.lucas.predictaapp.ui.components.ScreenTopBar
import com.lucas.predictaapp.ui.components.SheetHeader
import com.lucas.predictaapp.ui.components.StatusPill
import com.lucas.predictaapp.ui.components.pageWash
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.ThousandsVisualTransformation
import com.lucas.predictaapp.ui.utils.fmtArs
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * Suscripciones: total mensual arriba, zombies (las que no usás hace más de
 * [ZOMBIE_THRESHOLD_DAYS] días) primero y después las activas. Tocar edita; mantener borra.
 */
@Composable
fun SubscriptionsScreen(onBack: () -> Unit) {
    val subs by SubscriptionsRepository.subscriptions.collectAsStateWithLifecycle(emptyList())
    val allCategories by CategoryRepository.categories.collectAsStateWithLifecycle(emptyList())
    val expenseCategories = remember(allCategories) { allCategories.filter { it.type == CategoryType.EXPENSE } }
    val today = remember { LocalDate.now() }
    val zombies = subs.filter { it.isZombie(today) }
    val active = subs.filter { !it.isZombie(today) }
    val totalMonthly = subs.sumOf { it.monthly }
    val scope = rememberCoroutineScope()

    var showSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Subscription?>(null) }
    var deletingItem by remember { mutableStateOf<Subscription?>(null) }

    fun openForm(sub: Subscription?) {
        editingItem = sub
        showSheet = true
    }

    PredictaPullRefresh(
        modifier = Modifier
            .fillMaxSize()
            .pageWash(),
        onRefresh = { SyncManager.pullAll() },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "top") {
                ScreenTopBar(title = "Suscripciones", onBack = onBack) {
                    HeaderIconButton(Icons.Default.Add, "Agregar suscripción", { openForm(null) })
                }
            }

            if (subs.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        emoji = "🔁",
                        title = "Sin suscripciones",
                        body = "Cargá Netflix, Spotify o el gimnasio y cada mes las registro solas el día de cobro.",
                        action = { PrimaryButton("Agregar suscripción", { openForm(null) }, leadingIcon = Icons.Default.Add) },
                    )
                }
            } else {
                item(key = "summary") {
                    HeroCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
                        Text("Te cuestan por mes", style = PredictaTypography.label.copy(fontSize = 13.sp), color = Color.White.copy(alpha = 0.85f))
                        AnimatedAmount(
                            value = totalMonthly,
                            style = PredictaTypography.display.copy(fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-1).sp),
                            color = Color.White,
                            formatter = { "$${it.fmtArs()}" },
                        )
                        Text(
                            text = buildString {
                                append("${subs.size} ${if (subs.size == 1) "suscripción" else "suscripciones"}")
                                if (zombies.isNotEmpty()) append(" · ${zombies.size} sin usar")
                            },
                            style = PredictaTypography.label.copy(fontSize = 13.sp),
                            color = Color.White,
                        )
                    }
                }
            }

            listOf(
                "Zombies · hace más de $ZOMBIE_THRESHOLD_DAYS días que no las usás" to zombies,
                "Activas" to active,
            ).forEach { (label, group) ->
                if (group.isNotEmpty()) {
                    item(key = "g_$label") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.animateItem()) {
                            Kicker(label, modifier = Modifier.padding(start = 4.dp), color = if (group === zombies) PredictaColors.danger else PredictaColors.kicker)
                            PredictaCard(modifier = Modifier.fillMaxWidth()) {
                                group.forEachIndexed { i, sub ->
                                    SubscriptionRow(sub, today, onEdit = { openForm(sub) }, onDelete = { deletingItem = sub })
                                    if (i < group.lastIndex) RowDivider(inset = 54.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        PredictaSheet(onDismiss = { showSheet = false; editingItem = null }, wash = true) {
            SubscriptionForm(
                initial = editingItem,
                categories = expenseCategories,
                onSave = { sub ->
                    scope.launch {
                        SubscriptionsRepository.upsert(sub)
                        showSheet = false
                        editingItem = null
                    }
                },
                onDelete = { sub ->
                    showSheet = false
                    editingItem = null
                    deletingItem = sub
                },
                onClose = { showSheet = false; editingItem = null },
            )
        }
    }

    deletingItem?.let { item ->
        PredictaDialog(
            title = "¿Eliminar suscripción?",
            text = "\"${item.service}\" deja de registrarse cada mes. Los cobros ya cargados quedan.",
            confirmLabel = "Eliminar",
            destructive = true,
            onConfirm = {
                scope.launch { SubscriptionsRepository.cancel(item.id) }
                deletingItem = null
            },
            onDismiss = { deletingItem = null },
        )
    }
}

private fun formatDaysAgo(days: Int): String = when (days) {
    0 -> "hoy"
    1 -> "ayer"
    else -> "hace $days días"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubscriptionRow(sub: Subscription, today: LocalDate, onEdit: () -> Unit, onDelete: () -> Unit) {
    val zombie = sub.isZombie(today)
    val days = sub.daysSinceLastUsed(today)
    val charged = sub.isChargedThisMonth()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onEdit, onLongClick = onDelete)
            .padding(vertical = 12.dp),
    ) {
        GlyphBadge(container = if (zombie) PredictaColors.dangerSoft else PredictaColors.accentSoft) {
            Text(
                text = sub.initial.ifBlank { sub.service.take(1).uppercase() },
                style = PredictaTypography.label.copy(fontSize = 16.sp, fontWeight = PredictaTypography.rowAmount.fontWeight),
                color = if (zombie) PredictaColors.danger else PredictaColors.accent,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(sub.service, style = PredictaTypography.rowTitle, color = PredictaColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = "Día ${sub.effectiveBillingDay()} · " + if (charged) "ya cobrada" else "por cobrar",
                style = PredictaTypography.rowSubtitle,
                color = PredictaColors.inkSecondary,
            )
            if (days != null) {
                Text(
                    text = "La usaste ${formatDaysAgo(days)}",
                    style = PredictaTypography.rowSubtitle,
                    color = if (zombie) PredictaColors.danger else PredictaColors.inkSecondary,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("$${sub.monthly.fmtArs()}", style = PredictaTypography.rowAmount, color = PredictaColors.ink)
            if (charged) StatusPill("Cobrada", PredictaColors.successSoft, PredictaColors.success)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SubscriptionForm(
    initial: Subscription?,
    categories: List<Category>,
    onSave: (Subscription) -> Unit,
    onDelete: (Subscription) -> Unit,
    onClose: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.service ?: "") }
    var amount by remember { mutableStateOf(initial?.monthly?.toString() ?: "") }
    var billingDay by remember { mutableStateOf((initial?.billingDay ?: 1).toString()) }
    val defaultCategoryId = remember(categories) {
        val byName = categories.associateBy { it.name }
        (byName["Suscripciones"] ?: byName["Servicios"] ?: byName["Otros"] ?: categories.firstOrNull())?.id ?: 0L
    }
    var categoryId by remember { mutableStateOf(initial?.categoryId?.takeIf { it > 0 } ?: defaultCategoryId) }
    var selectedDate by remember {
        mutableStateOf(
            initial?.lastUsedDate
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: LocalDate.now(),
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val billingDayValid = billingDay.toIntOrNull()?.let { it in 1..31 } == true
    val isValid = name.isNotBlank() &&
        amount.toIntOrNull()?.let { it > 0 } == true &&
        billingDayValid && categoryId > 0L

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "AR")) }
    val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(selectedDate, LocalDate.now()).toInt().coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
    ) {
        SheetHeader(
            title = if (initial == null) "Nueva suscripción" else "Editar suscripción",
            onClose = onClose,
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PredictaTextField(
                value = name,
                onValueChange = { name = it },
                label = "Servicio",
                placeholder = "Netflix, Spotify…",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PredictaTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() }.take(10) },
                    label = "Por mes",
                    placeholder = "4.500",
                    prefix = "$",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    visualTransformation = ThousandsVisualTransformation,
                    modifier = Modifier.weight(1.6f),
                )
                PredictaTextField(
                    value = billingDay,
                    onValueChange = { billingDay = it.filter { c -> c.isDigit() }.take(2) },
                    label = "Cobra el día",
                    placeholder = "5",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Kicker("Categoría del gasto")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { cat ->
                        ChoiceChip(text = cat.name, leading = cat.emoji, selected = cat.id == categoryId, onClick = { categoryId = cat.id })
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Kicker("Última vez que la usaste")
                PickerRow(
                    icon = Icons.Outlined.CalendarMonth,
                    title = formatDaysAgo(daysAgo).replaceFirstChar { it.uppercase() },
                    subtitle = selectedDate.format(dateFormatter),
                    trailing = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    onClick = { showDatePicker = true },
                )
            }
        }
        PrimaryButton(
            text = "Guardar",
            enabled = isValid,
            onClick = {
                val base = initial ?: Subscription(id = UUID.randomUUID().toString(), service = "", initial = "", monthly = 0)
                // copy: preserva estado de facturación, activa y usuario al editar.
                onSave(
                    base.copy(
                        service = name.trim(),
                        initial = name.trim().take(1).uppercase(),
                        monthly = amount.toInt(),
                        lastUsedDate = selectedDate.toString(),
                        billingDay = billingDay.toInt(),
                        categoryId = categoryId,
                    ),
                )
            },
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = if (initial == null) 16.dp else 0.dp),
        )
        if (initial != null) {
            TextButton(onClick = { onDelete(initial) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Eliminar suscripción", style = PredictaTypography.label, color = PredictaColors.danger)
            }
        }
    }

    if (showDatePicker) {
        val utc = ZoneId.of("UTC")
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(utc).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= System.currentTimeMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(utc).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Listo", style = PredictaTypography.label, color = PredictaColors.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", style = PredictaTypography.label, color = PredictaColors.inkSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = PredictaColors.card),
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = PredictaColors.card,
                    selectedDayContainerColor = PredictaColors.accent,
                    selectedDayContentColor = PredictaColors.onAccent,
                    todayDateBorderColor = PredictaColors.accent,
                    todayContentColor = PredictaColors.accent,
                ),
            )
        }
    }
}
