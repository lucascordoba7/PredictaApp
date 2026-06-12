package com.lucas.predictaapp.features.fixedexpenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.model.FixedExpense
import com.lucas.predictaapp.data.model.FixedExpenseStatus
import com.lucas.predictaapp.data.model.computeStatus
import com.lucas.predictaapp.data.repository.FixedExpensesRepository
import com.lucas.predictaapp.data.repository.SyncManager
import com.lucas.predictaapp.ui.components.PredictaPullRefresh
import com.lucas.predictaapp.ui.theme.IBMPlexMono
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.ThousandsVisualTransformation
import com.lucas.predictaapp.ui.utils.fmtArs
import kotlinx.coroutines.launch

private enum class FixedExpensesTab { ESTE_MES, GESTIONAR }

// Ciclo de colores para los círculos de inicial en Gestionar
private val initialColors = listOf(
    Color(0xFFF5A524), // amber
    Color(0xFFE26B4A), // coral
    Color(0xFF7BC47A), // green
    Color(0xFF6A92E8), // blue
    Color(0xFF9D7AE8), // purple
    Color(0xFF6FD3C5), // teal
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedExpensesScreen(onBack: () -> Unit) {
    val items by FixedExpensesRepository.fixedExpenses.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTab by remember { mutableStateOf(FixedExpensesTab.ESTE_MES) }
    var showSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<FixedExpense?>(null) }
    var deletingItem by remember { mutableStateOf<FixedExpense?>(null) }

    val pagados = items.filter { it.computeStatus() == FixedExpenseStatus.PAGADO }
    val vencidos = items.filter { it.computeStatus() == FixedExpenseStatus.VENCIDO }
    val pendientes = items.filter { it.computeStatus() == FixedExpenseStatus.PENDIENTE }
    val totalMonthly = items.sumOf { it.amount }
    val totalPagado = pagados.sumOf { it.amount }
    val progressFraction = if (totalMonthly > 0) totalPagado.toFloat() / totalMonthly else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PredictaColors.charcoal),
    ) {
        PredictaPullRefresh(modifier = Modifier.fillMaxSize(), onRefresh = { SyncManager.pullAll() }) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = if (selectedTab == FixedExpensesTab.GESTIONAR) 96.dp else 56.dp,
            ),
        ) {
            // ── HEADER ──
            item {
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
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = PredictaColors.cream60,
                            )
                        }
                        Text(
                            text = "Gastos Fijos",
                            style = PredictaTypography.cardTitle,
                            color = PredictaColors.cream,
                            modifier = Modifier.weight(1f),
                        )
                        if (selectedTab == FixedExpensesTab.GESTIONAR) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PredictaColors.amberSoft)
                                    .clickable { editingItem = null; showSheet = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Agregar",
                                    tint = PredictaColors.amber,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(PredictaDimensions.Spacing.sm))

                    // Resumen card
                    if (selectedTab == FixedExpensesTab.ESTE_MES) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
                                .background(PredictaColors.surface)
                                .padding(PredictaDimensions.Spacing.base),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${pagados.size} de ${items.size} pagados",
                                    style = PredictaTypography.bodyTight.copy(color = PredictaColors.amber),
                                )
                                Text(
                                    text = "este mes",
                                    style = PredictaTypography.caption.copy(
                                        fontFamily = IBMPlexMono,
                                        color = PredictaColors.cream35,
                                    ),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(PredictaColors.surfaceHigh),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(PredictaColors.amber),
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "$ ${totalPagado.fmtArs()} de $ ${totalMonthly.fmtArs()} cubiertos",
                                style = PredictaTypography.caption.copy(
                                    fontFamily = IBMPlexMono,
                                    color = PredictaColors.cream35,
                                ),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
                                .background(PredictaColors.surface)
                                .padding(
                                    horizontal = PredictaDimensions.Spacing.base,
                                    vertical = PredictaDimensions.Spacing.md,
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${items.size} gastos configurados",
                                style = PredictaTypography.small.copy(color = PredictaColors.cream60),
                            )
                            Text(
                                text = "$ ${totalMonthly.fmtArs()}/mes",
                                style = PredictaTypography.small.copy(
                                    fontFamily = IBMPlexMono,
                                    fontWeight = FontWeight.Medium,
                                    color = PredictaColors.amber,
                                ),
                            )
                        }
                    }

                    Spacer(Modifier.height(PredictaDimensions.Spacing.md))

                    // Tab switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                            .background(PredictaColors.surface)
                            .border(1.dp, PredictaColors.line, RoundedCornerShape(999.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TabPill(
                            label = "Este mes",
                            active = selectedTab == FixedExpensesTab.ESTE_MES,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = FixedExpensesTab.ESTE_MES },
                        )
                        TabPill(
                            label = "Gestionar",
                            active = selectedTab == FixedExpensesTab.GESTIONAR,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = FixedExpensesTab.GESTIONAR },
                        )
                    }
                }
            }

            // ── ESTE MES ──
            if (selectedTab == FixedExpensesTab.ESTE_MES) {
                if (items.isEmpty()) {
                    item {
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
                                    text = "Sin gastos fijos",
                                    style = PredictaTypography.body.copy(color = PredictaColors.cream60),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Agregá tus gastos en la pestaña Gestionar",
                                    style = PredictaTypography.small.copy(color = PredictaColors.cream35),
                                )
                            }
                        }
                    }
                }

                if (vencidos.isNotEmpty()) {
                    item { SectionHeader("VENCIDOS", PredictaColors.coral) }
                    items(vencidos, key = { it.id }) { item ->
                        ChecklistRow(
                            item = item,
                            status = FixedExpenseStatus.VENCIDO,
                            onTogglePaid = { scope.launch { FixedExpensesRepository.togglePaid(item) } },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                if (pendientes.isNotEmpty()) {
                    item { SectionHeader("POR PAGAR", PredictaColors.pending) }
                    items(pendientes, key = { it.id }) { item ->
                        ChecklistRow(
                            item = item,
                            status = FixedExpenseStatus.PENDIENTE,
                            onTogglePaid = { scope.launch { FixedExpensesRepository.togglePaid(item) } },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                if (pagados.isNotEmpty()) {
                    item { SectionHeader("PAGADOS", PredictaColors.green) }
                    items(pagados, key = { it.id }) { item ->
                        ChecklistRow(
                            item = item,
                            status = FixedExpenseStatus.PAGADO,
                            onTogglePaid = { scope.launch { FixedExpensesRepository.togglePaid(item) } },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(PredictaDimensions.Spacing.base))
                    Text(
                        text = "Tocá el círculo para marcar como pagado",
                        style = PredictaTypography.caption.copy(color = PredictaColors.cream35),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PredictaDimensions.Spacing.screenPadding),
                    )
                }
            }

            // ── GESTIONAR ──
            if (selectedTab == FixedExpensesTab.GESTIONAR) {
                if (items.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = PredictaDimensions.Spacing.screenPadding,
                                    vertical = PredictaDimensions.Spacing.xl,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Todavía no cargaste gastos fijos",
                                style = PredictaTypography.small.copy(color = PredictaColors.cream35),
                            )
                        }
                    }
                }

                items(items, key = { it.id }) { item ->
                    val colorIndex = (item.name.hashCode() and 0x7FFFFFFF) % initialColors.size
                    val color = initialColors[colorIndex]
                    ManageRow(
                        item = item,
                        accentColor = color,
                        onEdit = { editingItem = item; showSheet = true },
                        onDelete = { deletingItem = item },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
        }

        // ── FAB agregar (solo en Gestionar) ──
        if (selectedTab == FixedExpensesTab.GESTIONAR) {
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
                            text = "Agregar gasto fijo",
                            style = PredictaTypography.bodyTight.copy(color = PredictaColors.charcoal),
                        )
                    }
                }
            }
        }
    }

    // ── Bottom sheet form ──
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false; editingItem = null },
            sheetState = sheetState,
            containerColor = PredictaColors.surface,
        ) {
            FixedExpenseForm(
                initial = editingItem,
                onSave = { item ->
                    scope.launch {
                        FixedExpensesRepository.upsert(item)
                        showSheet = false
                        editingItem = null
                    }
                },
                onCancel = { showSheet = false; editingItem = null },
            )
        }
    }

    // ── Delete confirmation ──
    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            containerColor = PredictaColors.surface,
            title = {
                Text(
                    text = "¿Eliminar gasto fijo?",
                    style = PredictaTypography.cardTitle,
                    color = PredictaColors.cream,
                )
            },
            text = {
                Text(
                    text = "\"${item.name}\" se eliminará de tus gastos fijos mensuales.",
                    style = PredictaTypography.small,
                    color = PredictaColors.cream60,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { FixedExpensesRepository.delete(item.id) }
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

// ── Tab pill ──
@Composable
private fun TabPill(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) PredictaColors.amber else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = PredictaTypography.small.copy(
                fontWeight = FontWeight.Medium,
                color = if (active) PredictaColors.charcoal else PredictaColors.cream60,
                fontSize = 13.sp,
            ),
        )
    }
}

