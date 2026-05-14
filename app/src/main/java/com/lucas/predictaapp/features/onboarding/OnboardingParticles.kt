package com.lucas.predictaapp.features.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

private data class Particle(
    val xFraction: Float,   // 0..1 horizontal position
    val sizePx: Float,      // size in dp units (scaled later)
    val color: Color,
    val speedTier: Int,     // 0=slow, 1=medium, 2=fast
    val phase: Float,       // 0..1 starting y phase
    val rotationDeg: Float,
)

private val particleColors = listOf(
    Color(0xFFF5A524), // amber
    Color(0xFF7BC47A), // green/mint
    Color(0xFFE26B4A), // coral
)

@Composable
fun HexParticles(modifier: Modifier = Modifier) {
    val particles = remember {
        val rng = java.util.Random(42L)
        (0 until 14).map { i ->
            Particle(
                xFraction = rng.nextFloat() * 0.88f + 0.04f,
                sizePx = 6f + rng.nextFloat() * 16f,
                color = particleColors[i % 3],
                speedTier = i % 3,
                phase = rng.nextFloat(),
                rotationDeg = rng.nextFloat() * 360f,
            )
        }
    }

    val infinite = rememberInfiniteTransition(label = "hex_particles")

    // Three speed tiers — each drives a set of particles at a different pace
    val tSlow by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Restart),
        label = "slow",
    )
    val tMed by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
        label = "med",
    )
    val tFast by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart),
        label = "fast",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val density = 3f  // sizePx → actual px scale

        particles.forEach { p ->
            val t = when (p.speedTier) { 1 -> tMed; 2 -> tFast; else -> tSlow }
            // Current progress in [0,1], shifted by phase → wraps
            val progress = (p.phase + t) % 1f
            // Particles float upward: y goes from h (bottom) to -size (above top)
            val yCenter = h * (1f - progress) - p.sizePx * density / 2f
            val xCenter = w * p.xFraction
            val alpha = when {
                progress < 0.1f -> progress / 0.1f * 0.07f
                progress > 0.9f -> (1f - (progress - 0.9f) / 0.1f) * 0.07f
                else -> 0.07f
            }
            if (alpha <= 0f) return@forEach

            val s = p.sizePx * density
            drawHexagon(
                center = Offset(xCenter, yCenter),
                size = s,
                color = p.color.copy(alpha = alpha),
                rotationDeg = p.rotationDeg + progress * 120f,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHexagon(
    center: Offset,
    size: Float,
    color: Color,
    rotationDeg: Float,
) {
    val path = Path()
    val rotRad = Math.toRadians(rotationDeg.toDouble()).toFloat()
    for (i in 0 until 6) {
        val angleDeg = 60f * i - 30f
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat() + rotRad
        val x = center.x + size / 2f * cos(angleRad)
        val y = center.y + size / 2f * sin(angleRad)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
