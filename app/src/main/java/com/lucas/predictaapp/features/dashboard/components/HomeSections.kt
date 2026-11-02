package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.data.model.FixedExpense
import com.lucas.predictaapp.ui.components.AnimatedAmount
import com.lucas.predictaapp.ui.components.GlyphBadge
import com.lucas.predictaapp.ui.components.HeroCard
import com.lucas.predictaapp.ui.components.PaidCheck
import com.lucas.predictaapp.ui.components.PredictaCard
import com.lucas.predictaapp.ui.components.RowDivider
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs

/** Variación del gasto del día a día contra el mes anterior (a la misma altura del mes). */
data class SpendComparison(val pct: Int, val previousMonthName: String)

/** Hero de la home: cuánto gastaste, comparación y división fijos / día a día. */
@Composable
fun SpendHeroCard(
    monthSpend: Int,
    fixed: Int,
    variable: Int,
    comparison: SpendComparison?,
    label: String,
    modifier: Modifier = Modifier,
) {
    val onHero = Color.White
    val onHeroDim = Color.White.copy(alpha = 0.85f)
    // Compacta: el número manda, pero no se come media pantalla.
    HeroCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
        Column {
            Text(text = label, style = PredictaTypography.label.copy(fontSize = 14.sp), color = onHeroDim)
            AnimatedAmount(
                value = monthSpend,
                style = PredictaTypography.display.copy(fontSize = 38.sp, lineHeight = 44.sp, letterSpacing = (-1.4).sp),
                color = onHero,
                formatter = { "$${it.fmtArs()}" },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeroTotal("Fijos", fixed)
                HeroTotal("Día a día", variable)
            }
        }
        if (comparison != null) {
            val less = comparison.pct < 0
            val text = when {
                comparison.pct == 0 -> "Día a día: igual que ${comparison.previousMonthName}"
                less -> "Día a día: ${-comparison.pct}% menos que ${comparison.previousMonthName}"
                else -> "Día a día: ${comparison.pct}% más que ${comparison.previousMonthName}"
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            ) {
                if (comparison.pct != 0) {
                    Icon(
                        imageVector = if (less) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        tint = onHero,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(text = text, style = PredictaTypography.caption.copy(fontSize = 13.sp, fontWeight = PredictaTypography.label.fontWeight), color = onHero)
            }
        }
    }
}

@Composable
private fun HeroTotal(label: String, amount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        Text(text = label, style = PredictaTypography.small, color = Color.White.copy(alpha = 0.85f))
        Text(text = "$${amount.fmtArs()}", style = PredictaTypography.label.copy(fontSize = 15.sp, fontWeight = PredictaTypography.rowAmount.fontWeight), color = Color.White)
    }
}

/**
 * Un pago que viene este mes. Los gastos fijos los paga el usuario ([fixed] no nulo: se marcan
 * pagados desde acá); las suscripciones se cobran solas el día de cobro.
 */
data class UpcomingPayment(
    val key: String,
    val name: String,
    val amount: Int,
    val dueDay: Int,
    val daysUntil: Int,
    val fixed: FixedExpense? = null,
) {
    val isSubscription: Boolean get() = fixed == null
    val overdue: Boolean get() = daysUntil < 0
}

/** Cuántas filas entran en la home; el resto queda detrás de "Ver todos". */
const val UPCOMING_VISIBLE = 3

/**
 * Lo que queda por pagar este mes. Los fijos van fila por fila (los pagás vos: círculo para
 * marcarlos); las suscripciones se cobran solas, así que van todas juntas en una fila de resumen
 * al pie. Si los fijos ya están pagos y quedan suscripciones, una línea lo confirma arriba.
 */
