package com.lucas.predictaapp.features.subscriptions

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.model.Subscription
import com.lucas.predictaapp.data.repository.SubscriptionsRepository
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs
import kotlinx.coroutines.launch

@Composable
fun SubscriptionsScreen(onBack: () -> Unit) {
    val subs by SubscriptionsRepository.subscriptions.collectAsStateWithLifecycle(emptyList())
    val zombies = subs.filter { it.zombie }
    val active = subs.filter { !it.zombie }
    val totalMonthly = subs.sumOf { it.monthly }
    val scope = rememberCoroutineScope()

    fun cancel(sub: Subscription) = scope.launch { SubscriptionsRepository.cancel(sub.id) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PredictaColors.charcoal),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            bottom = PredictaDimensions.Spacing.xxl,
        ),
    ) {
        item {
            TopBar(totalMonthly = totalMonthly, onBack = onBack)
        }

        if (zombies.isNotEmpty()) {
            item { SectionHeader("🧟 Zombies — no las usás") }
            items(zombies, key = { it.id }) { sub ->
                SubscriptionCard(sub = sub, onCancel = { cancel(sub); Unit })
            }
        }

        if (active.isNotEmpty()) {
            item { SectionHeader("✅ Activas") }
            items(active, key = { it.id }) { sub ->
                SubscriptionCard(sub = sub, onCancel = null)
            }
        }
    }
}

@Composable
private fun TopBar(totalMonthly: Int, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = PredictaDimensions.Spacing.screenPadding,
                vertical = PredictaDimensions.Spacing.base,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = -12.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = PredictaColors.cream60)
            }
            Text(
                text = "Suscripciones",
                style = PredictaTypography.cardTitle,
                color = PredictaColors.cream,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.sm))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
                .background(PredictaColors.surface)
                .padding(PredictaDimensions.Spacing.base),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Total mensual",
                    style = PredictaTypography.caption,
                    color = PredictaColors.cream35,
                )
                Text(
                    text = "\$${totalMonthly.fmtArs()}",
                    style = PredictaTypography.kpiInline,
                    color = PredictaColors.cream,
                )
            }
            Text(
                text = "por mes",
                style = PredictaTypography.small,
                color = PredictaColors.cream35,
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
private fun SubscriptionCard(
    sub: Subscription,
    onCancel: (() -> Unit)?,
) {
    val isZombie = sub.zombie
    val accentColor = if (isZombie) PredictaColors.coral else PredictaColors.green
    val accentSoft = if (isZombie) PredictaColors.coralSoft else PredictaColors.greenSoft

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = PredictaDimensions.Spacing.screenPadding,
                vertical = PredictaDimensions.Spacing.xs,
            )
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .then(
                if (isZombie) Modifier.border(
                    1.dp,
                    PredictaColors.coralSoft,
                    RoundedCornerShape(PredictaDimensions.Radius.card),
                ) else Modifier
            )
            .padding(PredictaDimensions.Spacing.base),
        verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = sub.initial,
                    style = PredictaTypography.bodyTight,
                    color = accentColor,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sub.service,
                    style = PredictaTypography.bodyTight,
                    color = PredictaColors.cream,
                )
                Text(
                    text = "\$${sub.monthly.fmtArs()}/mes",
                    style = PredictaTypography.small,
                    color = PredictaColors.cream60,
                )
            }

            if (isZombie) {
                Text(
                    text = "Zombie",
                    style = PredictaTypography.caption,
                    color = PredictaColors.coral,
                    modifier = Modifier
                        .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
                        .background(PredictaColors.coralSoft)
                        .padding(horizontal = PredictaDimensions.Spacing.sm, vertical = 2.dp),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Uso del mes",
                    style = PredictaTypography.caption,
                    color = PredictaColors.cream35,
                )
                Text(
                    text = "${sub.usagePct}%",
                    style = PredictaTypography.caption,
                    color = accentColor,
                )
            }
            LinearProgressIndicator(
                progress = { sub.usagePct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(PredictaDimensions.Radius.pill)),
                color = accentColor,
                trackColor = PredictaColors.surfaceHigh,
                strokeCap = StrokeCap.Round,
            )
        }

        if (onCancel != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(PredictaDimensions.Radius.sm))
                    .background(PredictaColors.coralSoft)
                    .clickable(onClick = onCancel)
                    .padding(vertical = PredictaDimensions.Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Cancelar suscripción",
                    style = PredictaTypography.small,
                    color = PredictaColors.coral,
                )
            }
        }
    }
}
