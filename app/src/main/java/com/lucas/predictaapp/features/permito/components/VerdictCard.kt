package com.lucas.predictaapp.features.permito.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lucas.predictaapp.data.model.BreakdownRow
import com.lucas.predictaapp.data.model.PermitoOutput
import com.lucas.predictaapp.data.model.PermitoVerdict
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs

@Composable
fun VerdictCard(
    result: PermitoOutput.Verdict,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PredictaDimensions.Spacing.screenPadding),
    ) {
        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = PredictaColors.cream60,
            )
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        VerdictHeader(result.verdict, result.suggestedWaitUntil)

        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))

        PurchasePill(item = result.parsedPurchase.item, amount = result.parsedPurchase.amount)

        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        Text(
            text = result.reasoning,
            style = PredictaTypography.body,
            color = PredictaColors.cream60,
        )

        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))

        BreakdownCard(rows = result.breakdown)

        result.alternativeAdvice?.let { advice ->
            Spacer(Modifier.height(PredictaDimensions.Spacing.base))
            AdviceCard(advice)
        }

        if (result.promoStore != null) {
            Spacer(Modifier.height(PredictaDimensions.Spacing.base))
            PromoCard(
                store = result.promoStore,
                category = result.promoCategory,
                fromPrice = result.promoFromPrice,
                originalPrice = result.parsedPurchase.amount,
            )
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        NewQueryButton(onClick = onBack)

        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))
    }
}

@Composable
private fun VerdictHeader(verdict: PermitoVerdict, waitUntil: String?) {
    val (icon, label, color, bgColor) = when (verdict) {
        PermitoVerdict.YES -> VerdictStyle(
            Icons.Filled.CheckCircle, "Sí dale", PredictaColors.green, PredictaColors.greenSoft,
        )
        PermitoVerdict.WAIT -> VerdictStyle(
            Icons.Filled.Schedule, "Esperá", PredictaColors.pending, PredictaColors.pendingSoft,
        )
        PermitoVerdict.NO -> VerdictStyle(
            Icons.Filled.HighlightOff, "Este mes no", PredictaColors.coral, PredictaColors.coralSoft,
        )
    }

    Column(horizontalAlignment = Alignment.Start) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = label,
                style = PredictaTypography.section,
                color = color,
            )
        }

        if (verdict == PermitoVerdict.WAIT && waitUntil != null) {
            Spacer(Modifier.height(PredictaDimensions.Spacing.xs))
            Text(
                text = "hasta el $waitUntil",
                style = PredictaTypography.small,
                color = PredictaColors.cream60,
            )
        }
    }
}

private data class VerdictStyle(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val bgColor: Color,
)

@Composable
private fun PurchasePill(item: String, amount: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
            .background(PredictaColors.surfaceHigh)
            .padding(
                horizontal = PredictaDimensions.Spacing.base,
                vertical = PredictaDimensions.Spacing.sm,
            ),
        horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item,
            style = PredictaTypography.bodyTight,
            color = PredictaColors.cream,
        )
        Text(
            text = "·",
            style = PredictaTypography.body,
            color = PredictaColors.cream35,
        )
        Text(
            text = "\$${amount.fmtArs()}",
            style = PredictaTypography.bodyTight,
            color = PredictaColors.amber,
        )
    }
}

@Composable
private fun BreakdownCard(rows: List<BreakdownRow>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .padding(PredictaDimensions.Spacing.base),
        verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
    ) {
        Text(
            text = "DESGLOSE",
            style = PredictaTypography.monoCap,
            color = PredictaColors.cream35,
        )
        Spacer(Modifier.height(PredictaDimensions.Spacing.xs))

        rows.forEachIndexed { i, row ->
            val isLast = i == rows.lastIndex
            val valueColor = when {
                isLast && row.value >= 0 -> PredictaColors.green
                isLast && row.value < 0 -> PredictaColors.coral
                row.value < 0 -> PredictaColors.cream60
                else -> PredictaColors.cream
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.label,
                    style = if (isLast) PredictaTypography.bodyTight else PredictaTypography.body,
                    color = if (isLast) PredictaColors.cream else PredictaColors.cream60,
                )
                Text(
                    text = "${if (row.value >= 0) "\$" else "-\$"}${Math.abs(row.value).fmtArs()}",
                    style = if (isLast) PredictaTypography.bodyTight else PredictaTypography.body,
                    color = valueColor,
                )
            }

            if (!isLast) {
                Divider(color = PredictaColors.line, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun AdviceCard(advice: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.amberSoft)
            .border(1.dp, PredictaColors.amberEdge, RoundedCornerShape(PredictaDimensions.Radius.card))
            .padding(PredictaDimensions.Spacing.base),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
    ) {
        Text(text = "💡", style = PredictaTypography.body)
        Text(
            text = advice,
            style = PredictaTypography.body,
            color = PredictaColors.cream,
        )
    }
}

@Composable
private fun PromoCard(
    store: String,
    category: String?,
    fromPrice: Int?,
    originalPrice: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(PredictaDimensions.Radius.card))
            .padding(PredictaDimensions.Spacing.base),
        verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "DÓNDE CONSEGUIRLO",
                style = PredictaTypography.monoCap,
                color = PredictaColors.cream35,
            )
            if (fromPrice != null && fromPrice < originalPrice) {
                val savings = ((originalPrice - fromPrice).toFloat() / originalPrice * 100).toInt()
                Text(
                    text = "-$savings%",
                    style = PredictaTypography.monoCap,
                    color = PredictaColors.green,
                    modifier = Modifier
                        .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
                        .background(PredictaColors.greenSoft)
                        .padding(horizontal = PredictaDimensions.Spacing.sm, vertical = 2.dp),
                )
            }
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.xs))

        Text(
            text = store,
            style = PredictaTypography.cardTitle,
            color = PredictaColors.cream,
        )

        if (category != null) {
            Text(
                text = category,
                style = PredictaTypography.small,
                color = PredictaColors.cream60,
            )
        }

        if (fromPrice != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.xs),
            ) {
                Text(
                    text = "desde",
                    style = PredictaTypography.small,
                    color = PredictaColors.cream60,
                )
                Text(
                    text = "\$${fromPrice.fmtArs()}",
                    style = PredictaTypography.bodyTight,
                    color = PredictaColors.amber,
                )
            }
        }
    }
}

@Composable
private fun NewQueryButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
            .background(PredictaColors.surfaceHigh)
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(PredictaDimensions.Radius.pill))
            .clickable(onClick = onClick)
            .padding(vertical = PredictaDimensions.Spacing.base),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Nueva consulta",
            style = PredictaTypography.bodyTight,
            color = PredictaColors.cream,
            textAlign = TextAlign.Center,
        )
    }
}