// ── Section header ──
@Composable
private fun SectionHeader(label: String, color: Color) {
    Text(
        text = label,
        style = PredictaTypography.monoCap.copy(color = color),
        modifier = Modifier.padding(
            horizontal = PredictaDimensions.Spacing.screenPadding,
            vertical = PredictaDimensions.Spacing.sm,
        ),
    )
}

// ── Checklist row (Este mes tab) ──
@Composable
private fun ChecklistRow(
    item: FixedExpense,
    status: FixedExpenseStatus,
    onTogglePaid: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPaid = status == FixedExpenseStatus.PAGADO
    val accentColor = when (status) {
        FixedExpenseStatus.PAGADO -> PredictaColors.green
        FixedExpenseStatus.VENCIDO -> PredictaColors.coral
        FixedExpenseStatus.PENDIENTE -> PredictaColors.pending
    }
    val surface = PredictaColors.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PredictaDimensions.Spacing.screenPadding, vertical = 4.dp)
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .drawBehind {
                drawRect(surface)
                drawRect(
                    color = accentColor.copy(alpha = if (isPaid) 0.35f else 0.6f),
                    topLeft = Offset.Zero,
                    size = Size(3.dp.toPx(), size.height),
                )
            }
            .alpha(if (isPaid) 0.55f else 1f)
            .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Large checkbox
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isPaid) PredictaColors.greenSoft
                    else accentColor.copy(alpha = 0.08f),
                )
                .border(
                    width = if (isPaid) 0.dp else 2.dp,
                    color = if (isPaid) Color.Transparent else accentColor,
                    shape = CircleShape,
                )
                .clickable(onClick = onTogglePaid),
            contentAlignment = Alignment.Center,
        ) {
            if (isPaid) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Marcar como no pagado",
                    tint = PredictaColors.green,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = PredictaTypography.bodyTight.copy(
                    color = PredictaColors.cream,
                    textDecoration = if (isPaid) TextDecoration.LineThrough else TextDecoration.None,
                ),
            )
            Text(
                text = if (status == FixedExpenseStatus.VENCIDO) "venció el día ${item.dueDayOfMonth}"
                else "vence el día ${item.dueDayOfMonth}",
                style = PredictaTypography.caption.copy(color = PredictaColors.cream35),
            )
        }

        Text(
            text = "\$${item.amount.fmtArs()}",
            style = PredictaTypography.small.copy(
                fontFamily = IBMPlexMono,
                fontSize = 12.sp,
                color = PredictaColors.cream60,
            ),
        )
    }
}

