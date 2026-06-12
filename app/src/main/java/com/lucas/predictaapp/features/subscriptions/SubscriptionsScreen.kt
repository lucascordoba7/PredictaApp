package com.lucas.predictaapp.features.subscriptions

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.model.Category
import com.lucas.predictaapp.data.model.CategoryType
import com.lucas.predictaapp.data.model.Subscription
import com.lucas.predictaapp.data.model.computedUsagePct
import com.lucas.predictaapp.data.model.daysSinceLastUsed
import com.lucas.predictaapp.data.model.effectiveBillingDay
import com.lucas.predictaapp.data.model.isChargedThisMonth
import com.lucas.predictaapp.data.model.isZombie
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.SubscriptionsRepository
import com.lucas.predictaapp.ui.components.AnimatedAmount
import com.lucas.predictaapp.data.repository.SyncManager
import com.lucas.predictaapp.ui.components.PredictaPullRefresh
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.ThousandsVisualTransformation
import com.lucas.predictaapp.ui.utils.fmtArs
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PredictaColors.charcoal),
    ) {
        PredictaPullRefresh(modifier = Modifier.fillMaxSize(), onRefresh = { SyncManager.pullAll() }) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                item {
                    TopBar(totalMonthly = totalMonthly, onBack = onBack)
                }

                if (subs.isEmpty()) {
                    item { EmptyState() }
                }

                if (zombies.isNotEmpty()) {
                    item { SectionHeader("🧟 Zombies — no las usás") }
                    items(zombies, key = { it.id }) { sub ->
                        SubscriptionCard(
                            sub = sub,
                            today = today,
                            onEdit = { editingItem = sub; showSheet = true },
                            onDelete = { deletingItem = sub },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                if (active.isNotEmpty()) {
                    item { SectionHeader("✅ Activas") }
                    items(active, key = { it.id }) { sub ->
                        SubscriptionCard(
                            sub = sub,
                            today = today,
                            onEdit = { editingItem = sub; showSheet = true },
                            onDelete = { deletingItem = sub },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(PredictaColors.charcoal)
                .padding(
                    horizontal = PredictaDimensions.Spacing.screenPadding,
                    vertical = PredictaDimensions.Spacing.base,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(PredictaColors.amber)
                    .clickable { editingItem = null; showSheet = true }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = PredictaColors.charcoal,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Agregar suscripción",
                        style = PredictaTypography.bodyTight.copy(color = PredictaColors.charcoal),
                    )
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false; editingItem = null },
            sheetState = sheetState,
            containerColor = PredictaColors.surface,
        ) {
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
                onCancel = { showSheet = false; editingItem = null },
            )
        }
    }

    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            containerColor = PredictaColors.surface,
            title = {
                Text(
                    text = "¿Cancelar suscripción?",
                    style = PredictaTypography.cardTitle,
                    color = PredictaColors.cream,
                )
            },
            text = {
                Text(
                    text = "\"${item.service}\" se eliminará de tus suscripciones.",
                    style = PredictaTypography.small,
                    color = PredictaColors.cream60,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { SubscriptionsRepository.cancel(item.id) }
                    deletingItem = null
                }) {
                    Text("Eliminar", color = PredictaColors.coral)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text("Cancelar", color = PredictaColors.cream60)
                }
            },
        )
    }
}

