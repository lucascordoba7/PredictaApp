package com.lucas.predictaapp.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.data.model.Personality
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography

sealed class PersonalityBannerState {
    data class Ready(val personality: Personality) : PersonalityBannerState()
    data class Insufficient(val transactionCount: Int, val requiredCount: Int = 10) : PersonalityBannerState()
}

@Composable
fun PersonalityBanner(
    state: PersonalityBannerState,
    onPress: (() -> Unit)? = null,
) {
    when (state) {
        is PersonalityBannerState.Ready -> ReadyBanner(state.personality, onPress)
        is PersonalityBannerState.Insufficient -> InsufficientBanner(state.transactionCount, state.requiredCount)
    }
}

@Composable
private fun ReadyBanner(personality: Personality, onPress: (() -> Unit)?) {
    val accent = personality.accentColorKey.toAccentColor()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to accent.copy(alpha = 0.22f),
                        0.55f to accent.copy(alpha = 0.08f),
                        1.0f to PredictaColors.charcoal.copy(alpha = 0.60f),
                    ),
                ),
            )
            .then(if (onPress != null) Modifier.clickable { onPress() } else Modifier),
    ) {
        // Watermark: emoji decorativo en el fondo
        Text(
            text = personality.emoji,
            fontSize = 110.sp,
            lineHeight = 116.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 16.dp, y = (-4).dp)
                .rotate(15f)
                .alpha(0.09f),
        )

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "TU PERSONALIDAD",
                style = PredictaTypography.monoCap.copy(
                    color = accent,
                    fontSize = 9.5.sp,
                    letterSpacing = 2.4.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = personality.name,
                style = PredictaTypography.section.copy(
                    color = PredictaColors.cream,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 26.sp,
                ),
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = personality.description,
                style = PredictaTypography.body.copy(
                    color = PredictaColors.cream60,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                ),
                maxLines = 3,
            )

            Spacer(modifier = Modifier.height(18.dp))

            HorizontalDivider(color = PredictaColors.cream.copy(alpha = 0.08f), thickness = 1.dp)

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "↑ ${personality.spikeStat} los ${personality.weeklySpike}",
                        style = PredictaTypography.monoCap.copy(
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (onPress != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ver historia",
                            style = PredictaTypography.monoCap.copy(
                                color = PredictaColors.cream35,
                                fontSize = 11.sp,
                            ),
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = PredictaColors.cream35,
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .height(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsufficientBanner(transactionCount: Int, requiredCount: Int) {
    val progress = (transactionCount.toFloat() / requiredCount).coerceIn(0f, 1f)
    val remaining = requiredCount - transactionCount

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, PredictaColors.cream.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to PredictaColors.cream.copy(alpha = 0.04f),
                        1.0f to PredictaColors.charcoal,
                    ),
                ),
            ),
    ) {
        Text(
            text = "🔍",
            fontSize = 100.sp,
            lineHeight = 108.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 14.dp, y = (-4).dp)
                .rotate(10f)
                .alpha(0.08f),
        )

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "TU PERSONALIDAD",
                style = PredictaTypography.monoCap.copy(
                    color = PredictaColors.cream35,
                    fontSize = 9.5.sp,
                    letterSpacing = 2.4.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Todavía estamos aprendiendo",
                style = PredictaTypography.section.copy(
                    color = PredictaColors.cream60,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    lineHeight = 24.sp,
                ),
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Registrá más gastos y Predicta irá descubriendo tu perfil financiero único.",
                style = PredictaTypography.body.copy(
                    color = PredictaColors.cream35,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                ),
                maxLines = 3,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PredictaColors.cream.copy(alpha = 0.10f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    PredictaColors.cream35,
                                    PredictaColors.cream60,
                                ),
                            ),
                        ),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$transactionCount de $requiredCount",
                    style = PredictaTypography.caption.copy(color = PredictaColors.cream35),
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Faltan $remaining más",
                    style = PredictaTypography.caption.copy(
                        color = PredictaColors.cream60,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

private fun String.toAccentColor(): Color = when (this) {
    "coral"  -> PredictaColors.coral
    "green"  -> PredictaColors.green
    "blue"   -> PredictaColors.blue
    "purple" -> PredictaColors.purple
    "teal"   -> PredictaColors.teal
    "pink"   -> PredictaColors.pink
    else     -> PredictaColors.amber
}