// ── Management row (Gestionar tab) ──
@Composable
private fun ManageRow(
    item: FixedExpense,
    accentColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PredictaDimensions.Spacing.screenPadding, vertical = 4.dp)
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .padding(horizontal = PredictaDimensions.Spacing.base, vertical = PredictaDimensions.Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.md),
    ) {
        // Initial circle
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = PredictaTypography.bodyTight.copy(
                    color = accentColor,
                    fontSize = 14.sp,
                ),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = PredictaTypography.bodyTight.copy(color = PredictaColors.cream),
            )
            Text(
                text = "día ${item.dueDayOfMonth} del mes",
                style = PredictaTypography.caption.copy(color = PredictaColors.cream35),
            )
        }

        Text(
            text = "\$${item.amount.fmtArs()}",
            style = PredictaTypography.small.copy(
                fontFamily = IBMPlexMono,
                fontSize = 12.sp,
                color = PredictaColors.cream60,
            ),
        )

        Spacer(Modifier.width(4.dp))

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
}

// ── Form (bottom sheet) ──
@Composable
private fun FixedExpenseForm(
    initial: FixedExpense?,
    onSave: (FixedExpense) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var dueDay by remember { mutableStateOf(initial?.dueDayOfMonth?.toString() ?: "") }

    val isValid = name.isNotBlank() &&
        amount.toIntOrNull()?.let { it > 0 } == true &&
        dueDay.toIntOrNull()?.let { it in 1..31 } == true

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PredictaColors.amber,
        unfocusedBorderColor = PredictaColors.lineStrong,
        focusedLabelColor = PredictaColors.amber,
        unfocusedLabelColor = PredictaColors.cream35,
        cursorColor = PredictaColors.amber,
        focusedTextColor = PredictaColors.cream,
        unfocusedTextColor = PredictaColors.cream,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PredictaDimensions.Spacing.base)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.md),
    ) {
        Text(
            text = if (initial == null) "Nuevo gasto fijo" else "Editar gasto fijo",
            style = PredictaTypography.cardTitle,
            color = PredictaColors.cream,
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre") },
            placeholder = { Text("Ej: Alquiler, Internet…", color = PredictaColors.cream35) },
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
            placeholder = { Text("150,000", color = PredictaColors.cream35) },
            prefix = { Text("$", color = PredictaColors.cream60) },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
            visualTransformation = ThousandsVisualTransformation,
            singleLine = true,
        )

        OutlinedTextField(
            value = dueDay,
            onValueChange = { dueDay = it.filter { c -> c.isDigit() }.take(2) },
            label = { Text("Día de vencimiento") },
            placeholder = { Text("5", color = PredictaColors.cream35) },
            suffix = { Text("del mes", color = PredictaColors.cream35) },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            singleLine = true,
        )

        Spacer(Modifier.height(PredictaDimensions.Spacing.sm))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
                .background(if (isValid) PredictaColors.amber else PredictaColors.cream12)
                .clickable(enabled = isValid) {
                    onSave(
                        FixedExpense(
                            id = initial?.id ?: 0,
                            name = name.trim(),
                            amount = amount.toInt(),
                            dueDayOfMonth = dueDay.toInt(),
                            paidMonthKey = initial?.paidMonthKey ?: "",
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
}
