package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography

@Composable
fun AddGoalCard(
    onAddSolo: () -> Unit = {},
    onAddWithFriends: () -> Unit = {},
) {
    val stripeColor = PredictaColors.charcoal
    val bgColor = PredictaColors.surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .drawBehind {
                // Fill background
                drawRect(bgColor)
                // Draw diagonal stripes (135°)
                val spacing = 12.dp.toPx()
                val strokeWidth = 1.5.dp.toPx()
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(
                        color = stripeColor,
                        start = Offset(x, size.height),
                        end = Offset(x + size.height, 0f),
                        strokeWidth = strokeWidth,
                    )
                    x += spacing
                }
            }
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .drawBehind { drawRect(PredictaColors.surfaceHigh) }
                    .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = PredictaColors.amber,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Empezá tu primera meta",
                    style = PredictaTypography.bodyTight.copy(
                        fontSize = 15.sp,
                        color = PredictaColors.cream,
                        letterSpacing = (-0.2).sp,
                    ),
                )
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = "Predicta sugiere monto y plazo según tu flujo",
                    style = PredictaTypography.caption.copy(
                        color = PredictaColors.cream60,
                    ),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "→",
                color = PredictaColors.amber,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
