package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.lucas.predictaapp.data.model.GoalMember
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs

@Composable
fun GoalCard(
    emoji: String,
    title: String,
    members: List<GoalMember>,
    membersLabel: String,
    current: Int,
    target: Int,
    daysLeft: Int,
) {
    val pct = (current.toFloat() / target).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, PredictaColors.line, RoundedCornerShape(16.dp))
            .background(PredictaColors.surface)
            .padding(18.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = emoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = PredictaTypography.bodyTight.copy(
                            fontSize = 14.sp,
                            color = PredictaColors.cream,
                            letterSpacing = (-0.1).sp,
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = membersLabel.uppercase(),
                    style = PredictaTypography.monoCap.copy(
                        color = PredictaColors.cream60,
                        letterSpacing = 1.2.sp,
                    ),
                )
            }
            MemberAvatars(members = members)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(PredictaColors.surfaceHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(6.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PredictaColors.amber, PredictaColors.amber.copy(alpha = 0.7f)),
                        ),
                    ),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = buildString {
                    append("$ ${current.fmtArs()}")
                    append(" / ")
                    append(target.fmtArs())
                },
                style = PredictaTypography.monoCap.copy(
                    color = PredictaColors.cream60,
                    fontSize = 12.sp,
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(pct * 100).toInt()}%",
                style = PredictaTypography.monoCap.copy(
                    color = PredictaColors.amber,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$daysLeft días",
                style = PredictaTypography.monoCap.copy(
                    color = PredictaColors.cream60,
                    fontSize = 12.sp,
                ),
            )
        }
    }
}

@Composable
private fun MemberAvatars(members: List<GoalMember>) {
    Box {
        members.forEachIndexed { i, member ->
            val color = if (member.color == "amber") PredictaColors.amber else PredictaColors.green
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = (i * 16).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(2.dp, PredictaColors.surface, CircleShape)
                    .background(color),
            ) {
                Text(
                    text = member.initial,
                    style = PredictaTypography.monoCap.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        color = PredictaColors.onAmber,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.width((members.size * 16 + 24).dp))
    }
}