@Composable
fun UpcomingPaymentsCard(
    fixed: List<UpcomingPayment>,
    subscriptions: List<UpcomingPayment>,
    fixedAllPaid: Boolean,
    monthShort: String,
    onTogglePaid: (FixedExpense) -> Unit,
    onOpenFixed: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = fixed.take(UPCOMING_VISIBLE)
    PredictaCard(modifier = modifier.fillMaxWidth().animateContentSize()) {
        if (fixedAllPaid) {
            FixedPaidRow(onClick = onOpenFixed)
        }
        visible.forEachIndexed { i, payment ->
            if (i > 0) RowDivider(inset = 54.dp)
            UpcomingRow(payment, monthShort, onTogglePaid = onTogglePaid, onClick = onOpenFixed)
        }
        if (fixed.size > UPCOMING_VISIBLE) {
            RowDivider(inset = 54.dp)
            Text(
                text = "y ${fixed.size - UPCOMING_VISIBLE} más",
                style = PredictaTypography.label,
                color = PredictaColors.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onOpenFixed)
                    .padding(start = 54.dp, top = 12.dp, bottom = 12.dp),
            )
        }
        if (subscriptions.isNotEmpty()) {
            if (visible.isNotEmpty() || fixedAllPaid) RowDivider()
            SubscriptionsRow(subscriptions, onClick = onOpenSubscriptions)
        }
    }
}

