package com.lucas.predictaapp.features.permito.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography

private val examples = listOf(
    "Zapatillas \$75k",
    "Smart TV \$400k",
    "PlayStation 5 \$900k",
    "Cena en Don Julio",
    "Vuelo a Bariloche \$250k",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PermitoInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = PredictaDimensions.Spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 3 }),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "¿Te lo permito?",
                    style = PredictaTypography.titlePage,
                    color = PredictaColors.cream,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(PredictaDimensions.Spacing.sm))
                Text(
                    text = "Contame qué querés comprar\ny te digo si se puede.",
                    style = PredictaTypography.body,
                    color = PredictaColors.cream60,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
        ) {
            Column {
                InputBox(
                    text = text,
                    onTextChange = onTextChange,
                    onSubmit = onSubmit,
                )

                Spacer(Modifier.height(PredictaDimensions.Spacing.md))

                Text(
                    text = "por ejemplo",
                    style = PredictaTypography.monoCap,
                    color = PredictaColors.cream35,
                )

                Spacer(Modifier.height(PredictaDimensions.Spacing.sm))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
                ) {
                    examples.forEach { example ->
                        ExampleChip(
                            label = example,
                            onClick = { onTextChange(example) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InputBox(
    text: String,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .border(
                width = 1.dp,
                color = if (text.isNotBlank()) PredictaColors.amberEdge else PredictaColors.lineStrong,
                shape = RoundedCornerShape(PredictaDimensions.Radius.card),
            )
            .padding(PredictaDimensions.Spacing.base),
    ) {
        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = PredictaTypography.body.copy(color = PredictaColors.cream),
            cursorBrush = SolidColor(PredictaColors.amber),
            minLines = 3,
            maxLines = 6,
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text(
                        text = "Ej: \"quiero comprar unas zapatillas Nike por 75 lucas\"",
                        style = PredictaTypography.body,
                        color = PredictaColors.cream35,
                    )
                }
                inner()
            },
        )

        Spacer(Modifier.height(PredictaDimensions.Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            SubmitButton(
                enabled = text.isNotBlank(),
                onClick = onSubmit,
            )
        }
    }
}

@Composable
private fun SubmitButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (enabled) PredictaColors.amber else PredictaColors.surfaceHigh
    val textColor = if (enabled) PredictaColors.onAmber else PredictaColors.cream35

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
            .background(bgColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = PredictaDimensions.Spacing.base,
                vertical = PredictaDimensions.Spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.xs),
    ) {
        Text(
            text = "Preguntale a Predicta",
            style = PredictaTypography.bodyTight,
            color = textColor,
        )
        Icon(
            imageVector = Icons.Filled.ArrowForward,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun ExampleChip(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = PredictaTypography.small,
        color = PredictaColors.amber,
        modifier = Modifier
            .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
            .border(1.dp, PredictaColors.amberEdge, RoundedCornerShape(PredictaDimensions.Radius.pill))
            .clickable(onClick = onClick)
            .padding(
                horizontal = PredictaDimensions.Spacing.md,
                vertical = PredictaDimensions.Spacing.xs,
            ),
    )
}
