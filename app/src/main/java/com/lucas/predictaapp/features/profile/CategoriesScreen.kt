package com.lucas.predictaapp.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.model.Category
import com.lucas.predictaapp.data.model.CategoryType
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import kotlinx.coroutines.launch

private val EMOJI_GRID = listOf(
    "🛒", "🚌", "🏠", "🍻", "🔁", "💊", "💼", "⛽",
    "🎁", "📚", "👕", "✈️", "🍔", "☕", "🎬", "🎮",
    "🐾", "💄", "🧘", "🧾", "💡", "🚿", "🎵", "📱",
)

private val COLOR_PALETTE = listOf(
    "#E8A83E", "#6A92E8", "#6FC58B", "#E66B57",
    "#9D7AE8", "#E87AB6", "#6FD3C5", "#E8D14A",
    "#8AA8E8", "#C58F6F", "#9DE8A0", "#E07A7A",
)

private data class SuggestedCategory(val name: String, val emoji: String, val color: String, val why: String)

private val SUGGESTED = listOf(
    SuggestedCategory("Café",      "☕", "#C58F6F", "detectamos tx en Starbucks/Havanna este mes"),
    SuggestedCategory("Streaming", "🎬", "#9D7AE8", "3 servicios sin categorizar"),
    SuggestedCategory("Mascotas",  "🐾", "#6FD3C5", "compras recurrentes en pet shop"),
)

private fun hexToColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrElse { PredictaColors.amber }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val allCategories by CategoryRepository.categories.collectAsStateWithLifecycle(emptyList())
    val expenseCategories = allCategories.filter { it.type == CategoryType.EXPENSE }
    val incomeCategories  = allCategories.filter { it.type == CategoryType.INCOME }

    val scope = rememberCoroutineScope()
    var editTarget by remember { mutableStateOf<Category?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var createPrefill by remember { mutableStateOf<SuggestedCategory?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize().background(PredictaColors.charcoal)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 0.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = PredictaColors.cream,
                    )
                }
                Text(
                    text = "Categorías",
                    style = PredictaTypography.cardTitle.copy(
                        fontSize = 16.sp,
                        color = PredictaColors.cream,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PredictaColors.amberSoft)
                        .border(1.dp, PredictaColors.amberEdge, CircleShape)
                        .clickable { createPrefill = null; showCreate = true },
                ) {
                    Text(text = "+", style = PredictaTypography.section.copy(color = PredictaColors.amber, fontSize = 18.sp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PredictaDimensions.Spacing.screenPadding),
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${allCategories.size} categorías · tocá para editar",
                    style = PredictaTypography.caption.copy(color = PredictaColors.cream35),
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                CategorySection(title = "GASTOS", categories = expenseCategories, onTap = { editTarget = it })
                Spacer(modifier = Modifier.height(24.dp))
                CategorySection(title = "INGRESOS", categories = incomeCategories, onTap = { editTarget = it })

                val existingNamesSet = remember(allCategories) {
                    allCategories.map { it.name.trim().lowercase() }.toSet()
                }
                val pendingSuggestions = remember(existingNamesSet) {
                    SUGGESTED.filter { it.name.trim().lowercase() !in existingNamesSet }
                }
                if (pendingSuggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    SuggestedSection(
                        suggestions = pendingSuggestions,
                        onAdd = { s -> createPrefill = s; showCreate = true },
                    )
                }
                Spacer(modifier = Modifier.height(96.dp)) // space for FAB
            }
        }

        // Sticky FAB at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, PredictaColors.charcoal.copy(alpha = 0.95f))
                    )
                )
                .padding(horizontal = PredictaDimensions.Spacing.screenPadding, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(PredictaColors.amber)
                    .clickable { createPrefill = null; showCreate = true },
            ) {
                Text(
                    text = "+ Crear categoría",
                    style = PredictaTypography.bodyTight.copy(
                        color = PredictaColors.onAmber,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                    ),
                )
            }
        }
    }

    // Edit sheet
    editTarget?.let { cat ->
        ModalBottomSheet(
            onDismissRequest = { editTarget = null },
            sheetState = sheetState,
            containerColor = PredictaColors.surface,
            tonalElevation = 0.dp,
        ) {
            CategoryEditorSheet(
                category = cat,
                onSave = { name, emoji, color, type ->
                    scope.launch { CategoryRepository.update(cat, name, emoji, color, type) }
                    editTarget = null
                },
                onDelete = {
                    scope.launch { CategoryRepository.delete(cat.id) }
                    editTarget = null
                },
                onClose = { editTarget = null },
            )
        }
    }

    // Create sheet
    if (showCreate) {
        val existingNames = remember(allCategories) { allCategories.map { it.name.trim().lowercase() }.toSet() }
        ModalBottomSheet(
            onDismissRequest = { showCreate = false },
            sheetState = sheetState,
            containerColor = PredictaColors.surface,
            tonalElevation = 0.dp,
        ) {
            CategoryCreatorSheet(
                prefill = createPrefill,
                existingNames = existingNames,
                onSave = { name, emoji, color, type ->
                    scope.launch { CategoryRepository.add(name, emoji, color, type) }
                    showCreate = false
                    createPrefill = null
                },
                onClose = { showCreate = false; createPrefill = null },
            )
        }
    }
}

