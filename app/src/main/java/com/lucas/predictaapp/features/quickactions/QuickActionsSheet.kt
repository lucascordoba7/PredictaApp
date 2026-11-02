package com.lucas.predictaapp.features.quickactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.LaunchActions
import com.lucas.predictaapp.ui.components.GlyphBadge
import com.lucas.predictaapp.ui.components.HeroCard
import com.lucas.predictaapp.ui.components.PredictaCard
import com.lucas.predictaapp.ui.components.PredictaSheet
import com.lucas.predictaapp.ui.components.SheetHeader
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography

/**
 * Menú del +: la única entrada a las formas de carga. La carga rápida es la principal (tarjeta
 * con degradé); escanear y dictar van debajo. Para escribir está la pestaña Chat. [onNavigate] recibe una acción de [LaunchActions] o una ruta.
 */
@Composable
fun QuickActionsSheet(
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    fun pick(route: String) {
        onDismiss()
        onNavigate(route)
    }

    PredictaSheet(onDismiss = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        ) {
            SheetHeader(title = "¿Qué anotamos?", subtitle = "Elegí cómo cargar el gasto", onClose = onDismiss)

            HeroCard(
                contentPadding = PaddingValues(18.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(PredictaDimensions.Radius.hero))
                    .clickable(role = Role.Button) { pick(LaunchActions.ACTION_MANUAL) },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlyphBadge(size = 48.dp, container = PredictaColors.honey) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, tint = PredictaColors.onHoney, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Carga rápida", style = PredictaTypography.cardTitle, color = Color.White)
                        Text("Monto y categoría, en 5 segundos", style = PredictaTypography.rowSubtitle, color = Color.White.copy(alpha = 0.85f))
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile(
                    icon = Icons.Outlined.DocumentScanner,
                    title = "Escanear ticket",
                    subtitle = "Sacale una foto",
                    onClick = { pick(LaunchActions.ACTION_SCAN) },
                    modifier = Modifier.weight(1f),
                )
                ActionTile(
                    icon = Icons.Outlined.Mic,
                    title = "Dictar un gasto",
                    subtitle = "Decilo y lo anoto",
                    onClick = { pick(LaunchActions.ACTION_VOICE) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PredictaCard(modifier = modifier, contentPadding = PaddingValues(16.dp), onClick = onClick) {
        GlyphBadge(size = 44.dp) {
            Icon(icon, contentDescription = null, tint = PredictaColors.accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.size(12.dp))
        Text(title, style = PredictaTypography.rowTitle.copy(fontSize = 15.sp, fontWeight = PredictaTypography.button.fontWeight), color = PredictaColors.ink)
        Text(subtitle, style = PredictaTypography.rowSubtitle, color = PredictaColors.inkSecondary)
    }
}
