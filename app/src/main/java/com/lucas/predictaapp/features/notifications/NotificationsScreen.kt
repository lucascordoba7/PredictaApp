package com.lucas.predictaapp.features.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.model.DateGroup
import com.lucas.predictaapp.data.model.Notification
import com.lucas.predictaapp.data.model.NotificationAction
import com.lucas.predictaapp.data.repository.NotificationsRepository
import com.lucas.predictaapp.data.repository.SyncManager
import com.lucas.predictaapp.ui.components.EmptyState
import com.lucas.predictaapp.ui.components.GlyphBadge
import com.lucas.predictaapp.ui.components.Kicker
import com.lucas.predictaapp.ui.components.PredictaCard
import com.lucas.predictaapp.ui.components.PredictaPullRefresh
import com.lucas.predictaapp.ui.components.ScreenTopBar
import com.lucas.predictaapp.ui.components.StatusPill
import com.lucas.predictaapp.ui.components.pageWash
import com.lucas.predictaapp.ui.theme.PredictaColors
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
            .pageWash(),
        onRefresh = { SyncManager.pullAll() },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "top") {
                val unread = items.count { it.unread }
                ScreenTopBar(title = "Notificaciones", onBack = onBack, modifier = Modifier.padding(bottom = 8.dp)) {
                    if (unread > 0) StatusPill("$unread sin leer", PredictaColors.accentSoft, PredictaColors.accent)
                }
            }

            if (items.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        emoji = "🔔",
                        title = "Nada por acá",
                        body = "Te aviso cuando venza un pago o se te vaya la mano con una categoría.",
                    )
                }
            }

            val groups = listOf(
                DateGroup.TODAY to "Hoy",
                DateGroup.YESTERDAY to "Ayer",
                DateGroup.OLDER to "Más antiguas",
            )
            groups.forEach { (group, label) ->
                val groupItems = items.filter { it.dateGroup == group }
                if (groupItems.isNotEmpty()) {
                    item(key = "h_$label") {
                        Kicker(label, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
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
private fun NotificationCard(
    notification: Notification,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PredictaCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            GlyphBadge(size = 40.dp, container = if (notification.unread) PredictaColors.accentSoft else PredictaColors.neutralSoft) {
                Text(notification.icon, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.typeLabel,
                        style = PredictaTypography.label.copy(fontSize = 12.sp),
                        color = PredictaColors.kicker,
                        modifier = Modifier.weight(1f),
                    )
                    Text(notification.time, style = PredictaTypography.caption, color = PredictaColors.inkSecondary)
                    if (notification.unread) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(8.dp).background(PredictaColors.accent, CircleShape))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(notification.title, style = PredictaTypography.rowTitle, color = PredictaColors.ink)
                Spacer(Modifier.height(2.dp))
                Text(notification.body, style = PredictaTypography.body.copy(fontSize = 14.sp, lineHeight = 20.sp), color = PredictaColors.inkSecondary)
            }
        }

        if (notification.actions.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    val shape = RoundedCornerShape(14.dp)
    val (bg, fg) = when {
        action.primary -> PredictaColors.accent to PredictaColors.onAccent
        action.danger -> PredictaColors.dangerSoft to PredictaColors.danger
        else -> PredictaColors.card to PredictaColors.ink
    }
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(bg)
            .then(if (!action.primary && !action.danger) Modifier.border(1.dp, PredictaColors.border, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Text(action.label, style = PredictaTypography.label, color = fg)
    }
}
