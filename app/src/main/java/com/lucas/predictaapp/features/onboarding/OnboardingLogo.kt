package com.lucas.predictaapp.features.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lucas.predictaapp.ui.theme.PredictaColors

/**
 * Isometric hex dice logo — faithfully ported from logo-icon.svg.
 * Viewbox 0-48 normalized to [0,1] then scaled to [size].
 */
@Composable
fun PredictaLogoDice(
    size: Dp = 56.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width  // canvas px

        fun n(x: Float) = x / 48f * s  // normalize SVG coord to canvas px

        val amber = PredictaColors.amber
        val amberFill1 = amber.copy(alpha = 0.18f)
        val amberFill2 = amber.copy(alpha = 0.08f)
        val amberFill3 = amber.copy(alpha = 0.04f)
        val amberEdge = amber.copy(alpha = 0.6f)
        val amberInternal = amber.copy(alpha = 0.35f * 0.6f)

        // Helper offsets (from SVG)
        val top    = Offset(n(24f), n(4f))
        val topR   = Offset(n(41.32f), n(14f))
        val botR   = Offset(n(41.32f), n(34f))
        val bot    = Offset(n(24f), n(44f))
        val botL   = Offset(n(6.68f), n(34f))
        val topL   = Offset(n(6.68f), n(14f))
        val center = Offset(n(24f), n(24f))

        // ── Faces ──
        drawFace(listOf(top, topR, center, topL), amberFill1)
        drawFace(listOf(center, topR, botR, bot), amberFill2)
        drawFace(listOf(center, topL, botL, bot), amberFill3)

        // ── Face edges (thin amber) ──
        drawFaceStroke(listOf(top, topR, center, topL), amberEdge, strokeWidth = n(0.6f))
        drawFaceStroke(listOf(center, topR, botR, bot), amberEdge, strokeWidth = n(0.6f))
        drawFaceStroke(listOf(center, topL, botL, bot), amberEdge, strokeWidth = n(0.6f))

        // ── Outer hex ──
        val hexPath = Path().apply {
            moveTo(top.x, top.y)
            lineTo(topR.x, topR.y)
            lineTo(botR.x, botR.y)
            lineTo(bot.x, bot.y)
            lineTo(botL.x, botL.y)
            lineTo(topL.x, topL.y)
            close()
        }
        drawPath(hexPath, amber, style = Stroke(width = n(1.8f)))

        // ── Internal edges ──
        val internalPaint = androidx.compose.ui.graphics.Paint().apply {
            color = amberInternal
            strokeWidth = n(0.6f)
        }
        drawLine(amberInternal, center, topR, strokeWidth = n(0.6f))
        drawLine(amberInternal, center, topL, strokeWidth = n(0.6f))
        drawLine(amberInternal, center, bot, strokeWidth = n(0.6f))

        // ── Pips ──
        val pipR = n(1.8f)
        // Top face: 1 pip
        drawCircle(amber, pipR, Offset(n(24f), n(14f)))
        // Right face: 3 pips
        drawCircle(amber, pipR, Offset(n(36.99f), n(21.5f)))
        drawCircle(amber, pipR, Offset(n(32.66f), n(29f)))
        drawCircle(amber, pipR, Offset(n(28.33f), n(36.5f)))
        // Left face: 2 pips
        drawCircle(amber, pipR, Offset(n(11.01f), n(21.5f)))
        drawCircle(amber, pipR, Offset(n(19.67f), n(36.5f)))
    }
}

private fun DrawScope.drawFace(points: List<Offset>, color: Color) {
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawFaceStroke(points: List<Offset>, color: Color, strokeWidth: Float) {
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, color, style = Stroke(width = strokeWidth))
}

/**
 * Hex-shaped badge with [dotCount] amber dots, matching the design's .hex-badge.
 * Uses the CSS clip-path polygon: 50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%
 */
@Composable
fun HexBadge(
    dotCount: Int,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val amber = PredictaColors.amber

        // Hex outline
        val hexPath = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w * 1.0f, h * 0.25f)
            lineTo(w * 1.0f, h * 0.75f)
            lineTo(w * 0.5f, h * 1.0f)
            lineTo(w * 0.0f, h * 0.75f)
            lineTo(w * 0.0f, h * 0.25f)
            close()
        }
        drawPath(hexPath, amber.copy(alpha = 0.12f))
        drawPath(hexPath, amber.copy(alpha = 0.25f), style = Stroke(width = 1f))

        // Dots centered in hex
        val dotR = w * 0.08f
        val spacing = w * 0.22f
        val startX = w * 0.5f - spacing * (dotCount - 1) / 2f
        val centerY = h * 0.5f
        repeat(dotCount) { i ->
            drawCircle(amber, dotR, Offset(startX + spacing * i, centerY))
        }
    }
}
