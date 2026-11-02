package com.lucas.predictaapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    PredictaDialog(
        title = "¿Eliminar movimiento?",
        text = "${item.merchant} · $${item.amount.fmtArs()}. No se puede deshacer.",
        confirmLabel = "Eliminar",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        destructive = true,
    )
}

/**
 * Fila de transacción compartida (home + Actividad): insignia con el emoji de la categoría,
 * comercio, categoría, monto y fecha relativa. Tap edita, long-press borra.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    item: ExpenseWithCategory,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    emojiFor: (String) -> String = { ExpenseCategories.emojiFor(it) },
    @Suppress("UNUSED_PARAMETER") colorFor: (String) -> Color = { categoryColor(it) },
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    /** Detalle bajo el comercio. Por defecto la categoría; Actividad le suma la hora. */
    subtitle: String? = null,
    /** Fecha relativa bajo el monto. Se apaga cuando la lista ya agrupa por día. */
    showDate: Boolean = true,
) {
    val isIncome = item.isIncome
    val emoji = if (isIncome) "💰" else emojiFor(item.category)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(contentPadding),
    ) {
        GlyphBadge(container = if (isIncome) PredictaColors.successSoft else PredictaColors.accentSoft) {
            Text(text = emoji, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.merchant,
                style = PredictaTypography.rowTitle,
                color = PredictaColors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = (if (item.isSubscription) "🔁 " else "") + (subtitle ?: item.category),
                style = PredictaTypography.rowSubtitle,
                color = PredictaColors.inkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isIncome) "+" else ""}$${item.amount.fmtArs()}",
                style = PredictaTypography.rowAmount,
                color = if (isIncome) PredictaColors.success else PredictaColors.ink,
            )
            if (showDate) {
                Text(
                    text = relativeDateLabel(item.dateMillis),
                    style = PredictaTypography.caption.copy(fontWeight = FontWeight.Bold),
                    color = PredictaColors.inkSecondary,
                )
            }
        }
    }
}
