package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.data.model.ExpenseWithCategory
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.theme.categoryColor
import com.lucas.predictaapp.ui.utils.fmtArs
import com.lucas.predictaapp.ui.utils.relativeDateLabel

private data class CategoryStyle(val emoji: String, val bg: Color, val fg: Color)

private fun styleFor(category: String, emojiFor: (String) -> String, colorFor: (String) -> Color): CategoryStyle {
    val color = colorFor(category)
    return CategoryStyle(emojiFor(category), color.copy(alpha = 0.15f), color)
}
private val defaultStyle = CategoryStyle("💸", PredictaColors.surfaceHigh, PredictaColors.cream60)

@Composable
fun TransactionsCard(
    expenses: List<ExpenseWithCategory>,
    onDelete: (Expense) -> Unit = {},
    onEdit: (Expense) -> Unit = {},
    onSeeAll: (() -> Unit)? = null,
    emojiFor: (String) -> String = { ExpenseCategories.emojiFor(it) },
    colorFor: (String) -> Color = { categoryColor(it) },
) {
    var pendingDelete by remember { mutableStateOf<ExpenseWithCategory?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(PredictaColors.surfaceMid, PredictaColors.surface)))
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(16.dp)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            Text(
                text = "ÚLTIMAS TRANSACCIONES",
                style = PredictaTypography.monoCap.copy(
                    fontWeight = FontWeight.Medium,
                    color = PredictaColors.cream60,
                    letterSpacing = 2.2.sp,
                ),
                modifier = Modifier.weight(1f),
            )
            if (onSeeAll != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .padding(4.dp),
                ) {
                    Text(
                        text = "Ver todas",
                        style = PredictaTypography.small.copy(
                            fontWeight = FontWeight.Medium,
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

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PredictaColors.line))

        expenses.forEachIndexed { i, item ->
            TransactionRow(
                item = item,
                onClick = { onEdit(item.expense) },
                onLongClick = { pendingDelete = item },
                emojiFor = emojiFor,
                colorFor = colorFor,
            )
            if (i < expenses.lastIndex) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PredictaColors.line))
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = PredictaColors.surface,
            title = {
                Text(
                    text = "¿Eliminar transacción?",
                    style = PredictaTypography.cardTitle,
                    color = PredictaColors.cream,
                )
            },
            text = {
                Text(
                    text = "${item.merchant} · $${item.amount.fmtArs()}",
                    style = PredictaTypography.small,
                    color = PredictaColors.cream60,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(item.expense)
                    pendingDelete = null
                }) {
                    Text("Eliminar", color = PredictaColors.coral)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar", color = PredictaColors.cream60)
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionRow(
    item: ExpenseWithCategory,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    emojiFor: (String) -> String = { ExpenseCategories.emojiFor(it) },
    colorFor: (String) -> Color = { categoryColor(it) },
) {
    val isIncome = item.isIncome
    val style = if (isIncome) defaultStyle.copy(
        emoji = "💰",
        bg = PredictaColors.greenSoft,
        fg = PredictaColors.green,
    ) else styleFor(item.category, emojiFor, colorFor)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(style.bg),
        ) {
            Text(text = style.emoji, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.merchant,
                style = PredictaTypography.small.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = PredictaColors.cream,
                    lineHeight = 18.sp,
                ),
                maxLines = 1,
            )
            Text(
                text = item.category,
                style = PredictaTypography.monoCap.copy(
                    fontSize = 11.sp,
                    color = PredictaColors.cream60,
                    letterSpacing = 0.4.sp,
                ),
                maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isIncome) "+" else "−"}$${item.amount.fmtArs()}",
                style = PredictaTypography.bodyTight.copy(
                    fontSize = 14.sp,
                    color = if (isIncome) PredictaColors.green else PredictaColors.cream,
                    letterSpacing = (-0.1).sp,
                ),
            )
            Text(
                text = relativeDateLabel(item.dateMillis),
                style = PredictaTypography.monoCap.copy(
                    fontSize = 10.5.sp,
                    color = PredictaColors.cream35,
                ),
            )
        }
    }
}
