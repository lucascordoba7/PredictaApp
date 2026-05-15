package com.lucas.predictaapp.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.data.model.UserProfile
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography

@Composable
fun ProfileHeader(
    user: UserProfile,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        AvatarCircle(initial = user.name.first().toString())

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.md))

        Text(
            text = user.name,
            style = PredictaTypography.section,
            color = PredictaColors.cream,
        )

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.xs))

        Text(
            text = user.email,
            style = PredictaTypography.caption.copy(color = PredictaColors.cream35),
        )

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.md))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip(label = "Score", value = "${user.score}")
            StatChip(label = "Disponible", value = formatAmount(user.availableNow))
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, PredictaColors.cream.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .background(PredictaColors.cream.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = PredictaTypography.monoCap.copy(
                color = PredictaColors.cream35,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
            ),
        )
        Text(
            text = value,
            style = PredictaTypography.monoCap.copy(
                color = PredictaColors.cream,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
private fun AvatarCircle(
    initial: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(PredictaColors.amberSoft)
            .border(2.dp, PredictaColors.amberEdge, CircleShape),
    ) {
        Text(
            text = initial,
            style = PredictaTypography.scoreHero,
            color = PredictaColors.amber,
        )
    }
}

private fun formatAmount(amount: Int): String =
    if (amount >= 1_000_000) "$${amount / 1_000_000}M"
    else if (amount >= 1_000) "$${amount / 1_000}K"
    else "$$amount"
