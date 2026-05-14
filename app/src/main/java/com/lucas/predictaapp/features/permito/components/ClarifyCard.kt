package com.lucas.predictaapp.features.permito.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lucas.predictaapp.data.model.PermitoOutput
import com.lucas.predictaapp.data.model.PermitoSuggestion
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClarifyCard(
    result: PermitoOutput.Clarify,
    onBack: () -> Unit,
    onSuggestionClick: (String) -> Unit,
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

        Text(
            text = "Casi, pero...",
            style = PredictaTypography.section,
            color = PredictaColors.cream,
        )

        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))

        EchoBox(userEcho = result.userEcho)

        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        Text(
            text = result.reason,
            style = PredictaTypography.small,
            color = PredictaColors.cream60,
        )

        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        Text(
            text = result.question,
            style = PredictaTypography.cardTitle,
            color = PredictaColors.cream,
        )

        if (result.suggestions.isNotEmpty()) {
            Spacer(Modifier.height(PredictaDimensions.Spacing.base))

            Text(
                text = "OPCIONES",
                style = PredictaTypography.monoCap,
                color = PredictaColors.cream35,
            )

            Spacer(Modifier.height(PredictaDimensions.Spacing.sm))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
            ) {
                result.suggestions.forEach { suggestion ->
                    SuggestionChip(
                        suggestion = suggestion,
                        onClick = { onSuggestionClick(suggestion.rewrittenIntent) },
                    )
                }
            }
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        RewriteButton(onClick = onBack)

        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))
    }
}

@Composable
private fun EchoBox(userEcho: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.cream12)
            .padding(PredictaDimensions.Spacing.base),
    ) {
        Text(
            text = "\"$userEcho\"",
            style = PredictaTypography.body,
            color = PredictaColors.cream60,
        )
    }
}

@Composable
private fun SuggestionChip(
    suggestion: PermitoSuggestion,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
            .border(1.dp, PredictaColors.amberEdge, RoundedCornerShape(PredictaDimensions.Radius.pill))
            .background(PredictaColors.amberSoft)
            .clickable(onClick = onClick)
            .padding(
                horizontal = PredictaDimensions.Spacing.base,
                vertical = PredictaDimensions.Spacing.sm,
            ),
        horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = suggestion.label,
            style = PredictaTypography.bodyTight,
            color = PredictaColors.amber,
        )
    }
}

@Composable
private fun RewriteButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(PredictaDimensions.Radius.pill))
            .clickable(onClick = onClick)
            .padding(vertical = PredictaDimensions.Spacing.base),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Edit,
            contentDescription = null,
            tint = PredictaColors.cream60,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.size(PredictaDimensions.Spacing.sm))
        Text(
            text = "Reescribir consulta",
            style = PredictaTypography.bodyTight,
            color = PredictaColors.cream,
            textAlign = TextAlign.Center,
        )
    }
}
