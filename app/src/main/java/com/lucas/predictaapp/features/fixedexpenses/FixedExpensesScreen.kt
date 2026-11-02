package com.lucas.predictaapp.features.fixedexpenses

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.model.FixedExpense
import com.lucas.predictaapp.data.model.FixedExpenseStatus
import com.lucas.predictaapp.data.model.computeStatus
import com.lucas.predictaapp.data.repository.FixedExpensesRepository
import com.lucas.predictaapp.data.repository.SyncManager
import com.lucas.predictaapp.ui.components.EmptyState
import com.lucas.predictaapp.ui.components.HeaderIconButton
import com.lucas.predictaapp.ui.components.HeroCard
import com.lucas.predictaapp.ui.components.Kicker
import com.lucas.predictaapp.ui.components.PaidCheck
import com.lucas.predictaapp.ui.components.PredictaCard
import com.lucas.predictaapp.ui.components.PredictaDialog
import com.lucas.predictaapp.ui.components.PredictaPullRefresh
import com.lucas.predictaapp.ui.components.PredictaSheet
import com.lucas.predictaapp.ui.components.PredictaTextField
import com.lucas.predictaapp.ui.components.PrimaryButton
import com.lucas.predictaapp.ui.components.RowDivider
import com.lucas.predictaapp.ui.components.ScreenTopBar
import com.lucas.predictaapp.ui.components.SheetHeader
import com.lucas.predictaapp.ui.components.pageWash
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.ThousandsVisualTransformation
import com.lucas.predictaapp.ui.utils.fmtArs
import kotlinx.coroutines.launch

/**
 * Gastos fijos: checklist del mes (vencidos, por pagar, pagados) y alta/edición en la misma vista.
 * El círculo marca pagado; tocar la fila la edita (y desde ahí se borra).
 */
@Composable
fun FixedExpensesScreen(onBack: () -> Unit) {
    val items by FixedExpensesRepository.fixedExpenses.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()

    var showSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<FixedExpense?>(null) }
    var deletingItem by remember { mutableStateOf<FixedExpense?>(null) }

    val vencidos = items.filter { it.computeStatus() == FixedExpenseStatus.VENCIDO }.sortedBy { it.dueDayOfMonth }
    val pendientes = items.filter { it.computeStatus() == FixedExpenseStatus.PENDIENTE }.sortedBy { it.dueDayOfMonth }
    val pagados = items.filter { it.computeStatus() == FixedExpenseStatus.PAGADO }.sortedBy { it.dueDayOfMonth }
    val totalMonthly = items.sumOf { it.amount }
    val totalPagado = pagados.sumOf { it.amount }

    fun openForm(item: FixedExpense?) {
        editingItem = item
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
                ScreenTopBar(title = "Gastos fijos", onBack = onBack) {
                    HeaderIconButton(Icons.Default.Add, "Agregar gasto fijo", { openForm(null) })
                }
            }

            if (items.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        emoji = "🏠",
                        title = "Sin gastos fijos",
                        body = "Alquiler, expensas, internet: cargalos una vez y cada mes te aviso cuándo vencen.",
                        action = { PrimaryButton("Agregar gasto fijo", { openForm(null) }, leadingIcon = Icons.Default.Add) },
                    )
                }
            } else {
                item(key = "summary") {
                    SummaryCard(paid = pagados.size, count = items.size, paidAmount = totalPagado, total = totalMonthly)
                }
            }

            listOf(
                Triple("Vencidos", vencidos, FixedExpenseStatus.VENCIDO),
                Triple("Por pagar", pendientes, FixedExpenseStatus.PENDIENTE),
                Triple("Pagados", pagados, FixedExpenseStatus.PAGADO),
            ).forEach { (label, group, status) ->
                if (group.isNotEmpty()) {
                    item(key = "g_$label") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.animateItem()) {
                            Kicker(label, modifier = Modifier.padding(start = 4.dp), color = if (status == FixedExpenseStatus.VENCIDO) PredictaColors.danger else PredictaColors.kicker)
                            PredictaCard(modifier = Modifier.fillMaxWidth()) {
                                group.forEachIndexed { i, item ->
                                    ChecklistRow(
                                        item = item,
                                        status = status,
                                        onTogglePaid = { scope.launch { FixedExpensesRepository.togglePaid(item) } },
                                        onEdit = { openForm(item) },
                                        onLongPress = { deletingItem = item },
                                    )
                                    if (i < group.lastIndex) RowDivider(inset = 48.dp)
                                }
                            }
                        }
                    }
                }
            }

            if (items.isNotEmpty()) {
                item(key = "hint") {
                    Text(
                        text = "Tocá el círculo para marcar como pagado y la fila para editarlo.",
                        style = PredictaTypography.rowSubtitle,
                        color = PredictaColors.inkSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }

    if (showSheet) {
        PredictaSheet(onDismiss = { showSheet = false; editingItem = null }, wash = true) {
            FixedExpenseForm(
                initial = editingItem,
                onSave = { item ->
                    scope.launch {
                        FixedExpensesRepository.upsert(item)
                        showSheet = false
                        editingItem = null
                    }
                },
                onDelete = { item ->
                    showSheet = false
                    editingItem = null
                    deletingItem = item
                },
                onClose = { showSheet = false; editingItem = null },
            )
        }
    }

    deletingItem?.let { item ->
        PredictaDialog(
            title = "¿Eliminar gasto fijo?",
            text = "\"${item.name}\" deja de aparecer en tus gastos fijos de todos los meses.",
            confirmLabel = "Eliminar",
            destructive = true,
            onConfirm = {
                scope.launch { FixedExpensesRepository.delete(item.id) }
                deletingItem = null
            },
            onDismiss = { deletingItem = null },
        )
    }
}

@Composable
private fun SummaryCard(paid: Int, count: Int, paidAmount: Int, total: Int) {
    val fraction = if (total > 0) (paidAmount.toFloat() / total).coerceIn(0f, 1f) else 0f
    val left = (total - paidAmount).coerceAtLeast(0)
    HeroCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
        Text(
            text = if (left == 0) "Este mes" else "Te falta pagar",
            style = PredictaTypography.label.copy(fontSize = 13.sp),
            color = Color.White.copy(alpha = 0.85f),
        )
        Text(
            text = if (left == 0) "¡Todo pago!" else "$${left.fmtArs()}",
            style = PredictaTypography.display.copy(fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-1).sp),
            color = Color.White,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.22f)),
        ) {
            Box(Modifier.fillMaxWidth(fraction).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White))
        }
        Text(
            text = "$paid de $count pagados · $${total.fmtArs()} por mes",
            style = PredictaTypography.label.copy(fontSize = 13.sp),
            color = Color.White,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChecklistRow(
    item: FixedExpense,
    status: FixedExpenseStatus,
    onTogglePaid: () -> Unit,
    onEdit: () -> Unit,
    onLongPress: () -> Unit,
) {
    val isPaid = status == FixedExpenseStatus.PAGADO
    val ring = when (status) {
        FixedExpenseStatus.PAGADO -> PredictaColors.success
        FixedExpenseStatus.VENCIDO -> PredictaColors.danger
        FixedExpenseStatus.PENDIENTE -> PredictaColors.warn
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onEdit, onLongClick = onLongPress)
            .padding(vertical = 12.dp),
    ) {
        PaidCheck(
            checked = isPaid,
            ring = ring,
            onClick = onTogglePaid,
            contentDescription = if (isPaid) "Pagado, tocá para desmarcar" else "Marcar ${item.name} como pagado",
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = PredictaTypography.rowTitle.copy(textDecoration = if (isPaid) TextDecoration.LineThrough else TextDecoration.None),
                color = if (isPaid) PredictaColors.inkSecondary else PredictaColors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when (status) {
                    FixedExpenseStatus.VENCIDO -> "Venció el día ${item.dueDayOfMonth}"
                    FixedExpenseStatus.PAGADO -> "Pagado · día ${item.dueDayOfMonth}"
                    FixedExpenseStatus.PENDIENTE -> "Vence el día ${item.dueDayOfMonth}"
                },
                style = PredictaTypography.rowSubtitle,
                color = if (status == FixedExpenseStatus.VENCIDO) PredictaColors.danger else PredictaColors.inkSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$${item.amount.fmtArs()}",
            style = PredictaTypography.rowAmount,
            color = if (isPaid) PredictaColors.inkSecondary else PredictaColors.ink,
        )
    }
}

