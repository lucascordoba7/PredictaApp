package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.data.model.FixedExpense
import com.lucas.predictaapp.data.model.FixedExpenseStatus
import com.lucas.predictaapp.data.model.computeStatus
import com.lucas.predictaapp.ui.theme.IBMPlexMono
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs

@Composable
fun FixedExpensesCard(
    items: List<FixedExpense>,
    onManageClick: () -> Unit,
) {
    val totalMonthly = items.sumOf { it.amount }
    val amber = PredictaColors.amber
    val surface = PredictaColors.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(14.dp))
            .drawBehind {
                drawRect(surface)
                drawRect(
                    color = amber,
                    topLeft = Offset.Zero,
                    size = Size(3.dp.toPx(), size.height),
                )
            }
            .padding(start = 17.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "GASTOS FIJOS",
                style = PredictaTypography.monoCap.copy(
                    color = PredictaColors.cream60,
                    letterSpacing = 0.5.sp,
                ),
            )
            if (items.isNotEmpty()) {
                Text(
                    text = "$ ${totalMonthly.fmtArs()}",
                    style = PredictaTypography.monoCap.copy(
                        color = PredictaColors.amber,
                        letterSpacing = 0.5.sp,
                    ),
                )
            }
        }

        if (items.isEmpty()) {
            Spacer(Modifier.height(10.dp))
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(PredictaColors.amberSoft)
                    .clickable(onClick = onManageClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    text = "Cargar alquiler, servicios, etc.",
                    style = PredictaTypography.small.copy(color = PredictaColors.amber),
                )
                Text(
                    text = "+ Agregar",
                    style = PredictaTypography.small.copy(color = PredictaColors.amber),
                )
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.take(3).forEach { item ->
                    val status = item.computeStatus()
                    val statusColor = when (status) {
                        FixedExpenseStatus.PAGADO -> PredictaColors.green
                        FixedExpenseStatus.VENCIDO -> PredictaColors.coral
                        FixedExpenseStatus.PENDIENTE -> PredictaColors.pending
                    }
                    val statusLabel = when (status) {
                        FixedExpenseStatus.PAGADO -> "PAGADO"
                        FixedExpenseStatus.VENCIDO -> "VENCIDO"
                        FixedExpenseStatus.PENDIENTE -> "día ${item.dueDayOfMonth}"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.name,
                            style = PredictaTypography.small.copy(color = PredictaColors.cream),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "$ ${item.amount.fmtArs()}",
                            style = PredictaTypography.small.copy(
                                fontFamily = IBMPlexMono,
                                fontSize = 12.sp,
                                color = PredictaColors.cream60,
                            ),
                        )
                        Text(
                            text = statusLabel,
                            style = PredictaTypography.caption.copy(color = statusColor),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = if (items.size > 3) "Ver todos (${items.size}) →" else "Gestionar →",
                style = PredictaTypography.small.copy(color = PredictaColors.amber),
                modifier = Modifier.clickable(onClick = onManageClick),
            )
        }
    }
}
