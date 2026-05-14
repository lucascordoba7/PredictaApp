package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.formatMonthYear
import com.lucas.predictaapp.ui.utils.getDayOfMonth
import com.lucas.predictaapp.ui.utils.getDaysInMonth
import com.lucas.predictaapp.ui.utils.fmtArs
import com.lucas.predictaapp.ui.utils.fmtK

private data class ResumenCategoryStyle(val emoji: String, val color: Color)

private fun resumenCategoryStyle(name: String) = when (name) {
    "Salidas"        -> ResumenCategoryStyle("🍽", PredictaColors.coral)
    "Transporte"     -> ResumenCategoryStyle("🚕", PredictaColors.amber)
    "Salud"          -> ResumenCategoryStyle("💊", PredictaColors.green)
    "Delivery"       -> ResumenCategoryStyle("🛵", PredictaColors.coral)
    "Comida en casa" -> ResumenCategoryStyle("🛒", PredictaColors.cream60)
    "Supermercado"   -> ResumenCategoryStyle("🛒", PredictaColors.cream60)
    "Café"           -> ResumenCategoryStyle("☕", PredictaColors.amber)
    "Educación"      -> ResumenCategoryStyle("📚", PredictaColors.amber)
    "Suscripciones"  -> ResumenCategoryStyle("📱", PredictaColors.cream60)
    "Servicios"      -> ResumenCategoryStyle("⚡", PredictaColors.cream60)
    else             -> ResumenCategoryStyle("💸", PredictaColors.cream60)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenCard(
    spent: Int,
    income: Int,
    projected: Int,
    realPct: Int,
    projectedPct: Int,
    deltaVsLastMonth: Int,
    categories: List<Pair<String, Int>> = emptyList(),
) {
    val projOnly = (projectedPct - realPct).coerceAtLeast(0)
    val deltaColor = if (deltaVsLastMonth < 0) PredictaColors.green else PredictaColors.coral
    val deltaSign = if (deltaVsLastMonth < 0) "↓" else "↑"
    val deltaPct = Math.abs(deltaVsLastMonth)
    val categoryTotal = categories.sumOf { it.second }.coerceAtLeast(1)
    var showSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, PredictaColors.line, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = formatMonthYear(),
                style = PredictaTypography.monoCap.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                    color = PredictaColors.cream60,
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "día ${getDayOfMonth()} / ${getDaysInMonth()}",
                style = PredictaTypography.monoCap.copy(
                    letterSpacing = 1.2.sp,
                    color = PredictaColors.amber.copy(alpha = 0.7f),
                ),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$ ${spent.fmtArs()}",
            style = PredictaTypography.scoreHero.copy(
                fontSize = 42.sp,
                color = PredictaColors.cream,
                letterSpacing = (-1).sp,
            ),
        )

        Text(
            text = buildAnnotatedString {
                append("gastado de ")
                withStyle(SpanStyle(color = PredictaColors.cream, fontWeight = FontWeight.Medium)) {
                    append("$ ${projected.fmtK()}")
                }
                append(" de tope · ")
                withStyle(SpanStyle(color = deltaColor, fontWeight = FontWeight.SemiBold)) {
                    append("$deltaSign$deltaPct%")
                }
            },
            style = PredictaTypography.small.copy(
                color = PredictaColors.cream60,
                lineHeight = 18.sp,
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))
        ProgressBar(realPct = realPct, projOnly = projOnly)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            LegendDot(color = PredictaColors.amber, label = "Real $realPct%")
            Spacer(modifier = Modifier.weight(1f))
            LegendDot(
                color = PredictaColors.amber.copy(alpha = 0.45f),
                label = "Proyectado $projectedPct%",
            )
        }

        if (categories.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PredictaColors.line),
            )
            Spacer(modifier = Modifier.height(14.dp))
            CategorySection(
                categories = categories,
                categoryTotal = categoryTotal,
                onSeeAll = { showSheet = true },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PredictaColors.line),
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Ingresos del mes",
                style = PredictaTypography.small.copy(color = PredictaColors.cream35),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "+ $ ${income.fmtArs()}",
                style = PredictaTypography.bodyTight.copy(
                    color = PredictaColors.green,
                    letterSpacing = (-0.2).sp,
                ),
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = PredictaColors.surface,
        ) {
            AllCategoriesSheet(categories = categories, categoryTotal = categoryTotal)
        }
    }
}

@Composable
private fun CategorySection(
    categories: List<Pair<String, Int>>,
    categoryTotal: Int,
    onSeeAll: () -> Unit,
) {
    val top3 = categories.take(3)
    val hasMore = categories.size > 3

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "CATEGORÍAS",
                style = PredictaTypography.monoCap.copy(
                    color = PredictaColors.cream35,
                    letterSpacing = 1.5.sp,
                ),
                modifier = Modifier.weight(1f),
            )
            if (hasMore) {
                Row(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSeeAll,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "ver todas",
                        style = PredictaTypography.small.copy(
                            color = PredictaColors.amber,
                            fontSize = 12.sp,
                        ),
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = PredictaColors.amber,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        top3.forEach { (name, amount) ->
            CategoryRow(name = name, amount = amount, categoryTotal = categoryTotal)
        }
    }
}

@Composable
private fun CategoryRow(name: String, amount: Int, categoryTotal: Int) {
    val style = resumenCategoryStyle(name)
    val fraction = (amount.toFloat() / categoryTotal).coerceIn(0f, 1f)
    val pct = (fraction * 100).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = style.emoji, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                style = PredictaTypography.small.copy(color = PredictaColors.cream60),
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Text(
                text = "$ ${amount.fmtArs()}",
                style = PredictaTypography.bodyTight.copy(
                    fontSize = 13.sp,
                    color = PredictaColors.cream,
                    letterSpacing = (-0.2).sp,
                ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$pct%",
                style = PredictaTypography.monoCap.copy(
                    color = style.color,
                    fontSize = 10.sp,
                ),
                modifier = Modifier.width(26.dp),
                textAlign = TextAlign.End,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(PredictaColors.surfaceHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(style.color.copy(alpha = 0.65f)),
            )
        }
    }
}

@Composable
private fun AllCategoriesSheet(
    categories: List<Pair<String, Int>>,
    categoryTotal: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "GASTOS POR CATEGORÍA",
            style = PredictaTypography.monoCap.copy(
                color = PredictaColors.cream35,
                letterSpacing = 1.5.sp,
            ),
        )
        categories.forEach { (name, amount) ->
            CategoryRow(name = name, amount = amount, categoryTotal = categoryTotal)
        }
    }
}

@Composable
private fun ProgressBar(realPct: Int, projOnly: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(PredictaColors.surfaceHigh),
    ) {
        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            if (realPct > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((realPct / 100f).coerceIn(0f, 1f))
                        .background(PredictaColors.amber),
                )
            }
            if (projOnly > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((projOnly / 100f).coerceIn(0f, 1f - realPct / 100f))
                        .background(PredictaColors.amber.copy(alpha = 0.40f)),
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
        Text(
            text = "  $label",
            style = PredictaTypography.monoCap.copy(
                color = PredictaColors.cream60,
                fontSize = 10.5.sp,
            ),
        )
    }
}