@Composable
private fun CategorySection(
    title: String,
    categories: List<Category>,
    onTap: (Category) -> Unit,
) {
    Text(
        text = title,
        style = PredictaTypography.monoCap.copy(
            color = PredictaColors.cream35,
            letterSpacing = 1.8.sp,
            fontSize = 10.sp,
        ),
        modifier = Modifier.padding(bottom = 10.dp),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .border(1.dp, PredictaColors.line, RoundedCornerShape(PredictaDimensions.Radius.card)),
    ) {
        if (categories.isEmpty()) {
            Text(
                text = "Sin categorías",
                style = PredictaTypography.small.copy(color = PredictaColors.cream35),
                modifier = Modifier.padding(16.dp),
            )
        } else {
            categories.forEachIndexed { index, category ->
                CategoryListRow(
                    category = category,
                    onClick = { if (category.isCustom) onTap(category) },
                )
                if (index < categories.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 56.dp)
                            .height(1.dp)
                            .background(PredictaColors.line),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryListRow(category: Category, onClick: () -> Unit) {
    val catColor = hexToColor(category.color)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = category.isCustom, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        // Colored bubble (rounded square)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(catColor.copy(alpha = 0.13f))
                .border(1.dp, catColor.copy(alpha = 0.33f), RoundedCornerShape(10.dp)),
        ) {
            Text(text = category.emoji, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name + meta
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = category.name,
                    style = PredictaTypography.body.copy(color = PredictaColors.cream),
                )
                if (!category.isCustom) {
                    // lock icon for system categories
                    Box(
                        modifier = Modifier
                            .size(14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LockIcon(tint = PredictaColors.cream35)
                    }
                }
                if (category.isCustom) {
                    Text(
                        text = "CUSTOM",
                        style = PredictaTypography.monoCap.copy(
                            color = PredictaColors.amber,
                            fontSize = 9.sp,
                            letterSpacing = 1.2.sp,
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PredictaColors.amberSoft)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
        }

        // Color dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(catColor),
        )
    }
}

@Composable
private fun LockIcon(tint: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(10.dp)) {
        val w = size.width
        val h = size.height
        val paint = androidx.compose.ui.graphics.Paint().apply {
            color = tint
            strokeWidth = 1.2.dp.toPx()
            style = androidx.compose.ui.graphics.PaintingStyle.Stroke
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            strokeJoin = androidx.compose.ui.graphics.StrokeJoin.Round
        }
        // body
        drawRoundRect(
            color = tint,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.1f, h * 0.45f),
            size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx()),
        )
        // shackle
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.05f),
            size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.5f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx()),
        )
    }
}

@Composable
private fun SuggestedSection(
    suggestions: List<SuggestedCategory>,
    onAdd: (SuggestedCategory) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp),
    ) {
        Text(
            text = "SUGERIDAS POR PREDICTA",
            style = PredictaTypography.monoCap.copy(
                color = PredictaColors.cream35,
                letterSpacing = 1.8.sp,
                fontSize = 10.sp,
            ),
            modifier = Modifier.weight(1f),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.forEach { s ->
            SuggestedRow(suggestion = s, onAdd = { onAdd(s) })
        }
    }
}

