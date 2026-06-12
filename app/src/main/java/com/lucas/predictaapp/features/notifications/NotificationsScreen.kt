package com.lucas.predictaapp.features.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.model.DateGroup
import com.lucas.predictaapp.data.model.Notification
import com.lucas.predictaapp.data.model.NotificationAction
import com.lucas.predictaapp.data.repository.NotificationsRepository
import com.lucas.predictaapp.data.repository.SyncManager
import com.lucas.predictaapp.ui.components.PredictaPullRefresh
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val items by NotificationsRepository.notifications.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()

    fun dismiss(id: String) = scope.launch { NotificationsRepository.dismiss(id) }

    PredictaPullRefresh(
        modifier = Modifier
            .fillMaxSize()
            .background(PredictaColors.charcoal),
        onRefresh = { SyncManager.pullAll() },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(PredictaColors.charcoal),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = PredictaDimensions.Spacing.xxl,
            ),
        ) {
            item {
                TopBar(unreadCount = items.count { it.unread }, onBack = onBack)
            }

            val groups = listOf(
                DateGroup.TODAY to "Hoy",
                DateGroup.YESTERDAY to "Ayer",
                DateGroup.OLDER to "Más antiguas",
            )
            groups.forEach { (group, label) ->
                val groupItems = items.filter { it.dateGroup == group }
                if (groupItems.isNotEmpty()) {
                    item {
                        SectionHeader(label)
                    }
                    items(groupItems, key = { it.id }) { notif ->
                        NotificationCard(
                            notification = notif,
                            onDismiss = { dismiss(notif.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(unreadCount: Int, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = PredictaDimensions.Spacing.xs,
                end = PredictaDimensions.Spacing.screenPadding,
                top = PredictaDimensions.Spacing.base,
                bottom = PredictaDimensions.Spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PredictaColors.cream60)
        }
        Text(
            text = "Notificaciones",
            style = PredictaTypography.cardTitle,
            color = PredictaColors.cream,
            modifier = Modifier.weight(1f),
        )
        if (unreadCount > 0) {
            Text(
                text = "$unreadCount sin leer",
                style = PredictaTypography.caption,
                color = PredictaColors.amber,
                modifier = Modifier
                    .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
                    .background(PredictaColors.amberSoft)
                    .padding(horizontal = PredictaDimensions.Spacing.sm, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = PredictaTypography.monoCap,
        color = PredictaColors.cream35,
        modifier = Modifier.padding(
            horizontal = PredictaDimensions.Spacing.screenPadding,
            vertical = PredictaDimensions.Spacing.sm,
        ),
    )
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (notification.unread) PredictaColors.amberEdge else PredictaColors.line

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = PredictaDimensions.Spacing.screenPadding,
                vertical = PredictaDimensions.Spacing.xs,
            )
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .border(1.dp, borderColor, RoundedCornerShape(PredictaDimensions.Radius.card))
            .padding(PredictaDimensions.Spacing.base),
        verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PredictaColors.surfaceHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(notification.icon, style = PredictaTypography.body)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = notification.typeLabel,
                        style = PredictaTypography.caption,
                        color = PredictaColors.cream35,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.xs),
                    ) {
                        Text(
                            text = notification.time,
                            style = PredictaTypography.caption,
                            color = PredictaColors.cream35,
                        )
                        if (notification.unread) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(PredictaColors.amber, CircleShape),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = notification.title,
                    style = PredictaTypography.bodyTight,
                    color = PredictaColors.cream,
                )
                Text(
                    text = notification.body,
                    style = PredictaTypography.small,
                    color = PredictaColors.cream60,
                )
            }
        }

        if (notification.actions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
            ) {
                notification.actions.forEach { action ->
                    ActionButton(
                        action = action,
                        onDismiss = { onDismiss(notification.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    action: NotificationAction,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = when {
        action.primary -> PredictaColors.amberSoft
        action.danger -> PredictaColors.coralSoft
        else -> PredictaColors.surfaceHigh
    }
    val textColor = when {
        action.primary -> PredictaColors.amber
        action.danger -> PredictaColors.coral
        else -> PredictaColors.cream60
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PredictaDimensions.Radius.sm))
            .background(bgColor)
            .clickable(onClick = onDismiss)
            .padding(vertical = PredictaDimensions.Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = action.label,
            style = PredictaTypography.small,
            color = textColor,
        )
    }
}
