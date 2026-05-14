package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.theme.categoryColor
import com.lucas.predictaapp.ui.utils.formatMonthYear
import com.lucas.predictaapp.ui.utils.fmtArs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySpendingCard(expenses: List<Expense>, income: Int) {
    val nonIncome = expenses.filter { it.category != "Ingreso" }
    val totalSpent = nonIncome.sumOf { it.amount }

    val grouped = nonIncome
        .groupBy { it.category }
        .map { (cat, items) -> cat to items.sumOf { it.amount } }
        .sortedByDescending { it.second }

    val maxAmount = if (income > 0) income.toFloat() else totalSpent.toFloat().coerceAtLeast(1f)
    val totalFraction = (totalSpent.toFloat() / maxAmount).coerceIn(0f, 1f)
    val usedPct = (totalFraction * 100).toInt()

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(PredictaColors.surfaceMid, PredictaColors.surface)))
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(14.dp))
            .clickable(enabled = grouped.isNotEmpty()) { showSheet = true }
            .padding(16.dp),
    ) {
        // Header
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
                text = if (totalSpent > 0) "$ ${totalSpent.fmtArs()}" else "Sin gastos aún",
                style = PredictaTypography.bodyTight.copy(
                    color = PredictaColors.cream,
                    fontSize = 15.sp,
                    letterSpacing = (-0.3).sp,
                ),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stacked bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
        ) {
            if (grouped.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(PredictaColors.surfaceHigh),
                )
            } else {
                grouped.forEach { (cat, amount) ->
                    val seg = if (totalSpent > 0)
                        (amount.toFloat() / totalSpent * totalFraction).coerceAtLeast(0.005f)
                    else 0f
                    Box(
                        modifier = Modifier
                            .weight(seg)
                            .fillMaxHeight()
                            .background(categoryColor(cat)),
                    )
                }
                val remaining = 1f - totalFraction
                if (remaining > 0.005f) {
                    Box(
                        modifier = Modifier
                            .weight(remaining)
                            .fillMaxHeight()
                            .background(PredictaColors.surfaceHigh),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Color dot legend (top 4)
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                grouped.take(4).forEach { (cat, _) ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(categoryColor(cat)),
                    )
                }
                if (grouped.size > 4) {
                    Text(
                        text = "+${grouped.size - 4}",
                        style = PredictaTypography.monoCap.copy(
                            color = PredictaColors.cream35,
                            fontSize = 9.sp,
                        ),
                    )
                }
            }
            Text(
                text = "$usedPct% del ingreso",
                style = PredictaTypography.monoCap.copy(
                    color = PredictaColors.cream35,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.8.sp,
                ),
            )
        }

        // Top 3 categories
        if (grouped.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PredictaColors.line))
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                grouped.take(3).forEach { (cat, amount) ->
                    TopCategoryRow(
                        cat = cat,
                        amount = amount,
                        totalSpent = totalSpent,
                    )
                }
                if (grouped.size > 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "ver todas",
                            style = PredictaTypography.small.copy(
                                color = PredictaColors.amber,
                                fontSize = 11.sp,
                            ),
                        )
                        Text(
                            text = " →",
                            style = PredictaTypography.small.copy(
                                color = PredictaColors.amber,
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = PredictaColors.surface,
            tonalElevation = 0.dp,
        ) {
            CategoryBreakdownSheet(
                grouped = grouped,
                totalSpent = totalSpent,
                income = income,
                usedPct = usedPct,
            )
        }
    }
}

@Composable
private fun TopCategoryRow(cat: String, amount: Int, totalSpent: Int) {
    val color = categoryColor(cat)
    val fraction = if (totalSpent > 0) (amount.toFloat() / totalSpent).coerceIn(0f, 1f) else 0f
    val pct = (fraction * 100).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${ExpenseCategories.emojiFor(cat)}  $cat",
                style = PredictaTypography.small.copy(
                    color = PredictaColors.cream60,
                    fontSize = 12.sp,
                ),
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
                    color = color,
                    fontSize = 10.sp,
                ),
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.End,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PredictaColors.surfaceHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(color.copy(alpha = 0.75f)),
            )
        }
    }
}

@Composable
private fun CategoryBreakdownSheet(
    grouped: List<Pair<String, Int>>,
    totalSpent: Int,
    income: Int,
    usedPct: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        // Sheet header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GASTOS POR CATEGORÍA",
                    style = PredictaTypography.monoCap.copy(
                        color = PredictaColors.cream35,
                        letterSpacing = 1.8.sp,
                        fontSize = 9.5.sp,
                    ),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatMonthYear().lowercase().replaceFirstChar { it.uppercase() },
                    style = PredictaTypography.cardTitle.copy(
                        color = PredictaColors.cream,
                        letterSpacing = (-0.3).sp,
                    ),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$ ${totalSpent.fmtArs()}",
                    style = PredictaTypography.kpiInline.copy(
                        color = PredictaColors.cream,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp,
                    ),
                )
                Text(
                    text = "$usedPct% de $ ${income.fmtArs()}",
                    style = PredictaTypography.monoCap.copy(
                        color = PredictaColors.cream35,
                        fontSize = 10.sp,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PredictaColors.line))
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            grouped.forEach { (cat, amount) ->
                val pct = if (totalSpent > 0) (amount.toFloat() / totalSpent * 100).toInt() else 0
                val barFraction = if (totalSpent > 0) (amount.toFloat() / totalSpent).coerceIn(0f, 1f) else 0f
                val color = categoryColor(cat)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${ExpenseCategories.emojiFor(cat)}  $cat",
                        style = PredictaTypography.body.copy(
                            color = PredictaColors.cream,
                            fontSize = 14.sp,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    // Mini bar
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PredictaColors.surfaceHigh),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barFraction)
                                .height(4.dp)
                                .background(color),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$ ${amount.fmtArs()}",
                            style = PredictaTypography.bodyTight.copy(
                                color = PredictaColors.cream,
                                fontSize = 13.sp,
                                letterSpacing = (-0.2).sp,
                            ),
                        )
                        Text(
                            text = "$pct%",
                            style = PredictaTypography.monoCap.copy(
                                color = PredictaColors.cream35,
                                fontSize = 9.5.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}
