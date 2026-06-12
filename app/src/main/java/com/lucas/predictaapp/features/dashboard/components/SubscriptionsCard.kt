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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.data.model.Subscription
import com.lucas.predictaapp.data.model.daysSinceLastUsed
import com.lucas.predictaapp.data.model.effectiveBillingDay
import com.lucas.predictaapp.data.model.isChargedThisMonth
import com.lucas.predictaapp.data.model.isZombie
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs
import java.time.LocalDate

private val SubscriptionAccent = Color(0xFF9D7AE8) // violeta de la categoría Suscripciones

@Composable
fun SubscriptionsCard(
    subscriptions: List<Subscription>,
    onManageClick: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    val active = subscriptions.filter { it.active }
    val totalMonthly = active.sumOf { it.monthly }
    val pendingCount = active.count { !it.isChargedThisMonth() }
    val zombies = subscriptions.filter { it.isZombie(today) }
    val surface = PredictaColors.surface

    // Orden: pendientes de cobro primero (accionable), luego el resto. Hasta 3 filas.
    val rows = active.sortedByDescending { !it.isChargedThisMonth() }.take(3)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(14.dp))
            .drawBehind {
                drawRect(surface)
                drawRect(
                    color = SubscriptionAccent,
                    topLeft = Offset.Zero,
                    size = Size(3.dp.toPx(), size.height),
                )
            }
            .padding(start = 17.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SUSCRIPCIONES",
                style = PredictaTypography.monoCap.copy(
                    color = PredictaColors.cream60,
                    letterSpacing = 0.5.sp,
                ),
            )
            Text(
                text = "$ ${totalMonthly.fmtArs()}/mes",
                style = PredictaTypography.monoCap.copy(
                    color = SubscriptionAccent,
                    letterSpacing = 0.5.sp,
                ),
            )
        }
        // Subtítulo: activas · estado de cobro del mes
        Text(
            text = buildString {
                append("${active.size} activa${if (active.size == 1) "" else "s"}")
                append(" · ")
                append(
                    if (pendingCount == 0) "todas cobradas este mes"
                    else "$pendingCount pendiente${if (pendingCount == 1) "" else "s"} este mes",
                )
            },
            style = PredictaTypography.monoCap.copy(
                color = PredictaColors.cream35,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
            ),
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { sub ->
                SubscriptionRow(sub = sub, today = today)
            }
            // Nudge de zombies (no repetir las ya listadas arriba)
            zombies.filter { z -> rows.none { it.id == z.id } }.take(2).forEach { z ->
                ZombieRow(sub = z, today = today)
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "Gestionar →",
            style = PredictaTypography.small.copy(color = SubscriptionAccent),
            modifier = Modifier.clickable(onClick = onManageClick),
        )
    }
}

@Composable
private fun SubscriptionRow(sub: Subscription, today: LocalDate) {
    val zombie = sub.isZombie(today)
    val charged = sub.isChargedThisMonth()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = if (zombie) "🧟" else "🔁", fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = sub.service,
                style = PredictaTypography.small.copy(color = PredictaColors.cream),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "cobra el ${sub.effectiveBillingDay()}",
                style = PredictaTypography.caption.copy(color = PredictaColors.cream35),
            )
        }
        Text(
            text = if (charged) "✓ cobrada" else "pendiente",
            style = PredictaTypography.caption.copy(
                color = if (charged) PredictaColors.green else PredictaColors.pending,
            ),
        )
    }
}

@Composable
private fun ZombieRow(sub: Subscription, today: LocalDate) {
    val days = sub.daysSinceLastUsed(today)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🧟", fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = sub.service,
                style = PredictaTypography.small.copy(color = PredictaColors.cream),
            )
        }
        Text(
            text = if (days != null) "no la usás hace ${days}d" else "sin uso",
            style = PredictaTypography.caption.copy(color = PredictaColors.coral),
        )
    }
}
