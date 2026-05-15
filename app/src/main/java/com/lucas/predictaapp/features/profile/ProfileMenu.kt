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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    val disabled: Boolean = false,
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
            .alpha(if (item.disabled) 0.45f else 1f)
            .clickable(enabled = !item.disabled, onClick = onClick)
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
        if (item.disabled) {
            SoonBadge()
        } else {
            if (item.badge != null) BadgeCount(item.badge)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = PredictaColors.cream12,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SoonBadge() {
    Text(
        text = "PRONTO",
        style = PredictaTypography.monoCap,
        color = PredictaColors.cream35,
        modifier = Modifier
            .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
            .background(PredictaColors.cream.copy(alpha = 0.06f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
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
    HorizontalDivider(
        modifier = Modifier.padding(start = 54.dp),
        color = PredictaColors.line,
        thickness = 0.5.dp,
    )
}
