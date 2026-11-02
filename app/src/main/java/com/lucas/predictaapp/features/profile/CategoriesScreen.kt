package com.lucas.predictaapp.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.model.Category
import com.lucas.predictaapp.data.model.CategoryType
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.ui.components.GlyphBadge
import com.lucas.predictaapp.ui.components.HeaderIconButton
import com.lucas.predictaapp.ui.components.Kicker
import com.lucas.predictaapp.ui.components.PredictaCard
import com.lucas.predictaapp.ui.components.PredictaDialog
import com.lucas.predictaapp.ui.components.PredictaSheet
import com.lucas.predictaapp.ui.components.PredictaTextField
import com.lucas.predictaapp.ui.components.PrimaryButton
import com.lucas.predictaapp.ui.components.RowDivider
import com.lucas.predictaapp.ui.components.ScreenTopBar
import com.lucas.predictaapp.ui.components.SegmentedToggle
import com.lucas.predictaapp.ui.components.SheetHeader
import com.lucas.predictaapp.ui.components.StatusPill
import com.lucas.predictaapp.ui.components.pageWash
import com.lucas.predictaapp.ui.theme.PredictaColors
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

/** Ideas de categorías que mucha gente separa. Fijas: no salen de analizar tus gastos. */
private data class SuggestedCategory(val name: String, val emoji: String, val color: String, val why: String)

private val SUGGESTED = listOf(
    SuggestedCategory("Café", "☕", "#C58F6F", "Para ver cuánto se va en cafecitos"),
    SuggestedCategory("Streaming", "🎬", "#9D7AE8", "Netflix, Disney+ y compañía juntos"),
    SuggestedCategory("Mascotas", "🐾", "#6FD3C5", "Comida, veterinaria y pet shop"),
)

/** Categorías que no se pueden borrar: a "Otros" van los gastos de las que se borran. */
private const val FALLBACK_CATEGORY = "Otros"

