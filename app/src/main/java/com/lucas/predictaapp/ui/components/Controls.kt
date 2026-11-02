package com.lucas.predictaapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography

/*
 * Controles del lenguaje Atardecer: header de pantalla, botones, chips, campos, sheet y diálogo.
 * Todas las pantallas los comparten para que una tarjeta, un botón o un chip se vean igual en
 * cualquier lado y cambien de modo claro/oscuro sin tocar cada pantalla.
 */

/** Botón cuadrado translúcido del header (volver, campana, flechas de mes, cerrar). */
@Composable
fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: Boolean = false,
    container: Color = PredictaColors.glass,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) container else PredictaColors.glassDisabled)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) PredictaColors.ink else PredictaColors.inkTertiary.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp),
        )
        if (badge) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 11.dp).size(8.dp).clip(CircleShape).background(PredictaColors.accent))
        }
    }
}

/**
 * Header de pantalla: título grande a la izquierda y acciones a la derecha. Con [onBack] es el de
 * una pantalla de detalle (botón volver adelante y título un paso más chico).
 */
@Composable
fun ScreenTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
    ) {
        if (onBack != null) {
            HeaderIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Volver", onBack)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            if (subtitle != null) Kicker(subtitle)
            Text(
                text = title,
                style = if (onBack != null) PredictaTypography.title else PredictaTypography.titlePage,
                color = PredictaColors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** La acción principal de una vista: degradé de marca, texto blanco. Deshabilitado, gris neutro. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(PredictaDimensions.Heights.cta)
            .then(if (enabled) Modifier.softShadow(shape, elevation = 8.dp) else Modifier)
            .clip(shape)
            .then(if (enabled) Modifier.background(PredictaColors.primaryBrush) else Modifier.background(PredictaColors.neutralSoft))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
    ) {
        val content = if (enabled) Color.White else PredictaColors.inkTertiary
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = PredictaTypography.button, color = content)
    }
}

/** Acción secundaria a lo ancho: tarjeta con texto de acento. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    content: Color = PredictaColors.accent,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(PredictaColors.card)
            .border(1.dp, PredictaColors.border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = PredictaTypography.label.copy(fontSize = 15.sp), color = content)
    }
}

/** Chip de filtro o de elección. Elegido: relleno de marca; si no, tarjeta con borde. */
@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: String? = null,
) {
    val shape = RoundedCornerShape(PredictaDimensions.Radius.pill)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selected) PredictaColors.heroStart else PredictaColors.card)
            .border(1.dp, if (selected) PredictaColors.heroStart else PredictaColors.border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        if (leading != null) {
            Text(text = leading, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            maxLines = 1,
            style = PredictaTypography.label.copy(fontWeight = if (selected) PredictaTypography.label.fontWeight else PredictaTypography.bodyTight.fontWeight),
            color = if (selected) Color.White else PredictaColors.ink,
        )
    }
}

/** Selector de dos o más opciones en una píldora (Gasto / Ingreso, Mensual / Anual). */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PredictaColors.card)
            .border(1.dp, PredictaColors.separator, RoundedCornerShape(14.dp))
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) PredictaColors.heroStart else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(i) }
                    .padding(horizontal = 18.dp),
            ) {
                Text(text = label, style = PredictaTypography.label, color = if (selected) Color.White else PredictaColors.inkSecondary)
            }
        }
    }
}

/** Buscador en tarjeta, con lupa y botón para limpiar. */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .softShadow(shape, elevation = 6.dp)
            .clip(shape)
            .background(PredictaColors.card)
            .padding(horizontal = 16.dp),
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = PredictaColors.inkSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle = PredictaTypography.body.copy(color = PredictaColors.ink),
            cursorBrush = SolidColor(PredictaColors.accent),
            singleLine = true,
            decorationBox = { inner ->
                if (query.isEmpty()) Text(placeholder, style = PredictaTypography.body, color = PredictaColors.inkTertiary)
                inner()
            },
        )
        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Limpiar búsqueda",
                tint = PredictaColors.inkSecondary,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onQueryChange("") }
                    .padding(7.dp),
            )
        }
    }
}

/**
 * Campo de texto: tarjeta de 52dp con rótulo arriba y borde de marca al enfocar. Reemplaza al
 * OutlinedTextField de Material, que no sigue la paleta.
 */
@Composable
fun PredictaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    prefix: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label != null) Kicker(label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            interactionSource = interaction,
            enabled = enabled,
            textStyle = PredictaTypography.body.copy(fontSize = 16.sp, fontWeight = PredictaTypography.bodyTight.fontWeight, color = if (enabled) PredictaColors.ink else PredictaColors.inkSecondary),
            cursorBrush = SolidColor(PredictaColors.accent),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(shape)
                        .background(if (enabled) PredictaColors.card else PredictaColors.neutralSoft)
                        .border(if (focused) 2.dp else 1.dp, if (focused) PredictaColors.accent else PredictaColors.border, shape)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    if (prefix != null) {
                        Text(prefix, style = PredictaTypography.body.copy(fontSize = 16.sp), color = PredictaColors.inkSecondary)
                        Spacer(Modifier.width(4.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = PredictaTypography.body.copy(fontSize = 16.sp), color = PredictaColors.inkTertiary)
                        }
                        inner()
                    }
                }
            },
        )
    }
}

