package com.lucas.predictaapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.data.model.ExpenseWithCategory
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.theme.categoryColor
import com.lucas.predictaapp.ui.utils.fmtArs
import com.lucas.predictaapp.ui.utils.relativeDateLabel

/** Confirmación de borrado de una transacción (compartido dashboard/Transacciones). */
@Composable
fun DeleteExpenseDialog(
    item: ExpenseWithCategory,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
            TextButton(onClick = onConfirm) {
                Text("Eliminar", color = PredictaColors.coral)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = PredictaColors.cream60)
            }
        },
    )
}

/**
 * Fila de transacción compartida (card del dashboard + pantalla Transacciones):
 * avatar de categoría, comercio, monto y fecha relativa. Tap edita, long-press borra.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    item: ExpenseWithCategory,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    emojiFor: (String) -> String = { ExpenseCategories.emojiFor(it) },
    colorFor: (String) -> Color = { categoryColor(it) },
) {
    val isIncome = item.isIncome
    val avatarColor = if (isIncome) PredictaColors.green else colorFor(item.category)
    val avatarBg = if (isIncome) PredictaColors.greenSoft else avatarColor.copy(alpha = 0.15f)
    val emoji = if (isIncome) "💰" else emojiFor(item.category)

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
                .background(avatarBg),
        ) {
            Text(text = emoji, fontSize = 16.sp)
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
                text = if (item.isSubscription) "🔁 ${item.category}" else item.category,
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