@Composable
private fun TopBar(totalMonthly: Int, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = PredictaDimensions.Spacing.screenPadding,
                vertical = PredictaDimensions.Spacing.base,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.offset(x = (-12).dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PredictaColors.cream60)
            }
            Text(
                text = "Suscripciones",
                style = PredictaTypography.cardTitle,
                color = PredictaColors.cream,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.sm))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
                .background(PredictaColors.surface)
                .padding(PredictaDimensions.Spacing.base),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Total mensual",
                    style = PredictaTypography.caption,
                    color = PredictaColors.cream35,
                )
                AnimatedAmount(
                    value = totalMonthly,
                    style = PredictaTypography.kpiInline,
                    color = PredictaColors.cream,
                    formatter = { "\$${it.fmtArs()}" },
                )
            }
            Text(
                text = "por mes",
                style = PredictaTypography.small,
                color = PredictaColors.cream35,
            )
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = PredictaTypography.monoCap,
        color = PredictaColors.cream35,
        modifier = Modifier.padding(
            horizontal = PredictaDimensions.Spacing.screenPadding,
            vertical = PredictaDimensions.Spacing.sm,
        ),
    )
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = PredictaDimensions.Spacing.screenPadding,
                vertical = PredictaDimensions.Spacing.xl,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Sin suscripciones cargadas",
                style = PredictaTypography.body.copy(color = PredictaColors.cream60),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tocá + para agregar la primera",
                style = PredictaTypography.small.copy(color = PredictaColors.cream35),
            )
        }
    }
}

private fun formatDaysAgo(days: Int): String = when (days) {
    0 -> "hoy"
    1 -> "ayer"
    else -> "hace ${days} días"
}

