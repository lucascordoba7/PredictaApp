package com.lucas.predictaapp.features.permito.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import kotlinx.coroutines.delay

private val checklistItems = listOf(
    "Verificando disponible",
    "Revisando fijos pendientes",
    "Chequeando objetivos",
    "Analizando patrones",
)

@Composable
fun PermitoLoading(modifier: Modifier = Modifier) {
    var checkedCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        checklistItems.indices.forEach { i ->
            delay(600L + i * 500L)
            checkedCount = i + 1
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PredictaDimensions.Spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(PredictaDimensions.Spacing.xxl))

        PulsingOrb()

        Spacer(Modifier.height(PredictaDimensions.Spacing.lg))

        Text(
            text = "Predicta está analizando...",
            style = PredictaTypography.cardTitle,
            color = PredictaColors.cream,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        checklistItems.forEachIndexed { i, label ->
            ChecklistRow(
                label = label,
                checked = i < checkedCount,
                active = i == checkedCount,
            )
            if (i < checklistItems.lastIndex) {
                Spacer(Modifier.height(PredictaDimensions.Spacing.md))
            }
        }
    }
}

@Composable
private fun PulsingOrb() {
    val infinite = rememberInfiniteTransition(label = "orb")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    val alpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .scale(scale)
                .background(
                    color = PredictaColors.amber.copy(alpha = alpha * 0.35f),
                    shape = CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(PredictaColors.amber, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "P",
                style = PredictaTypography.cardTitle,
                color = PredictaColors.onAmber,
            )
        }
    }
}

@Composable
private fun ChecklistRow(
    label: String,
    checked: Boolean,
    active: Boolean,
) {
    val iconColor = when {
        checked -> PredictaColors.green
        active -> PredictaColors.amber
        else -> PredictaColors.cream35
    }
    val textColor = when {
        checked -> PredictaColors.cream60
        active -> PredictaColors.cream
        else -> PredictaColors.cream35
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = if (checked) PredictaColors.greenSoft else Color.Transparent,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = PredictaColors.green,
                    modifier = Modifier.size(13.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(iconColor, CircleShape),
                )
            }
        }

        Spacer(Modifier.size(PredictaDimensions.Spacing.md))

        Text(
            text = label,
            style = PredictaTypography.body,
            color = textColor,
        )
    }
}