/** Todas las suscripciones que faltan cobrar este mes, en una fila: cuántas, cuánto y la próxima. */
@Composable
private fun SubscriptionsRow(subscriptions: List<UpcomingPayment>, onClick: () -> Unit) {
    val next = subscriptions.minByOrNull { it.daysUntil }
    val count = subscriptions.size
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        GlyphBadge(container = PredictaColors.neutralSoft) {
            Icon(Icons.Default.Autorenew, contentDescription = null, tint = PredictaColors.inkSecondary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (count == 1) "1 suscripción" else "$count suscripciones",
                style = PredictaTypography.rowTitle,
                color = PredictaColors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (next != null) {
                Text(
                    text = "${next.name} " + when (next.daysUntil) {
                        0 -> "se cobra hoy"
                        1 -> "se cobra mañana"
                        else -> "se cobra en ${next.daysUntil} días"
                    },
                    style = PredictaTypography.rowSubtitle,
                    color = PredictaColors.inkSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("$${subscriptions.sumOf { it.amount }.fmtArs()}", style = PredictaTypography.rowAmount, color = PredictaColors.ink)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PredictaColors.inkTertiary,
            modifier = Modifier.padding(start = 4.dp).size(20.dp),
        )
    }
}

@Composable
private fun FixedPaidRow(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        GlyphBadge(container = PredictaColors.successSoft) {
            Icon(Icons.Default.Check, contentDescription = null, tint = PredictaColors.success, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text("Gastos fijos pagados", style = PredictaTypography.rowTitle, color = PredictaColors.ink)
    }
}

/** Subtítulo del encabezado: "$145.500 · 2 vencidos". */
fun upcomingSummary(payments: List<UpcomingPayment>): String {
    val overdue = payments.count { it.overdue }
    return buildString {
        append("$${payments.sumOf { it.amount }.fmtArs()}")
        when {
            overdue == 1 -> append(" · 1 vencido")
            overdue > 1 -> append(" · $overdue vencidos")
            payments.size > UPCOMING_VISIBLE -> append(" · ${payments.size} pagos")
        }
    }
}

@Composable
private fun UpcomingRow(
    payment: UpcomingPayment,
    monthShort: String,
    onTogglePaid: (FixedExpense) -> Unit,
    onClick: () -> Unit,
) {
    val d = payment.daysUntil
    val (status, statusColor) = when {
        payment.isSubscription -> when (d) {
            0 -> "Se cobra solo hoy"
            1 -> "Se cobra solo mañana"
            else -> "Se cobra solo en $d días"
        } to PredictaColors.inkSecondary
        d < -1 -> "Venció hace ${-d} días" to PredictaColors.danger
        d == -1 -> "Venció ayer" to PredictaColors.danger
        d == 0 -> "Vence hoy" to PredictaColors.accent
        d == 1 -> "Vence mañana" to PredictaColors.accent
        else -> "Vence en $d días" to PredictaColors.inkSecondary
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        DateBadge(day = payment.dueDay, monthShort = monthShort, overdue = payment.overdue, muted = payment.isSubscription)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(payment.name, style = PredictaTypography.rowTitle, color = PredictaColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(status, style = PredictaTypography.rowSubtitle.copy(fontWeight = PredictaTypography.label.fontWeight), color = statusColor)
        }
        Spacer(Modifier.width(8.dp))
        Text("$${payment.amount.fmtArs()}", style = PredictaTypography.rowAmount, color = PredictaColors.ink)
        val fixed = payment.fixed
        if (fixed != null) {
            Spacer(Modifier.width(4.dp))
            PaidCheck(
                checked = false,
                ring = if (payment.overdue) PredictaColors.danger else PredictaColors.warn,
                onClick = { onTogglePaid(fixed) },
                contentDescription = "Marcar ${payment.name} como pagado",
            )
        }
    }
}

/** Hoja de calendario: el día grande y el mes abreviado arriba. */
@Composable
private fun DateBadge(day: Int, monthShort: String, overdue: Boolean, muted: Boolean) {
    val (bg, fg) = when {
        overdue -> PredictaColors.dangerSoft to PredictaColors.danger
        muted -> PredictaColors.neutralSoft to PredictaColors.inkSecondary
        else -> PredictaColors.accentSoft to PredictaColors.accent
    }
    GlyphBadge(container = bg) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(monthShort.uppercase(), style = PredictaTypography.kicker.copy(fontSize = 9.sp, lineHeight = 10.sp, letterSpacing = 0.5.sp), color = fg)
            Text("%02d".format(day), style = PredictaTypography.rowAmount.copy(fontSize = 16.sp, lineHeight = 18.sp), color = fg)
        }
    }
}

/** Una porción del gasto del mes por categoría. */
data class CategorySlice(val name: String, val emoji: String, val amount: Int, val pct: Int)

/** Cuántas categorías se listan; el resto se suma en "Otras". */
private const val BREAKDOWN_ROWS = 4

/**
 * En qué se va el día a día: una fila por categoría con su emoji, monto, porcentaje y una barra
 * del mismo color para todas (el largo es lo que compara). Tocar una fila abre Actividad filtrada.
 */
@Composable
fun CategoryBreakdownCard(
    slices: List<CategorySlice>,
    onCategoryClick: (String) -> Unit,
    onOthersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = slices.take(BREAKDOWN_ROWS)
    val rest = slices.drop(BREAKDOWN_ROWS)
    PredictaCard(modifier = modifier.fillMaxWidth()) {
        top.forEachIndexed { i, slice ->
            if (i > 0) RowDivider(inset = 54.dp)
            BreakdownRow(
                emoji = slice.emoji,
                name = slice.name,
                amount = slice.amount,
                pct = slice.pct,
                onClick = { onCategoryClick(slice.name) },
            )
        }
        if (rest.isNotEmpty()) {
            RowDivider(inset = 54.dp)
            BreakdownRow(
                emoji = "📦",
                name = if (rest.size == 1) rest.first().name else "Otras ${rest.size} categorías",
                amount = rest.sumOf { it.amount },
                pct = rest.sumOf { it.pct },
                onClick = { if (rest.size == 1) onCategoryClick(rest.first().name) else onOthersClick() },
            )
        }
    }
}

@Composable
private fun BreakdownRow(emoji: String, name: String, amount: Int, pct: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        GlyphBadge {
            Text(emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = PredictaTypography.rowTitle, color = PredictaColors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("$${amount.fmtArs()}", style = PredictaTypography.rowAmount.copy(fontSize = 16.sp), color = PredictaColors.ink)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(PredictaColors.neutralSoft),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth((pct / 100f).coerceIn(0.02f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(PredictaColors.accent),
                    )
                }
                Text(
                    text = "$pct%",
                    style = PredictaTypography.label.copy(fontSize = 12.sp),
                    color = PredictaColors.inkSecondary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(44.dp),
                )
            }
        }
    }
}
