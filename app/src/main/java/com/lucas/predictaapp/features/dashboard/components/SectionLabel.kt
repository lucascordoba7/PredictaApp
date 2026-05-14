package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography

@Composable
fun SectionLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = PredictaTypography.monoCap.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.4.sp,
                color = PredictaColors.cream35,
            ),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
        ) {
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0x0FFFFFFF), Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                ),
                start = Offset.Zero,
                end = Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}