/** Fila de una tarjeta que abre algo: insignia, título, detalle y chevron. */
@Composable
fun PickerRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: ImageVector? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .softShadow(shape, elevation = 6.dp)
            .clip(shape)
            .background(PredictaColors.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        GlyphBadge(size = 40.dp) {
            Icon(icon, contentDescription = null, tint = PredictaColors.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = PredictaTypography.rowTitle.copy(fontSize = 15.sp), color = PredictaColors.ink)
            if (subtitle != null) Text(subtitle, style = PredictaTypography.rowSubtitle, color = PredictaColors.inkSecondary)
        }
        if (trailing != null) Icon(trailing, contentDescription = null, tint = PredictaColors.inkSecondary, modifier = Modifier.size(20.dp))
    }
}

/** Estado vacío: emoji grande, título y una línea de ayuda. Centrado, es de las pocas excepciones. */
@Composable
fun EmptyState(
    emoji: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
    ) {
        GlyphBadge(size = 64.dp) { Text(emoji, fontSize = 28.sp) }
        Spacer(Modifier.height(8.dp))
        Text(title, style = PredictaTypography.heading, color = PredictaColors.ink, textAlign = TextAlign.Center)
        Text(body, style = PredictaTypography.body, color = PredictaColors.inkSecondary, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(10.dp))
            action()
        }
    }
}

/**
 * Bottom sheet de la app: fondo papel, esquinas de 30dp y manija tenue. Con [wash] lleva el velo
 * de marca arriba, como las pantallas (para formularios de carga). El contenido arma su padding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictaSheet(
    onDismiss: () -> Unit,
    wash: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    // El alto de la barra de estado se lee acá, en la ventana de la actividad.
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PredictaColors.paper,
        contentColor = PredictaColors.ink,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        scrimColor = Color(0xFF2A140A).copy(alpha = 0.48f),
        // La manija va dentro del contenido para que el velo la cubra también.
        dragHandle = null,
    ) {
        // Tope de altura sobre el espacio disponible de verdad (con teclado abierto es mucho
        // menos que la pantalla): el sheet nunca se mete debajo de la barra de estado.
        BoxWithConstraints {
        val maxHeight = maxHeight - statusBar - 12.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .then(if (wash) Modifier.background(PredictaColors.sheetWash) else Modifier),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp, bottom = 10.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(PredictaColors.border),
            )
            content()
        }
        }
    }
}

/** Título de sheet con botón de cerrar a la derecha. */
@Composable
fun SheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(verticalAlignment = Alignment.Top, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = PredictaTypography.title, color = PredictaColors.ink)
            if (subtitle != null) Text(subtitle, style = PredictaTypography.body.copy(fontSize = 14.sp), color = PredictaColors.inkSecondary)
        }
        HeaderIconButton(Icons.Default.Close, "Cerrar", onClose, container = PredictaColors.neutralSoft)
    }
}

/** Diálogo de confirmación. [destructive] pinta la acción en rojo (borrar, cerrar sesión). */
@Composable
fun PredictaDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissLabel: String = "Cancelar",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PredictaColors.card,
        shape = RoundedCornerShape(PredictaDimensions.Radius.card),
        title = { Text(title, style = PredictaTypography.heading, color = PredictaColors.ink) },
        text = { Text(text, style = PredictaTypography.body, color = PredictaColors.inkSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, style = PredictaTypography.label, color = if (destructive) PredictaColors.danger else PredictaColors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, style = PredictaTypography.label, color = PredictaColors.inkSecondary)
            }
        },
    )
}

/** Separador de filas dentro de una tarjeta. */
@Composable
fun RowDivider(modifier: Modifier = Modifier, inset: Dp = 0.dp) {
    Box(
        modifier
            .padding(start = inset)
            .fillMaxWidth()
            .height(1.dp)
            .background(PredictaColors.separator),
    )
}

/**
 * Círculo de "pagado": anillo del color de estado (vencido, pendiente) o relleno verde con tilde.
 * Área táctil de 40dp para un círculo de 28.
 */
@Composable
fun PaidCheck(
    checked: Boolean,
    ring: Color,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .then(if (checked) Modifier.background(PredictaColors.success) else Modifier.border(2.dp, ring, CircleShape)),
        ) {
            if (checked) Icon(Icons.Default.Check, contentDescription = null, tint = PredictaColors.card, modifier = Modifier.size(18.dp))
        }
    }
}
