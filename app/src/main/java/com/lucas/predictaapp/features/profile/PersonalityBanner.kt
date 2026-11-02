package com.lucas.predictaapp.features.profile

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.data.model.Personality
import com.lucas.predictaapp.ui.components.GlyphBadge
import com.lucas.predictaapp.ui.components.Kicker
import com.lucas.predictaapp.ui.components.PredictaCard
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography

sealed class PersonalityBannerState {
    data class Ready(val personality: Personality) : PersonalityBannerState()
    data class Insufficient(val transactionCount: Int, val requiredCount: Int = 5) : PersonalityBannerState()
    data object Loading : PersonalityBannerState()
}

/**
 * La personalidad de gasto del mes. Es una tarjeta más (no compite con la hero): el color lo
 * pone solo la insignia ámbar con el emoji.
 */
@Composable
fun PersonalityBanner(
    state: PersonalityBannerState,
    modifier: Modifier = Modifier,
    onPress: (() -> Unit)? = null,
) {
    PredictaCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        onClick = onPress,
    ) {
        when (state) {
            is PersonalityBannerState.Ready -> Ready(state.personality)
            is PersonalityBannerState.Insufficient -> Insufficient(state.transactionCount, state.requiredCount)
            PersonalityBannerState.Loading -> Loading()
        }
    }
}

@Composable
private fun Ready(personality: Personality) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GlyphBadge(size = 52.dp, container = PredictaColors.honey) {
            Text(personality.emoji, fontSize = 26.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Kicker("Tu personalidad")
            Text(personality.name, style = PredictaTypography.cardTitle, color = PredictaColors.ink)
            if (personality.weeklySpike.isNotBlank()) {
                Text(
                    text = "${personality.spikeStat} los ${personality.weeklySpike}",
                    style = PredictaTypography.label.copy(fontSize = 13.sp),
                    color = PredictaColors.accent,
                )
            }
        }
    }
    if (personality.description.isNotBlank()) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = personality.description,
            style = PredictaTypography.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = PredictaColors.inkSecondary,
            maxLines = 3,
        )
    }
}

@Composable
private fun Insufficient(count: Int, required: Int) {
    val progress = (count.toFloat() / required).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        GlyphBadge(size = 52.dp, container = PredictaColors.neutralSoft) {
            Text("🔍", fontSize = 24.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Kicker("Tu personalidad")
            Text("Todavía te estoy conociendo", style = PredictaTypography.cardTitle, color = PredictaColors.ink)
            Text(
                text = (required - count).let { n -> if (n == 1) "Falta 1 movimiento este mes" else "Faltan $n movimientos este mes" },
                style = PredictaTypography.rowSubtitle,
                color = PredictaColors.inkSecondary,
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(PredictaColors.neutralSoft),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PredictaColors.primaryBrush),
        )
    }
}

@Composable
private fun Loading() {
    val alpha by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shimmer_alpha",
    )
    val block = PredictaColors.neutralSoft.copy(alpha = alpha)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(52.dp).height(52.dp).clip(RoundedCornerShape(14.dp)).background(block))
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(100.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(block))
            Box(Modifier.width(180.dp).height(18.dp).clip(RoundedCornerShape(6.dp)).background(block))
        }
    }
}
