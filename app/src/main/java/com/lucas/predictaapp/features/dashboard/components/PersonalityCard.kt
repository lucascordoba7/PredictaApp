package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography

@Composable
fun PersonalityCard(
    emoji: String,
    name: String,
    stat: String,
    cta: String = "ver historia",
    onPress: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, PredictaColors.amber.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.0f to PredictaColors.amber.copy(alpha = 0.18f),
                        0.7f to PredictaColors.coral.copy(alpha = 0.10f),
                        1.0f to PredictaColors.charcoal.copy(alpha = 0.55f),
                    ),
                ),
            )
            .then(if (onPress != null) Modifier.clickable { onPress() } else Modifier)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PredictaColors.amber.copy(alpha = 0.18f)),
            ) {
                Text(text = emoji, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TU PERSONALIDAD",
                    style = PredictaTypography.monoCap.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.4.sp,
                        color = PredictaColors.amber,
                        fontSize = 9.5.sp,
                    ),
                )
                Text(
                    text = name,
                    style = PredictaTypography.bodyTight.copy(
                        fontSize = 15.sp,
                        color = PredictaColors.cream,
                        letterSpacing = (-0.2).sp,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = PredictaColors.amber, fontWeight = FontWeight.SemiBold)) {
                            append(stat)
                        }
                        append(" · $cta")
                    },
                    style = PredictaTypography.monoCap.copy(
                        fontSize = 11.5.sp,
                        color = PredictaColors.cream60,
                    ),
                    maxLines = 1,
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = PredictaColors.cream35,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
