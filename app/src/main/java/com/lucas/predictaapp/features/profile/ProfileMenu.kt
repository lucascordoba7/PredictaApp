package com.lucas.predictaapp.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography

data class MenuItem(
    val icon: ImageVector,
    val label: String,
    val subtitle: String? = null,
    val badge: String? = null,
    val route: String? = null,
)

@Composable
fun ProfileMenuSection(
    title: String,
    items: List<MenuItem>,
    onItemClick: (MenuItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = PredictaTypography.small,
            color = PredictaColors.cream35,
            modifier = Modifier.padding(bottom = PredictaDimensions.Spacing.sm),
        )
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
                .background(PredictaColors.surfaceHigh),
        ) {
            items.forEachIndexed { index, item ->
                MenuItemRow(
                    item = item,
                    onClick = { onItemClick(item) },
                )
                if (index < items.lastIndex) {
                    MenuDivider()
                }
            }
        }
    }
}

@Composable
private fun MenuItemRow(
    item: MenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = PredictaDimensions.Spacing.base, vertical = 14.dp),
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = PredictaColors.cream60,
            modifier = Modifier.size(22.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = PredictaDimensions.Spacing.md),
        ) {
            Text(
                text = item.label,
                style = PredictaTypography.body,
                color = PredictaColors.cream,
            )
            item.subtitle?.let {
                Text(
                    text = it,
                    style = PredictaTypography.caption,
                    color = PredictaColors.cream35,
                )
            }
        }
        if (item.badge != null) {
            BadgeCount(item.badge)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PredictaColors.cream12,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun BadgeCount(
    count: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = count,
        style = PredictaTypography.caption,
        color = PredictaColors.amber,
        modifier = modifier
            .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
            .background(PredictaColors.amberSoft)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun MenuDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 54.dp),
    ) {
        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier
                .weight(1f)
                .background(PredictaColors.line),
        )
    }
}
