package com.lucas.predictaapp.features.quickactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lucas.predictaapp.LaunchActions
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography

data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val sublabel: String,
    val color: Color,
    val colorSoft: Color,
    val route: String,
)

private val actions = listOf(
    QuickAction(
        icon = Icons.Filled.Bolt,
        label = "Carga rápida",
        sublabel = "Monto y categoría, sin IA",
        color = PredictaColors.amber,
        colorSoft = PredictaColors.amberSoft,
        route = LaunchActions.ACTION_MANUAL,
    ),
    QuickAction(
        icon = Icons.Filled.AddChart,
        label = "Contarle al chat",
        sublabel = "Escribir o dictar por voz",
        color = PredictaColors.green,
        colorSoft = PredictaColors.greenSoft,
        route = "chat",
    ),
    QuickAction(
        icon = Icons.Filled.CameraAlt,
        label = "Escanear ticket",
        sublabel = "Foto del comprobante",
        color = PredictaColors.pending,
        colorSoft = PredictaColors.pendingSoft,
        route = LaunchActions.ACTION_SCAN,
    ),
    QuickAction(
        icon = Icons.Filled.ReceiptLong,
        label = "Transacciones",
        sublabel = "Ver, buscar y filtrar",
        color = PredictaColors.cream60,
        colorSoft = PredictaColors.cream12,
        route = "transactions",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsSheet(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PredictaColors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = PredictaDimensions.Spacing.sm)
                    .size(width = 40.dp, height = 4.dp)
                    .background(PredictaColors.lineStrong, RoundedCornerShape(PredictaDimensions.Radius.pill)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = PredictaDimensions.Spacing.screenPadding,
                    end = PredictaDimensions.Spacing.screenPadding,
                    bottom = PredictaDimensions.Spacing.xxl,
                ),
        ) {
            Text(
                text = "¿Qué hacemos?",
                style = PredictaTypography.cardTitle,
                color = PredictaColors.cream,
            )
            Spacer(Modifier.height(PredictaDimensions.Spacing.xs))
            Text(
                text = "Elegí una acción rápida",
                style = PredictaTypography.small,
                color = PredictaColors.cream35,
            )
            Spacer(Modifier.height(PredictaDimensions.Spacing.lg))

            Column(verticalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.sm)) {
                actions.forEach { action ->
                    QuickActionRow(
                        action = action,
                        onClick = {
                            onDismiss()
                            onNavigate(action.route)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    action: QuickAction,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surfaceHigh)
            .clickable(onClick = onClick)
            .padding(PredictaDimensions.Spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PredictaDimensions.Spacing.base),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(action.colorSoft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = action.color,
                modifier = Modifier.size(22.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.label,
                style = PredictaTypography.bodyTight,
                color = PredictaColors.cream,
            )
            Text(
                text = action.sublabel,
                style = PredictaTypography.caption,
                color = PredictaColors.cream35,
            )
        }
    }
}
