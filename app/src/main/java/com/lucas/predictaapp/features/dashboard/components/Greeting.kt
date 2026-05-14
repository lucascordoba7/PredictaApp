package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.formatGreetingEyebrow
import com.lucas.predictaapp.ui.utils.formatMonthYear

@Composable
fun DashboardHeader(
    name: String,
    score: Int,
    onScoreClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = PredictaColors.cream60)) { append("Hola, ") }
                    withStyle(SpanStyle(color = PredictaColors.amber, fontWeight = FontWeight.Bold)) {
                        append(name)
                    }
                },
                style = PredictaTypography.titlePage.copy(
                    fontSize = 30.sp,
                    letterSpacing = (-0.8).sp,
                ),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = formatMonthYear().lowercase().replaceFirstChar { it.uppercase() },
                style = PredictaTypography.monoCap.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.4.sp,
                    color = PredictaColors.cream35,
                    fontSize = 9.5.sp,
                ),
            )
        }
        ScoreWidget(score = score, onClick = onScoreClick)
    }
}

@Composable
private fun ScoreWidget(score: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = "PREDICTA SCORE",
            style = PredictaTypography.monoCap.copy(
                color = PredictaColors.amber,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(contentAlignment = Alignment.Center) {
            ScoreRing(score = score, size = 54.dp, strokeWidth = 4.5.dp, showLabel = false)
            Text(
                text = score.toString(),
                style = PredictaTypography.bodyTight.copy(
                    fontSize = 17.sp,
                    color = PredictaColors.cream,
                    letterSpacing = (-0.5).sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}
