package com.lucas.predictaapp.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.model.Fixtures
import com.lucas.predictaapp.data.repository.NotificationsRepository
import com.lucas.predictaapp.data.repository.SubscriptionsRepository
import com.lucas.predictaapp.features.dashboard.components.HealthCard
import com.lucas.predictaapp.features.dashboard.components.PersonalityCard
import com.lucas.predictaapp.ui.navigation.Screen
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val user = Fixtures.user
    val subscriptions by SubscriptionsRepository.subscriptions.collectAsStateWithLifecycle(emptyList())
    val notifications by NotificationsRepository.notifications.collectAsStateWithLifecycle(emptyList())
    val scrollState = rememberScrollState()
    var showSignOutDialog by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(PredictaColors.charcoal)
            .verticalScroll(scrollState)
            .padding(horizontal = PredictaDimensions.Spacing.screenPadding),
    ) {
        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.lg))

        ProfileHeader(user = user)

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.lg))

        ProfileStats(user = user)

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.base))

        PersonalityCard(
            emoji = "🎉",
            name = user.personality.name,
            stat = "+318% los ${user.personality.weeklySpike}",
            cta = "ver historia",
        )

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.base))

        HealthCard(score = user.score, delta = 6)

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.lg))

        ProfileMenuSection(
            title = "Finanzas",
            items = listOf(
                MenuItem(
                    icon = Icons.Default.CreditCard,
                    label = "Suscripciones",
                    subtitle = "${subscriptions.size} activas",
                    route = Screen.Subscriptions.route,
                ),
                MenuItem(
                    icon = Icons.Default.Star,
                    label = "Score crediticio",
                    subtitle = "${user.score}/100",
                ),
            ),
            onItemClick = { item -> item.route?.let { onNavigate(it) } },
        )

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.base))

        ProfileMenuSection(
            title = "General",
            items = listOf(
                MenuItem(
                    icon = Icons.Default.Notifications,
                    label = "Notificaciones",
                    badge = "${notifications.count { it.unread }}",
                    route = Screen.Notifications.route,
                ),
                MenuItem(
                    icon = Icons.Default.Person,
                    label = "Datos personales",
                ),
                MenuItem(
                    icon = Icons.Default.Settings,
                    label = "Configuración",
                ),
            ),
            onItemClick = { item -> item.route?.let { onNavigate(it) } },
        )

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.lg))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
                .background(PredictaColors.coralSoft)
                .clickable { showSignOutDialog = true }
                .padding(vertical = PredictaDimensions.Spacing.base),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Cerrar sesión",
                style = PredictaTypography.bodyTight,
                color = PredictaColors.coral,
            )
        }

        Spacer(modifier = Modifier.height(PredictaDimensions.Spacing.xxl))
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = PredictaColors.surface,
            title = {
                Text(
                    text = "¿Cerrar sesión?",
                    style = PredictaTypography.cardTitle,
                    color = PredictaColors.cream,
                )
            },
            text = {
                Text(
                    text = "Tus datos quedan en este dispositivo. Para volver a entrar vas a necesitar registrarte de nuevo.",
                    style = PredictaTypography.small,
                    color = PredictaColors.cream60,
                )
            },
            confirmButton = {
                TextButton(onClick = { showSignOutDialog = false; onSignOut() }) {
                    Text("Sí, cerrar", color = PredictaColors.coral)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancelar", color = PredictaColors.cream60)
                }
            },
        )
    }
}
