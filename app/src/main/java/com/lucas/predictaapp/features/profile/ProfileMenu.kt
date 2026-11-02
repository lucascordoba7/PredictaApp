package com.lucas.predictaapp.features.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lucas.predictaapp.ui.components.GlyphBadge
import com.lucas.predictaapp.ui.components.Kicker
import com.lucas.predictaapp.ui.components.PredictaCard
import com.lucas.predictaapp.ui.components.RowDivider
import com.lucas.predictaapp.ui.components.StatusPill
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography

/** Estado que se muestra a la derecha de una fila en vez del chevron. */
enum class MenuStatus { ACTIVE, INACTIVE, SOON }

data class MenuItem(
    val icon: ImageVector,
    val label: String,
    val subtitle: String? = null,
    val badge: String? = null,
    val route: String? = null,
    val status: MenuStatus? = null,
) {
    val disabled: Boolean get() = status == MenuStatus.SOON
}

/** Sección del perfil: rótulo chico y una tarjeta con las filas separadas por líneas. */
@Composable
fun ProfileMenuSection(
    title: String,
    items: List<MenuItem>,
    onItemClick: (MenuItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Kicker(title, modifier = Modifier.padding(start = 4.dp))
        PredictaCard(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                MenuItemRow(item = item, onClick = { onItemClick(item) })
                if (index < items.lastIndex) RowDivider(inset = 52.dp)
            }
        }
    }
}

@Composable
private fun MenuItemRow(item: MenuItem, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.disabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        GlyphBadge(
            size = 40.dp,
            container = if (item.disabled) PredictaColors.neutralSoft else PredictaColors.accentSoft,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (item.disabled) PredictaColors.inkTertiary else PredictaColors.accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.label,
                style = PredictaTypography.rowTitle,
                color = if (item.disabled) PredictaColors.inkSecondary else PredictaColors.ink,
            )
            item.subtitle?.let {
                Text(text = it, style = PredictaTypography.rowSubtitle, color = PredictaColors.inkSecondary)
            }
        }
        Spacer(Modifier.width(8.dp))
        when (item.status) {
            MenuStatus.ACTIVE -> StatusPill("Activa", PredictaColors.successSoft, PredictaColors.success)
            MenuStatus.INACTIVE -> StatusPill("Activar", PredictaColors.accentSoft, PredictaColors.accent)
            MenuStatus.SOON -> StatusPill("Pronto", PredictaColors.neutralSoft, PredictaColors.inkSecondary)
            null -> {
                if (item.badge != null) {
                    StatusPill(item.badge, PredictaColors.accent, PredictaColors.onAccent)
                    Spacer(Modifier.width(4.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = PredictaColors.inkTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
