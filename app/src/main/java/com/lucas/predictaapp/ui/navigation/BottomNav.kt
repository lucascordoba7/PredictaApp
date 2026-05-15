package com.lucas.predictaapp.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.IBMPlexMono
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onFabClick: () -> Unit = {},
    profileScore: Int = 0,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PredictaDimensions.Heights.tabBar)
            .background(PredictaColors.surface),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem(
            icon = Icons.Outlined.Home,
            activeIcon = Icons.Default.Home,
            label = Screen.Dashboard.title,
            isSelected = currentRoute == Screen.Dashboard.route,
            onClick = { onNavigate(Screen.Dashboard.route) },
        )

        NavItem(
            icon = Icons.Outlined.QuestionAnswer,
            activeIcon = Icons.Default.QuestionAnswer,
            label = Screen.Permito.title,
            isSelected = currentRoute == Screen.Permito.route,
            onClick = { onNavigate(Screen.Permito.route) },
        )

        FabButton(onClick = onFabClick)

        NavItem(
            icon = Icons.Outlined.ChatBubbleOutline,
            activeIcon = Icons.Default.ChatBubble,
            label = Screen.Chat.title,
            isSelected = currentRoute == Screen.Chat.route,
            onClick = { onNavigate(Screen.Chat.route) },
        )

        ProfileNavItem(
            isSelected = currentRoute == Screen.Profile.route,
            score = profileScore,
            onClick = { onNavigate(Screen.Profile.route) },
        )
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    activeIcon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        label = "nav_scale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = if (isSelected) activeIcon else icon,
            contentDescription = label,
            tint = if (isSelected) PredictaColors.amber else PredictaColors.cream35,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = PredictaTypography.small,
            color = if (isSelected) PredictaColors.amber else PredictaColors.cream35,
        )
    }
}

@Composable
private fun ProfileNavItem(
    isSelected: Boolean,
    score: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Box {
            Icon(
                imageVector = if (isSelected) Icons.Default.Person else Icons.Outlined.PersonOutline,
                contentDescription = Screen.Profile.title,
                tint = if (isSelected) PredictaColors.amber else PredictaColors.cream35,
                modifier = Modifier.size(24.dp),
            )
            if (score > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-2).dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(PredictaColors.amber)
                        .border(1.5.dp, PredictaColors.surface, RoundedCornerShape(9999.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = score.toString(),
                        fontFamily = IBMPlexMono,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PredictaColors.charcoal,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = Screen.Profile.title,
            style = PredictaTypography.small,
            color = if (isSelected) PredictaColors.amber else PredictaColors.cream35,
        )
    }
}

@Composable
private fun FabButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(PredictaDimensions.Radius.lg))
            .background(PredictaColors.amber)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Acciones rápidas",
            tint = PredictaColors.onAmber,
            modifier = Modifier.size(28.dp),
        )
    }
}
