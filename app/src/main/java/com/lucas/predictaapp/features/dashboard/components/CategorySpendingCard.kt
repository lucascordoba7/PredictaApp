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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.data.model.ExpenseWithCategory
import com.lucas.predictaapp.ui.theme.IBMPlexMono
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.theme.categoryColor
import com.lucas.predictaapp.ui.utils.formatMonthYear
import com.lucas.predictaapp.ui.utils.fmtArs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySpendingCard(
    expenses: List<ExpenseWithCategory>,
    income: Int,
    emojiFor: (String) -> String = { ExpenseCategories.emojiFor(it) },
    colorFor: (String) -> Color = { categoryColor(it) },
) {
    val nonIncome = expenses.filter { !it.isIncome }
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
            .background(PredictaColors.surface)
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(14.dp))
            .clickable(enabled = grouped.isNotEmpty()) { showSheet = true }
            .padding(start = 17.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "GASTOS OCASIONALES",
                style = PredictaTypography.monoCap.copy(
                    color = PredictaColors.cream60,
                    letterSpacing = 0.5.sp,
                ),
            )
            if (totalSpent > 0) {
                Text(
                    text = "$ ${totalSpent.fmtArs()}",
                    style = PredictaTypography.monoCap.copy(
                        fontFamily = IBMPlexMono,
                        color = PredictaColors.cream,
                        letterSpacing = 0.sp,
                        fontSize = 11.sp,
                    ),
                )
            }
        }

        // Month + % context
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatMonthYear(),
                style = PredictaTypography.monoCap.copy(
                    color = PredictaColors.cream35,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                ),
            )
            if (income > 0 && totalSpent > 0) {
                Text(
                    text = "$usedPct% del ingreso",
                    style = PredictaTypography.monoCap.copy(
                        color = PredictaColors.cream35,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                    ),
                )
            }
        }

        // Stacked bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp)),
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
                            .background(colorFor(cat)),
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

        if (grouped.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Sin gastos registrados este mes",
                style = PredictaTypography.small.copy(color = PredictaColors.cream35),
            )
        } else {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                grouped.take(3).forEach { (cat, amount) ->
                    CategoryRow(
                        cat = cat,
                        amount = amount,
                        totalSpent = totalSpent,
                        emojiFor = emojiFor,
                        colorFor = colorFor,
                    )
                }
            }
            if (grouped.size > 3) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Ver todas (${grouped.size}) →",
                    style = PredictaTypography.small.copy(color = PredictaColors.amber),
                )
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
                emojiFor = emojiFor,
                colorFor = colorFor,
            )
        }
    }
}

@Composable
private fun CategoryRow(
    cat: String,
    amount: Int,
    totalSpent: Int,
    emojiFor: (String) -> String,
    colorFor: (String) -> Color,
) {
    val color = colorFor(cat)
    val pct = if (totalSpent > 0) (amount.toFloat() / totalSpent * 100).toInt() else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${emojiFor(cat)}  $cat",
            style = PredictaTypography.small.copy(color = PredictaColors.cream60, fontSize = 12.sp),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "$ ${amount.fmtArs()}",
            style = PredictaTypography.small.copy(
                fontFamily = IBMPlexMono,
                fontSize = 12.sp,
                color = PredictaColors.cream60,
            ),
        )
        Text(
            text = "  $pct%",
            style = PredictaTypography.monoCap.copy(
                color = color,
                fontSize = 9.sp,
            ),
            modifier = Modifier.width(30.dp),
        )
    }
}

@Composable
private fun CategoryBreakdownSheet(
    grouped: List<Pair<String, Int>>,
    totalSpent: Int,
    income: Int,
    usedPct: Int,
    emojiFor: (String) -> String = { ExpenseCategories.emojiFor(it) },
    colorFor: (String) -> Color = { categoryColor(it) },
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GASTOS DEL MES",
                    style = PredictaTypography.monoCap.copy(
                        color = PredictaColors.cream35,
                        letterSpacing = 1.5.sp,
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
                if (income > 0) {
                    Text(
                        text = "$usedPct% de $ ${income.fmtArs()}",
                        style = PredictaTypography.monoCap.copy(
                            color = PredictaColors.cream35,
                            fontSize = 10.sp,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PredictaColors.line))
        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            grouped.forEach { (cat, amount) ->
                val pct = if (totalSpent > 0) (amount.toFloat() / totalSpent * 100).toInt() else 0
                val barFraction = if (totalSpent > 0) (amount.toFloat() / totalSpent).coerceIn(0f, 1f) else 0f
                val color = colorFor(cat)

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${emojiFor(cat)}  $cat",
                            style = PredictaTypography.body.copy(
                                color = PredictaColors.cream,
                                fontSize = 14.sp,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "$ ${amount.fmtArs()}",
                            style = PredictaTypography.bodyTight.copy(
                                fontFamily = IBMPlexMono,
                                color = PredictaColors.cream,
                                fontSize = 13.sp,
                                letterSpacing = (-0.2).sp,
                            ),
                        )
                        Text(
                            text = "  $pct%",
                            style = PredictaTypography.monoCap.copy(
                                color = color,
                                fontSize = 9.5.sp,
                            ),
                            modifier = Modifier.width(32.dp),
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
                                .fillMaxWidth(barFraction)
                                .height(3.dp)
                                .background(color.copy(alpha = 0.7f)),
                        )
                    }
                }
            }
        }
    }
}