private fun hexToColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrElse { PredictaColors.accent }

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val allCategories by CategoryRepository.categories.collectAsStateWithLifecycle(emptyList())
    val expenseCategories = allCategories.filter { it.type == CategoryType.EXPENSE }
    val incomeCategories = allCategories.filter { it.type == CategoryType.INCOME }

    // Cada vez que se entra a la pantalla, rehidratar desde Supabase (la fuente compartida).
    LaunchedEffect(Unit) { CategoryRepository.pullFromRemote() }

    val scope = rememberCoroutineScope()
    var editTarget by remember { mutableStateOf<Category?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var createPrefill by remember { mutableStateOf<SuggestedCategory?>(null) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    val existingNames = remember(allCategories) { allCategories.map { it.name.trim().lowercase() }.toSet() }
    val pendingSuggestions = remember(existingNames) { SUGGESTED.filter { it.name.trim().lowercase() !in existingNames } }

    Column(
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier
            .fillMaxSize()
            .pageWash()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 32.dp),
    ) {
        ScreenTopBar(title = "Categorías", onBack = onBack) {
            HeaderIconButton(Icons.Default.Add, "Crear categoría", { createPrefill = null; showCreate = true })
        }

        CategorySection("Gastos", expenseCategories, onTap = { editTarget = it })
        CategorySection("Ingresos", incomeCategories, onTap = { editTarget = it })

        if (pendingSuggestions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Kicker("Ideas", modifier = Modifier.padding(start = 4.dp))
                PredictaCard(modifier = Modifier.fillMaxWidth()) {
                    pendingSuggestions.forEachIndexed { i, s ->
                        SuggestedRow(s, onAdd = { createPrefill = s; showCreate = true })
                        if (i < pendingSuggestions.lastIndex) RowDivider(inset = 54.dp)
                    }
                }
            }
        }
    }

    editTarget?.let { cat ->
        PredictaSheet(onDismiss = { editTarget = null }, wash = true) {
            CategoryEditor(
                title = "Editar categoría",
                initialName = cat.name,
                initialEmoji = cat.emoji,
                initialColor = cat.color,
                initialType = cat.type,
                nameEditable = cat.isCustom,
                existingNames = existingNames - cat.name.trim().lowercase(),
                confirmLabel = "Guardar cambios",
                onConfirm = { name, emoji, color, type ->
                    scope.launch { CategoryRepository.update(cat, name, emoji, color, type) }
                    editTarget = null
                },
                onDelete = if (cat.isCustom && cat.name != FALLBACK_CATEGORY) {
                    { editTarget = null; deleting = cat }
                } else null,
                onClose = { editTarget = null },
            )
        }
    }

    if (showCreate) {
        PredictaSheet(onDismiss = { showCreate = false; createPrefill = null }, wash = true) {
            CategoryEditor(
                title = "Nueva categoría",
                initialName = createPrefill?.name ?: "",
                initialEmoji = createPrefill?.emoji ?: EMOJI_GRID.first(),
                initialColor = createPrefill?.color ?: COLOR_PALETTE.first(),
                initialType = CategoryType.EXPENSE,
                nameEditable = true,
                existingNames = existingNames,
                confirmLabel = "Crear categoría",
                onConfirm = { name, emoji, color, type ->
                    scope.launch { CategoryRepository.add(name, emoji, color, type) }
                    showCreate = false
                    createPrefill = null
                },
                onDelete = null,
                onClose = { showCreate = false; createPrefill = null },
            )
        }
    }

    deleting?.let { cat ->
        PredictaDialog(
            title = "¿Eliminar ${cat.name}?",
            text = "Sus movimientos pasan a \"$FALLBACK_CATEGORY\". No se pierde ningún gasto.",
            confirmLabel = "Eliminar",
            destructive = true,
            onConfirm = {
                scope.launch { CategoryRepository.delete(cat.id) }
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun CategorySection(title: String, categories: List<Category>, onTap: (Category) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Kicker("$title · ${categories.size}", modifier = Modifier.padding(start = 4.dp))
        PredictaCard(modifier = Modifier.fillMaxWidth()) {
            if (categories.isEmpty()) {
                Text(
                    "Sin categorías todavía",
                    style = PredictaTypography.body,
                    color = PredictaColors.inkSecondary,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            categories.forEachIndexed { index, category ->
                CategoryRow(category, onClick = { onTap(category) })
                if (index < categories.lastIndex) RowDivider(inset = 54.dp)
            }
        }
    }
}

@Composable
private fun CategoryBadge(emoji: String, color: String, size: androidx.compose.ui.unit.Dp = 42.dp) {
    GlyphBadge(size = size, container = hexToColor(color).copy(alpha = if (PredictaColors.isDark) 0.22f else 0.16f)) {
        Text(emoji, fontSize = (size.value * 0.43f).sp)
    }
}

@Composable
private fun CategoryRow(category: Category, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        CategoryBadge(category.emoji, category.color)
        Spacer(Modifier.width(12.dp))
        Text(category.name, style = PredictaTypography.rowTitle, color = PredictaColors.ink, modifier = Modifier.weight(1f))
        if (category.isCustom) {
            StatusPill("Tuya", PredictaColors.accentSoft, PredictaColors.accent)
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = PredictaColors.inkTertiary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SuggestedRow(suggestion: SuggestedCategory, onAdd: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onAdd)
            .padding(vertical = 10.dp),
    ) {
        CategoryBadge(suggestion.emoji, suggestion.color)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(suggestion.name, style = PredictaTypography.rowTitle, color = PredictaColors.ink)
            Text(suggestion.why, style = PredictaTypography.rowSubtitle, color = PredictaColors.inkSecondary)
        }
        Spacer(Modifier.width(8.dp))
        StatusPill("+ Usar", PredictaColors.accentSoft, PredictaColors.accent)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditor(
    title: String,
    initialName: String,
    initialEmoji: String,
    initialColor: String,
    initialType: CategoryType,
    nameEditable: Boolean,
    existingNames: Set<String>,
    confirmLabel: String,
    onConfirm: (name: String, emoji: String, color: String, type: CategoryType) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    var color by remember { mutableStateOf(initialColor) }
    var type by remember { mutableStateOf(initialType) }
    val isDuplicate = name.trim().isNotEmpty() && name.trim().lowercase() in existingNames
    val isValid = name.trim().isNotEmpty() && !isDuplicate
    val catColor = hexToColor(color)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
    ) {
        SheetHeader(title = title, onClose = onClose, modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp))
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Vista previa: así se va a ver en las listas.
            PredictaCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(emoji, color)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = name.ifBlank { "Nombre de la categoría" },
                        style = PredictaTypography.rowTitle,
                        color = if (name.isBlank()) PredictaColors.inkTertiary else PredictaColors.ink,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PredictaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre",
                    placeholder = "Mascotas",
                    enabled = nameEditable,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                )
                when {
                    isDuplicate -> Text("Ya tenés una categoría con ese nombre", style = PredictaTypography.rowSubtitle, color = PredictaColors.danger)
                    !nameEditable -> Text("Es de las que vienen con la app: podés cambiar ícono, color y tipo.", style = PredictaTypography.rowSubtitle, color = PredictaColors.inkSecondary)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Kicker("Tipo")
                SegmentedToggle(
                    options = listOf("Gasto", "Ingreso"),
                    selectedIndex = if (type == CategoryType.INCOME) 1 else 0,
                    onSelect = { type = if (it == 1) CategoryType.INCOME else CategoryType.EXPENSE },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Kicker("Ícono")
                EMOJI_GRID.chunked(8).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { e ->
                            val selected = e == emoji
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) catColor.copy(alpha = 0.22f) else PredictaColors.card)
                                    .border(if (selected) 2.dp else 1.dp, if (selected) catColor else PredictaColors.separator, RoundedCornerShape(12.dp))
                                    .clickable(role = Role.RadioButton) { emoji = e },
                            ) { Text(e, fontSize = 18.sp) }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Kicker("Color")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    COLOR_PALETTE.forEach { hex ->
                        val selected = hex == color
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(hexToColor(hex))
                                .clickable(role = Role.RadioButton) { color = hex },
                        ) {
                            if (selected) Icon(Icons.Default.Check, contentDescription = "Elegido", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        PrimaryButton(
            text = confirmLabel,
            enabled = isValid,
            onClick = { onConfirm(name.trim(), emoji, color, type) },
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = if (onDelete == null) 16.dp else 0.dp),
        )
        if (onDelete != null) {
            TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Eliminar categoría", style = PredictaTypography.label, color = PredictaColors.danger)
            }
        }
    }
}
