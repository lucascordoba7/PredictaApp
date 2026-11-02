package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.data.model.Category
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.ThousandsVisualTransformation
import com.lucas.predictaapp.ui.utils.relativeDateLabel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

private fun localDateToUtcMillis(millis: Long): Long {
    val ld = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return ld.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun utcMillisToLocalNoon(utcMillis: Long): Long {
    val ld = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return ld.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun fmtFullDate(millis: Long): String =
    SimpleDateFormat("d 'de' MMMM, yyyy", Locale("es", "AR")).format(millis)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditExpenseSheet(
    expense: Expense,
    categories: List<Category>,
    onSave: (Expense) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Editar transacción",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // El sheet solo se arrastra (y cierra) desde arriba: handle + título. En el cuerpo, el
    // sobrante del scroll no se le pasa al sheet — si no, un swipe corto sobre las categorías
    // con el formulario arriba de todo lo cerraba y se perdía la edición.
    val blockSheetDrag = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = available
            override suspend fun onPostFling(consumed: Velocity, available: Velocity) = available
        }
    }

    var merchant by remember { mutableStateOf(expense.merchant) }
    // Alta nueva (amount 0): campo vacío en vez de un "0" que hay que borrar.
    var amount by remember { mutableStateOf(if (expense.amount > 0) expense.amount.toString() else "") }
    var categoryId by remember { mutableStateOf(expense.categoryId) }
    var dateMillis by remember { mutableStateOf(expense.dateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val isValid = merchant.isNotBlank() && amount.toIntOrNull()?.let { it > 0 } == true && categoryId > 0L

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PredictaColors.amber,
        unfocusedBorderColor = PredictaColors.lineStrong,
        focusedLabelColor = PredictaColors.amber,
        unfocusedLabelColor = PredictaColors.cream35,
        cursorColor = PredictaColors.amber,
        focusedTextColor = PredictaColors.cream,
        unfocusedTextColor = PredictaColors.cream,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PredictaColors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            // Fuera del scroll: es la zona de agarre para cerrar arrastrando.
            Text(
                text = title,
                style = PredictaTypography.cardTitle,
                color = PredictaColors.cream,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PredictaDimensions.Spacing.base)
                    .padding(top = PredictaDimensions.Spacing.sm, bottom = PredictaDimensions.Spacing.md),
            )

            // Cuerpo: campos scrolleables + acciones fijas ("Guardar" siempre a la vista).
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .nestedScroll(blockSheetDrag)
                    // Arrastres sobre zonas sin scroll (botones) tampoco mueven el sheet.
                    .pointerInput(Unit) { detectVerticalDragGestures { change, _ -> change.consume() } },
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = PredictaDimensions.Spacing.base),
                    verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.md),
                ) {
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text("Nombre") },
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
                        label = { Text("Monto") },
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

                    // Fecha
                    FieldLabel("FECHA")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
                            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(PredictaDimensions.Radius.card))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = PredictaDimensions.Spacing.base, vertical = 14.dp),
                    ) {
                        Text(
                            text = fmtFullDate(dateMillis),
                            style = PredictaTypography.body.copy(color = PredictaColors.cream),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = relativeDateLabel(dateMillis),
                            style = PredictaTypography.monoCap.copy(fontSize = 10.5.sp, color = PredictaColors.amber),
                        )
                    }

                    // Categoría: todas a la vista (grilla que envuelve) en vez de un carrusel
                    // horizontal donde había que buscar deslizando.
                    FieldLabel("CATEGORÍA")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { cat ->
                            CategoryChip(
                                category = cat,
                                selected = cat.id == categoryId,
                                onClick = {
                                    categoryId = cat.id
                                    // Cierra el teclado para que la grilla no quede tapada.
                                    focusManager.clearFocus()
                                },
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PredictaDimensions.Spacing.base)
                        .padding(top = PredictaDimensions.Spacing.md, bottom = PredictaDimensions.Spacing.sm),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
                            .background(if (isValid) PredictaColors.amber else PredictaColors.cream12)
                            .clickable(enabled = isValid) {
                                onSave(
                                    expense.copy(
                                        merchant = merchant.trim(),
                                        amount = amount.toInt(),
                                        categoryId = categoryId,
                                        dateMillis = dateMillis,
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

                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancelar", style = PredictaTypography.small, color = PredictaColors.cream60)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDateToUtcMillis(dateMillis),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateMillis = utcMillisToLocalNoon(it) }
                    showDatePicker = false
                }) { Text("OK", color = PredictaColors.amber) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = PredictaColors.cream60)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = PredictaColors.surface),
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = PredictaColors.surface,
                    selectedDayContainerColor = PredictaColors.amber,
                    selectedDayContentColor = PredictaColors.charcoal,
                    todayDateBorderColor = PredictaColors.amber,
                ),
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = PredictaTypography.monoCap.copy(fontSize = 10.sp, letterSpacing = 1.sp),
        color = PredictaColors.cream35,
    )
}

@Composable
private fun CategoryChip(category: Category, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(PredictaDimensions.Radius.pill)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) PredictaColors.amber else PredictaColors.surfaceHigh)
            .border(1.dp, if (selected) PredictaColors.amber else PredictaColors.lineStrong, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = category.emoji, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = category.name,
            maxLines = 1,
            style = PredictaTypography.small.copy(
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = if (selected) PredictaColors.charcoal else PredictaColors.cream60,
            ),
        )
    }
}
