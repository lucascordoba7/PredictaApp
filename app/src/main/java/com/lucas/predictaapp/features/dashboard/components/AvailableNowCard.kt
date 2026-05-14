package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs

@Composable
fun AvailableNowCard(
    availableNow: Int,
    daysToPayday: Int,
    dailyBudget: Int,
) {
    val paydayLabel = when {
        daysToPayday == 0 -> "hoy es cobro 🎉"
        daysToPayday == 1 -> "cobro mañana"
        else              -> "cobro en $daysToPayday días"
    }
    val paydayColor = if (daysToPayday <= 3) PredictaColors.pending else PredictaColors.cream35

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatItem(
            value = "$ ${availableNow.fmtArs()}",
            label = "disponible",
            valueColor = androidx.compose.ui.graphics.Color.Unspecified,
            modifier = Modifier.weight(1f),
        )

        Divider()

        StatItem(
            value = "$ ${dailyBudget.fmtArs()}",
            label = "por día",
            valueColor = PredictaColors.amber,
            modifier = Modifier.weight(1f),
        )

        Divider()

        StatItem(
            value = paydayLabel,
            label = null,
            valueColor = paydayColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String?,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = PredictaTypography.bodyTight.copy(
                fontSize = 13.sp,
                letterSpacing = (-0.2).sp,
                fontWeight = FontWeight.SemiBold,
                color = if (valueColor == androidx.compose.ui.graphics.Color.Unspecified)
                    PredictaColors.cream else valueColor,
            ),
            maxLines = 1,
        )
        if (label != null) {
            Text(
                text = label,
                style = PredictaTypography.monoCap.copy(
                    color = PredictaColors.cream35,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                ),
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(PredictaColors.line),
    )
    Spacer(modifier = Modifier.width(4.dp))
}