@Composable
private fun SubscriptionCard(
    sub: Subscription,
    today: LocalDate,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isZombie = sub.isZombie(today)
    val usagePct = sub.computedUsagePct(today)
    val days = sub.daysSinceLastUsed(today)
    val accentColor = if (isZombie) PredictaColors.coral else PredictaColors.green
    val accentSoft = if (isZombie) PredictaColors.coralSoft else PredictaColors.greenSoft

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = PredictaDimensions.Spacing.screenPadding,
                vertical = PredictaDimensions.Spacing.xs,
            )
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .then(
                if (isZombie) Modifier.border(
                    1.dp,
                    PredictaColors.coralSoft,
                    RoundedCornerShape(PredictaDimensions.Radius.card),
                ) else Modifier,
            )
            .padding(PredictaDimensions.Spacing.base),
        verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = sub.initial,
                    style = PredictaTypography.bodyTight,
                    color = accentColor,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sub.service,
                        style = PredictaTypography.bodyTight,
                        color = PredictaColors.cream,
                    )
                    if (isZombie) {
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = "Zombie",
                            style = PredictaTypography.caption,
                            color = PredictaColors.coral,
                            modifier = Modifier
                                .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
                                .background(PredictaColors.coralSoft)
                                .padding(horizontal = PredictaDimensions.Spacing.sm, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text = buildString {
                        append("\$${sub.monthly.fmtArs()}/mes")
                        if (days != null) append(" · ${formatDaysAgo(days)}")
                    },
                    style = PredictaTypography.small,
                    color = PredictaColors.cream60,
                )
                val charged = sub.isChargedThisMonth()
                Text(
                    text = "Cobra el día ${sub.effectiveBillingDay()} · " +
                        if (charged) "cobrada este mes" else "pendiente este mes",
                    style = PredictaTypography.caption,
                    color = if (charged) PredictaColors.green else PredictaColors.cream35,
                )
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = PredictaColors.cream35,
                    modifier = Modifier.size(16.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = PredictaColors.cream35,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Uso del mes",
                    style = PredictaTypography.caption,
                    color = PredictaColors.cream35,
                )
                Text(
                    text = "${usagePct}%",
                    style = PredictaTypography.caption,
                    color = accentColor,
                )
            }
            LinearProgressIndicator(
                progress = { usagePct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(PredictaDimensions.Radius.pill)),
                color = accentColor,
                trackColor = PredictaColors.surfaceHigh,
                strokeCap = StrokeCap.Round,
            )
        }

        if (isZombie) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(PredictaDimensions.Radius.sm))
                    .background(PredictaColors.coralSoft)
                    .clickable(onClick = onDelete)
                    .padding(vertical = PredictaDimensions.Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Cancelar suscripción",
                    style = PredictaTypography.small,
                    color = PredictaColors.coral,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionForm(
    initial: Subscription?,
    categories: List<Category>,
    onSave: (Subscription) -> Unit,
    onCancel: () -> Unit,
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

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PredictaColors.amber,
        unfocusedBorderColor = PredictaColors.lineStrong,
        focusedLabelColor = PredictaColors.amber,
        unfocusedLabelColor = PredictaColors.cream35,
        cursorColor = PredictaColors.amber,
        focusedTextColor = PredictaColors.cream,
        unfocusedTextColor = PredictaColors.cream,
    )

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "AR")) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PredictaDimensions.Spacing.base)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.md),
    ) {
        Text(
            text = if (initial == null) "Nueva suscripción" else "Editar suscripción",
            style = PredictaTypography.cardTitle,
            color = PredictaColors.cream,
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre del servicio") },
            placeholder = { Text("Ej: Netflix, Spotify…", color = PredictaColors.cream35) },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            singleLine = true,
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter { c -> c.isDigit() } },
            label = { Text("Monto mensual") },
            placeholder = { Text("4,500", color = PredictaColors.cream35) },
            prefix = { Text("$", color = PredictaColors.cream60) },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            visualTransformation = ThousandsVisualTransformation,
            singleLine = true,
        )

        OutlinedTextField(
            value = billingDay,
            onValueChange = { billingDay = it.filter { c -> c.isDigit() }.take(2) },
            label = { Text("Día de cobro (1-31)") },
            placeholder = { Text("Ej: 5", color = PredictaColors.cream35) },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors,
            isError = billingDay.isNotEmpty() && !billingDayValid,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            singleLine = true,
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Categoría del gasto",
                style = PredictaTypography.caption.copy(color = PredictaColors.cream60),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.id }) { cat ->
                    val selected = cat.id == categoryId
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
                            .background(if (selected) PredictaColors.amber else PredictaColors.surfaceHigh)
                            .clickable { categoryId = cat.id }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(text = cat.emoji)
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = cat.name,
                            style = PredictaTypography.small.copy(
                                color = if (selected) PredictaColors.charcoal else PredictaColors.cream60,
                            ),
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Última vez que la usaste",
                style = PredictaTypography.caption.copy(color = PredictaColors.cream60),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
                    .border(
                        1.dp,
                        PredictaColors.lineStrong,
                        RoundedCornerShape(PredictaDimensions.Radius.card),
                    )
                    .clickable { showDatePicker = true }
                    .padding(
                        horizontal = PredictaDimensions.Spacing.base,
                        vertical = PredictaDimensions.Spacing.md,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedDate.format(dateFormatter),
                    style = PredictaTypography.body.copy(color = PredictaColors.cream),
                )
                val days = java.time.temporal.ChronoUnit.DAYS
                    .between(selectedDate, LocalDate.now())
                    .toInt()
                    .coerceAtLeast(0)
                Text(
                    text = formatDaysAgo(days),
                    style = PredictaTypography.caption.copy(color = PredictaColors.cream35),
                )
            }
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.sm))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
                .background(if (isValid) PredictaColors.amber else PredictaColors.cream12)
                .clickable(enabled = isValid) {
                    onSave(
                        Subscription(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            service = name.trim(),
                            initial = name.trim().take(1).uppercase(),
                            monthly = amount.toInt(),
                            lastUsedDate = selectedDate.toString(),
                            billingDay = billingDay.toInt(),
                            categoryId = categoryId,
                            // Preservar estado de facturación al editar.
                            lastChargedMonthKey = initial?.lastChargedMonthKey ?: "",
                            active = initial?.active ?: true,
                        ),
                    )
                }
                .padding(vertical = PredictaDimensions.Spacing.base),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Guardar",
                style = PredictaTypography.bodyTight.copy(
                    color = if (isValid) PredictaColors.charcoal else PredictaColors.cream35,
                ),
            )
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Cancelar",
                style = PredictaTypography.small,
                color = PredictaColors.cream60,
            )
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.base))
    }

    if (showDatePicker) {
        val zoneId = ZoneId.of("UTC")
        val initialMillis = selectedDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK", color = PredictaColors.amber) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = PredictaColors.cream60)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = PredictaColors.surface,
            ),
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
