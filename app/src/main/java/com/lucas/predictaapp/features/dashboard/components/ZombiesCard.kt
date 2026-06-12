package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.data.model.Subscription
import com.lucas.predictaapp.data.model.daysSinceLastUsed
import com.lucas.predictaapp.ui.theme.IBMPlexMono
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs

@Composable
fun ZombiesCard(
    zombies: List<Subscription>,
    onCancel: () -> Unit,
) {
    val totalSaving = zombies.sumOf { it.monthly }
    val coral = PredictaColors.coral
    val surface = PredictaColors.surface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(14.dp))
            .drawBehind {
                drawRect(surface)
                drawRect(
                    color = coral,
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
                text = "🧟  ${zombies.size} suscripciones inactivas",
                style = PredictaTypography.bodyTight.copy(color = PredictaColors.cream),
            )
            Text(
                text = "−$ ${totalSaving.fmtArs()}/MES",
                style = PredictaTypography.monoCap.copy(
                    color = PredictaColors.coral,
                    letterSpacing = 0.5.sp,
                ),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            zombies.forEach { z ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = z.service,
                            style = PredictaTypography.small.copy(color = PredictaColors.cream),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val days = z.daysSinceLastUsed()
                        Text(
                            text = if (days != null) "hace ${days}d" else "—",
                            style = PredictaTypography.caption.copy(color = PredictaColors.cream35),
                        )
                    }
                    Text(
                        text = "$ ${z.monthly.fmtArs()}",
                        style = PredictaTypography.small.copy(
                            fontFamily = IBMPlexMono,
                            fontSize = 12.sp,
                            color = PredictaColors.cream60,
                        ),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            shape = RoundedCornerShape(9999.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PredictaColors.coral),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = PredictaColors.coral,
            ),
        ) {
            Text(
                text = "Cancelar →",
                style = PredictaTypography.small.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = PredictaColors.coral,
                ),
            )
        }
    }
}
