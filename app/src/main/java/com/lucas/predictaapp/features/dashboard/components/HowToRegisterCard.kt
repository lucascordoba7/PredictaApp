package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography

private data class RegisterWay(val emoji: String, val title: String, val hint: String)

private val ways = listOf(
    RegisterWay("💬", "Chat", "escribí \"café 1.200\" y confirmá"),
    RegisterWay("🎙️", "Voz", "tocá el mic del chat y dictalo"),
    RegisterWay("⚡", "Carga rápida", "botón ＋ → Carga rápida, sin IA"),
    RegisterWay("📷", "Ticket", "sacale foto desde el chat"),
    RegisterWay("📤", "Compartir", "mandá un texto o captura desde otra app"),
    RegisterWay("📱", "Widget", "agregalo a tu pantalla de inicio"),
)

/**
 * Tutorial in-app de las formas de registrar un gasto. Siempre visible por ahora
 * (decisión explícita mientras las features son nuevas); cuando el hábito esté
 * instalado se puede volver descartable.
 */
@Composable
fun HowToRegisterCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(PredictaDimensions.Radius.card))
            .padding(PredictaDimensions.Spacing.base),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "CÓMO REGISTRAR GASTOS",
            style = PredictaTypography.monoCap.copy(fontSize = 9.sp, letterSpacing = 1.sp),
            color = PredictaColors.cream35,
        )
        ways.forEach { way ->
            Row(verticalAlignment = Alignment.Top) {
                Text(text = way.emoji, fontSize = 14.sp)
                Text(
                    text = way.title,
                    style = PredictaTypography.small.copy(color = PredictaColors.cream),
                    modifier = Modifier.padding(start = 10.dp, end = 6.dp),
                )
                Text(
                    text = "· ${way.hint}",
                    style = PredictaTypography.small.copy(color = PredictaColors.cream60),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
