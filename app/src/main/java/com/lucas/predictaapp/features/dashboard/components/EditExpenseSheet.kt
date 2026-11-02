package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.data.model.Category
import com.lucas.predictaapp.data.model.CategoryType
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.ui.components.ChoiceChip
import com.lucas.predictaapp.ui.components.Kicker
import com.lucas.predictaapp.ui.components.PickerRow
import com.lucas.predictaapp.ui.components.PredictaSheet
import com.lucas.predictaapp.ui.components.PredictaTextField
import com.lucas.predictaapp.ui.components.PrimaryButton
import com.lucas.predictaapp.ui.components.SegmentedToggle
import com.lucas.predictaapp.ui.components.SheetHeader
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.ThousandsVisualTransformation
import com.lucas.predictaapp.ui.utils.relativeDateLabel
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun localDateToUtcMillis(millis: Long): Long {
    val ld = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return ld.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun utcMillisToLocalNoon(utcMillis: Long): Long {
    val ld = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return ld.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private val fullDate = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", Locale("es", "AR"))

private fun fmtFullDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(fullDate)

/**
 * Alta y edición de un movimiento. El monto va grande arriba (es lo único obligatorio de verdad
 * en una carga rápida); debajo, gasto/ingreso, dónde, categoría y fecha. [categories] trae las de
 * los dos tipos: el selector de tipo solo aparece si hay de ambos.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditExpenseSheet(
    expense: Expense,
    categories: List<Category>,
    onSave: (Expense) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Editar movimiento",
) {
    // El sheet solo se arrastra (y cierra) desde arriba: manija y título. En el cuerpo, el
    // sobrante del scroll no se le pasa al sheet — si no, un swipe corto sobre las categorías
    // con el formulario arriba de todo lo cerraba y se perdía la edición.
    val blockSheetDrag = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = available
            override suspend fun onPostFling(consumed: Velocity, available: Velocity) = available
        }
    }

    val isNew = expense.id == 0L
    var merchant by remember { mutableStateOf(expense.merchant) }
    // Alta nueva (amount 0): campo vacío en vez de un "0" que hay que borrar.
    var amount by remember { mutableStateOf(if (expense.amount > 0) expense.amount.toString() else "") }
    var categoryId by remember { mutableStateOf(expense.categoryId) }
    var dateMillis by remember { mutableStateOf(expense.dateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val initialType = categories.firstOrNull { it.id == expense.categoryId }?.type ?: CategoryType.EXPENSE
    var type by remember { mutableStateOf(initialType) }
    val hasBothTypes = categories.any { it.type == CategoryType.INCOME } && categories.any { it.type == CategoryType.EXPENSE }
    val visibleCategories = categories.filter { it.type == type }

    val focusManager = LocalFocusManager.current
    val amountFocus = remember { FocusRequester() }
    // En un alta el teclado numérico aparece solo: lo primero que se carga es el monto.
    LaunchedEffect(Unit) { if (isNew) amountFocus.requestFocus() }

    val isValid = merchant.isNotBlank() && amount.toIntOrNull()?.let { it > 0 } == true &&
        visibleCategories.any { it.id == categoryId }
    val noun = if (type == CategoryType.INCOME) "ingreso" else "gasto"

    PredictaSheet(onDismiss = onDismiss, wash = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding(),
        ) {
            // Fuera del scroll: es la zona de agarre para cerrar arrastrando.
            SheetHeader(
                title = if (isNew) "Nuevo $noun" else title,
                onClose = onDismiss,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
            )

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
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    ) {
                        AmountInput(
                            value = amount,
                            onValueChange = { amount = it.filter { c -> c.isDigit() }.take(10) },
                            modifier = Modifier.focusRequester(amountFocus),
                        )
                        if (hasBothTypes) {
                            SegmentedToggle(
                                options = listOf("Gasto", "Ingreso"),
                                selectedIndex = if (type == CategoryType.INCOME) 1 else 0,
                                onSelect = { type = if (it == 1) CategoryType.INCOME else CategoryType.EXPENSE },
                            )
                        }
                    }

                    PredictaTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = if (type == CategoryType.INCOME) "De dónde" else "Dónde",
                        placeholder = if (type == CategoryType.INCOME) "Sueldo, un cliente…" else "Comercio o descripción",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Kicker("Categoría")
                        // Todas a la vista (grilla que envuelve) en vez de un carrusel horizontal.
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            visibleCategories.forEach { cat ->
                                ChoiceChip(
                                    text = cat.name,
                                    leading = cat.emoji,
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

                    PickerRow(
                        icon = Icons.Outlined.CalendarMonth,
                        title = relativeDateLabel(dateMillis).replaceFirstChar { it.uppercase() },
                        subtitle = fmtFullDate(dateMillis),
                        onClick = { showDatePicker = true },
                        trailing = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    )
                }

                PrimaryButton(
                    text = if (isNew) "Guardar $noun" else "Guardar cambios",
                    enabled = isValid,
                    onClick = {
                        onSave(
                            expense.copy(
                                merchant = merchant.trim(),
                                amount = amount.toInt(),
                                categoryId = categoryId,
                                dateMillis = dateMillis,
                            ),
                        )
                    },
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                )
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
                state = pickerState,
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

/** El monto grande y centrado, con el cursor de marca. Vacío muestra "$0" en gris. */
@Composable
private fun AmountInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val style = PredictaTypography.display.copy(
        fontSize = 52.sp,
        lineHeight = 58.sp,
        letterSpacing = (-2).sp,
        textAlign = TextAlign.Center,
        color = PredictaColors.ink,
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(PredictaColors.accent),
        visualTransformation = PesoTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) Text("$0", style = style, color = PredictaColors.inkTertiary)
                inner()
            }
        },
    )
}

/** "12500" → "$12.500": separador de miles más el signo adelante. */
private object PesoTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val thousands = ThousandsVisualTransformation.filter(text)
        return TransformedText(
            AnnotatedString("$" + thousands.text.text),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = thousands.offsetMapping.originalToTransformed(offset) + 1
                override fun transformedToOriginal(offset: Int) = thousands.offsetMapping.transformedToOriginal((offset - 1).coerceAtLeast(0))
            },
        )
    }
}
