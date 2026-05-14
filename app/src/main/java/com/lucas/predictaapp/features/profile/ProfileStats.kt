package com.lucas.predictaapp.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lucas.predictaapp.data.model.UserProfile
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography

@Composable
fun ProfileStats(
    user: UserProfile,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
        modifier = modifier.fillMaxWidth(),
    ) {
        StatCard(
            label = "Ingreso",
            value = "$${formatAmount(user.monthIncome)}",
            valueColor = PredictaColors.green,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Gastado",
            value = "$${formatAmount(user.monthSpent)}",
            valueColor = PredictaColors.coral,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Disponible",
            value = "$${formatAmount(user.availableNow)}",
            valueColor = PredictaColors.amber,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surfaceHigh)
            .padding(vertical = PredictaDimensions.Spacing.md),
    ) {
        Text(
            text = value,
            style = PredictaTypography.bodyTight,
            color = valueColor,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = PredictaTypography.caption,
            color = PredictaColors.cream35,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatAmount(amount: Int): String {
    return amount.toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
}
