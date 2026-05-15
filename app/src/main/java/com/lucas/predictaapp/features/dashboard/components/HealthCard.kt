package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography

private val monthsEs = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
private fun previousMonthName(): String {
    val prevIndex = (java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) - 1 + 12) % 12
    return monthsEs[prevIndex]
}

private val sparklinePoints = listOf(
    Offset(0f, 26f), Offset(28f, 22f), Offset(56f, 28f), Offset(84f, 16f),
    Offset(112f, 20f), Offset(140f, 12f), Offset(168f, 15f), Offset(196f, 10f),
    Offset(224f, 8f), Offset(280f, 4f),
)

@Composable
fun HealthCard(score: Int, delta: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, PredictaColors.line, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PREDICTA SCORE",
                    style = PredictaTypography.monoCap.copy(
                        letterSpacing = 1.2.sp,
                        color = PredictaColors.cream35,
                    ),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = score.toString(),
                        style = PredictaTypography.scoreHero.copy(
                            fontSize = 52.sp,
                            color = PredictaColors.cream,
                            letterSpacing = (-1).sp,
                        ),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "/ 100",
                        style = PredictaTypography.monoCap.copy(
                            fontSize = 16.sp,
                            color = PredictaColors.cream35,
                        ),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    DeltaBadge(delta = delta)
                }
            }
            ScoreRing(score = score, size = 64.dp, strokeWidth = 6.dp)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Sparkline()
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Sumá 3 acciones esta semana y subís a ${score + delta}.",
            style = PredictaTypography.small.copy(
                color = PredictaColors.cream60,
                lineHeight = 20.sp,
            ),
        )
    }
}

@Composable
private fun DeltaBadge(delta: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(0.dp, Color.Transparent, RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(PredictaColors.greenSoft)
                .padding(horizontal = 7.dp, vertical = 3.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = PredictaColors.green,
                    modifier = Modifier.size(10.dp),
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "+$delta vs ${previousMonthName()}",
                    style = PredictaTypography.monoCap.copy(
                        color = PredictaColors.green,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                    ),
                )
            }
        }
    }
}

@Composable
fun ScoreRing(score: Int, size: Dp = 128.dp, strokeWidth: Dp = 10.dp, showLabel: Boolean = false) {
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "score_ring",
    )
    LaunchedEffect(score) { targetProgress = score / 100f }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val diameter = this.size.width - strokePx
            val topLeft = Offset(strokePx / 2, strokePx / 2)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = Color(0x1FFFFFFF),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            drawArc(
                color = PredictaColors.amber,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
        if (showLabel) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = score.toString(),
                    style = PredictaTypography.scoreHero.copy(
                        fontSize = 36.sp,
                        color = PredictaColors.cream,
                        letterSpacing = (-0.4).sp,
                    ),
                )
                Text(
                    text = "/100",
                    style = PredictaTypography.monoCap.copy(
                        fontSize = 11.sp,
                        color = PredictaColors.cream60,
                        letterSpacing = 2.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun Sparkline() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
    ) {
        val scaleX = size.width / 280f
        val scaleY = size.height / 32f
        val path = Path()
        sparklinePoints.forEachIndexed { i, pt ->
            val x = pt.x * scaleX
            val y = pt.y * scaleY
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = PredictaColors.amber,
            style = Stroke(
                width = 1.6.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