@Composable
private fun FixedExpenseForm(
    initial: FixedExpense?,
    onSave: (FixedExpense) -> Unit,
    onDelete: (FixedExpense) -> Unit,
    onClose: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var dueDay by remember { mutableStateOf(initial?.dueDayOfMonth?.toString() ?: "") }

    val isValid = name.isNotBlank() &&
        amount.toIntOrNull()?.let { it > 0 } == true &&
        dueDay.toIntOrNull()?.let { it in 1..31 } == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SheetHeader(title = if (initial == null) "Nuevo gasto fijo" else "Editar gasto fijo", onClose = onClose)

        PredictaTextField(
            value = name,
            onValueChange = { name = it },
            label = "Nombre",
            placeholder = "Alquiler, internet…",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PredictaTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() }.take(10) },
                label = "Monto mensual",
                placeholder = "150.000",
                prefix = "$",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                visualTransformation = ThousandsVisualTransformation,
                modifier = Modifier.weight(1.6f),
            )
            PredictaTextField(
                value = dueDay,
                onValueChange = { dueDay = it.filter { c -> c.isDigit() }.take(2) },
                label = "Vence el día",
                placeholder = "5",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f),
            )
        }

        PrimaryButton(
            text = "Guardar",
            enabled = isValid,
            onClick = {
                val base = initial ?: FixedExpense(name = "", amount = 0, dueDayOfMonth = 1)
                onSave(base.copy(name = name.trim(), amount = amount.toInt(), dueDayOfMonth = dueDay.toInt()))
            },
            modifier = Modifier.padding(top = 4.dp),
        )
        if (initial != null) {
            TextButton(onClick = { onDelete(initial) }, modifier = Modifier.fillMaxWidth()) {
                Text("Eliminar gasto fijo", style = PredictaTypography.label, color = PredictaColors.danger)
            }
        }
    }
}