@Composable
private fun SuggestedRow(suggestion: SuggestedCategory, onAdd: () -> Unit) {
    val catColor = hexToColor(suggestion.color)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.cream12.copy(alpha = 0.025f))
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(PredictaDimensions.Radius.card),)
            .padding(10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(catColor.copy(alpha = 0.13f))
                .border(1.dp, catColor.copy(alpha = 0.33f), CircleShape),
        ) {
            Text(text = suggestion.emoji, fontSize = 15.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(suggestion.name, style = PredictaTypography.body.copy(color = PredictaColors.cream))
            Text(suggestion.why, style = PredictaTypography.caption.copy(color = PredictaColors.cream35))
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(9999.dp))
                .border(1.dp, PredictaColors.amber, RoundedCornerShape(9999.dp))
                .clickable(onClick = onAdd)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "+ Usar",
                style = PredictaTypography.small.copy(
                    color = PredictaColors.amber,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                ),
            )
        }
    }
}

// ─── Editor sheet (editar categoría existente) ─────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorSheet(
    category: Category,
    onSave: (name: String, emoji: String, color: String, type: CategoryType) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var name by remember { mutableStateOf(category.name) }
    var emoji by remember { mutableStateOf(category.emoji) }
    var color by remember { mutableStateOf(category.color) }
    var selectedType by remember { mutableStateOf(category.type) }
    val isValid = name.trim().isNotEmpty()

    EditorSheetContent(
        title = "Editar categoría",
        subtitle = if (category.isCustom) null else "Categoría del sistema",
        name = name,
        onNameChange = { name = it },
        emoji = emoji,
        onEmojiChange = { emoji = it },
        color = color,
        onColorChange = { color = it },
        selectedType = selectedType,
        onTypeChange = { selectedType = it },
        isValid = isValid,
        isEditing = true,
        isCustom = category.isCustom,
        confirmLabel = "Guardar cambios",
        onConfirm = { onSave(name.trim(), emoji, color, selectedType) },
        onDelete = onDelete,
        onClose = onClose,
    )
}

