package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.lucas.predictaapp.ui.components.DeleteExpenseDialog
import com.lucas.predictaapp.ui.components.TransactionRow
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.theme.categoryColor

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
                        .clickable(onClick = onSeeAll)
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
        DeleteExpenseDialog(
            item = item,
            onConfirm = {
                onDelete(item.expense)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}
