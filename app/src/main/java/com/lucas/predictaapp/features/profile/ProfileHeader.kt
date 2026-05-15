package com.lucas.predictaapp.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
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
        AvatarCircle(
            initials = user.name
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercaseChar().toString() },
        )

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
    }
}

@Composable
private fun AvatarCircle(
    initials: String,
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
            text = initials,
            style = PredictaTypography.scoreHero,
            color = PredictaColors.amber,
        )
    }
}