// ─── Creator sheet (nueva categoría) ───────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryCreatorSheet(
    prefill: SuggestedCategory?,
    existingNames: Set<String>,
    onSave: (name: String, emoji: String, color: String, type: CategoryType) -> Unit,
    onClose: () -> Unit,
) {
    var name by remember { mutableStateOf(prefill?.name ?: "") }
    var emoji by remember { mutableStateOf(prefill?.emoji ?: EMOJI_GRID.first()) }
    var color by remember { mutableStateOf(prefill?.color ?: COLOR_PALETTE.first()) }
    var selectedType by remember { mutableStateOf(CategoryType.EXPENSE) }
    val isDuplicate = name.trim().isNotEmpty() && name.trim().lowercase() in existingNames
    val isValid = name.trim().isNotEmpty() && !isDuplicate

    EditorSheetContent(
        title = "Nueva categoría",
        subtitle = null,
        name = name,
        onNameChange = { name = it },
        emoji = emoji,
        onEmojiChange = { emoji = it },
        color = color,
        onColorChange = { color = it },
        selectedType = selectedType,
        onTypeChange = { selectedType = it },
        isValid = isValid,
        isDuplicate = isDuplicate,
        isEditing = false,
        isCustom = true,
        confirmLabel = "Crear categoría",
        onConfirm = { onSave(name.trim(), emoji, color, selectedType) },
        onDelete = null,
        onClose = onClose,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorSheetContent(
    title: String,
    subtitle: String?,
    name: String,
    onNameChange: (String) -> Unit,
    emoji: String,
    onEmojiChange: (String) -> Unit,
    color: String,
    onColorChange: (String) -> Unit,
    selectedType: CategoryType,
    onTypeChange: (CategoryType) -> Unit,
    isValid: Boolean,
    isDuplicate: Boolean = false,
    isEditing: Boolean,
    isCustom: Boolean,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit,
) {
    val catColor = hexToColor(color)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PredictaDimensions.Spacing.screenPadding)
            .padding(bottom = 16.dp),
    ) {
        // Preview chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(9999.dp))
                .background(catColor.copy(alpha = 0.1f))
                .border(1.dp, catColor.copy(alpha = 0.33f), RoundedCornerShape(9999.dp))
                .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.2f)),
            ) {
                Text(text = emoji, fontSize = 15.sp)
            }
            Text(
                text = if (name.isBlank()) "nombre…" else name,
                style = PredictaTypography.bodyTight.copy(
                    color = if (name.isBlank()) PredictaColors.cream35 else PredictaColors.cream,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                ),
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Name
        FieldLabel("NOMBRE")
        var focused by remember { mutableStateOf(false) }
        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            textStyle = PredictaTypography.body.copy(color = PredictaColors.cream),
            cursorBrush = SolidColor(PredictaColors.amber),
            singleLine = true,
            enabled = isCustom,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(PredictaDimensions.Radius.md))
                .background(PredictaColors.charcoal)
                .border(
                    1.dp,
                    if (focused) PredictaColors.amber else PredictaColors.lineStrong,
                    RoundedCornerShape(PredictaDimensions.Radius.md),
                )
                .padding(horizontal = 14.dp)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                    if (name.isEmpty()) {
                        Text("ej. Mascotas", style = PredictaTypography.body.copy(color = PredictaColors.cream35))
                    }
                    inner()
                }
            },
        )
        if (isDuplicate) {
            Text(
                text = "Ya existe una categoría con ese nombre",
                style = PredictaTypography.caption.copy(color = PredictaColors.coral),
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Emoji grid (8 columns)
        FieldLabel("ÍCONO")
        val cols = 8
        val rows = (EMOJI_GRID.size + cols - 1) / cols
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(rows) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(cols) { col ->
                        val idx = row * cols + col
                        val e = EMOJI_GRID.getOrNull(idx)
                        if (e != null) {
                            val selected = e == emoji
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(PredictaDimensions.Radius.sm))
                                    .background(if (selected) catColor.copy(alpha = 0.2f) else PredictaColors.charcoal)
                                    .border(
                                        1.dp,
                                        if (selected) catColor.copy(alpha = 0.8f) else PredictaColors.line,
                                        RoundedCornerShape(PredictaDimensions.Radius.sm),
                                    )
                                    .clickable { onEmojiChange(e) },
                            ) {
                                Text(text = e, fontSize = 18.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Color swatches
        FieldLabel("COLOR")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            COLOR_PALETTE.forEach { hex ->
                val selected = hex == color
                val swatchColor = hexToColor(hex)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) PredictaColors.cream else PredictaColors.cream12,
                            shape = CircleShape,
                        )
                        .clickable { onColorChange(hex) },
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Type toggle
        FieldLabel("TIPO")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PredictaDimensions.Radius.md))
                .background(PredictaColors.charcoal),
        ) {
            TypeTab(
                label = "Gasto",
                selected = selectedType == CategoryType.EXPENSE,
                onClick = { onTypeChange(CategoryType.EXPENSE) },
                modifier = Modifier.weight(1f),
            )
            TypeTab(
                label = "Ingreso",
                selected = selectedType == CategoryType.INCOME,
                onClick = { onTypeChange(CategoryType.INCOME) },
                modifier = Modifier.weight(1f),
            )
        }

        // Delete (only for custom non-system categories in edit mode)
        if (isEditing && isCustom && onDelete != null) {
            Spacer(modifier = Modifier.height(18.dp))
            FieldLabel("ACCIONES")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(PredictaDimensions.Radius.md))
                    .border(1.dp, PredictaColors.coral.copy(alpha = 0.27f), RoundedCornerShape(PredictaDimensions.Radius.md))
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Column {
                    Text("Eliminar categoría", style = PredictaTypography.body.copy(color = PredictaColors.coral))
                    Text(
                        "Los gastos existentes se mueven a \"Otros\"",
                        style = PredictaTypography.caption.copy(color = PredictaColors.cream35),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(9999.dp))
                    .clickable(onClick = onClose),
            ) {
                Text("Cancelar", style = PredictaTypography.bodyTight.copy(color = PredictaColors.cream))
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(2f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(if (isValid) PredictaColors.amber else PredictaColors.surfaceHigh)
                    .clickable(enabled = isValid, onClick = onConfirm),
            ) {
                Text(
                    confirmLabel,
                    style = PredictaTypography.bodyTight.copy(
                        color = if (isValid) PredictaColors.onAmber else PredictaColors.cream35,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = PredictaTypography.monoCap.copy(
            color = PredictaColors.cream35,
            letterSpacing = 1.4.sp,
        ),
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun TypeTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(PredictaDimensions.Radius.md))
            .background(if (selected) PredictaColors.amberSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = PredictaTypography.small.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) PredictaColors.amber else PredictaColors.cream60,
            ),
        )
    }
}
