package com.lucas.predictaapp.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.components.HeroCard
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs

/** Avatar con iniciales, nombre y mail. */
@Composable
fun ProfileHeader(name: String, email: String, modifier: Modifier = Modifier) {
    val initials = name.split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(PredictaColors.primaryBrush)
                .border(3.dp, PredictaColors.card, CircleShape),
        ) {
            Text(initials, style = PredictaTypography.title.copy(fontSize = 26.sp), color = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = name.ifBlank { "Tu perfil" },
                style = PredictaTypography.title.copy(fontSize = 22.sp),
                color = PredictaColors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (email.isNotBlank()) {
                Text(email, style = PredictaTypography.body.copy(fontSize = 14.sp), color = PredictaColors.kicker, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** El mes en tres números sobre la hero: lo que entra, lo que salió y lo que queda. */
@Composable
fun ProfileMonthCard(
    monthLabel: String,
    income: Int,
    spent: Int,
    available: Int,
    modifier: Modifier = Modifier,
) {
    HeroCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
        Text(monthLabel, style = PredictaTypography.label.copy(fontSize = 13.sp), color = Color.White.copy(alpha = 0.85f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MonthFigure("Ingreso", income, Modifier.weight(1f))
            MonthFigure("Gastado", spent, Modifier.weight(1f))
            MonthFigure("Disponible", available, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MonthFigure(label: String, amount: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = PredictaTypography.label.copy(fontSize = 12.sp), color = Color.White.copy(alpha = 0.85f))
        Text(
            text = "$${amount.fmtArs()}",
            style = PredictaTypography.rowAmount.copy(fontSize = 16.sp),
            color = Color.White,
            maxLines = 1,
            softWrap = false,
        )
    }
}
