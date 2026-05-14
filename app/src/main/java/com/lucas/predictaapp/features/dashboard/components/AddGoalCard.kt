package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(PredictaColors.surfaceMid, PredictaColors.surface)))
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(16.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🎯", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Agregar una meta",
                    style = PredictaTypography.bodyTight.copy(
                        fontSize = 15.sp,
                        color = PredictaColors.cream,
                        letterSpacing = (-0.2).sp,
                    ),
                )
                Text(
                    text = "Ahorrá con un objetivo claro",
                    style = PredictaTypography.monoCap.copy(
                        color = PredictaColors.cream60,
                        fontSize = 11.sp,
                        letterSpacing = 0.4.sp,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GoalOptionButton(
                emoji = "👤",
                label = "Solo",
                modifier = Modifier.weight(1f),
                onClick = onAddSolo,
            )
            GoalOptionButton(
                emoji = "👥",
                label = "Con amigos",
                modifier = Modifier.weight(1f),
                onClick = onAddWithFriends,
            )
        }
    }
}

@Composable
private fun GoalOptionButton(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, PredictaColors.amber.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            .background(PredictaColors.amber.copy(alpha = 0.07f))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = emoji, fontSize = 15.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = PredictaTypography.bodyTight.copy(
                    fontSize = 13.sp,
                    color = PredictaColors.amber,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}
