package com.lucas.predictaapp.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.local.AppSettings
import com.lucas.predictaapp.data.local.ThemeMode
import com.lucas.predictaapp.ui.components.Kicker
import com.lucas.predictaapp.ui.components.PredictaCard
import com.lucas.predictaapp.ui.components.SegmentedToggle
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import kotlinx.coroutines.launch

/** Tema de la app: igual que el sistema, siempre claro o siempre oscuro. Se aplica al instante. */
@Composable
fun AppearanceSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val modes = remember { AppSettings.themeMode(context) }
    val mode by modes.collectAsStateWithLifecycle(ThemeMode.SYSTEM)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Kicker("Apariencia", modifier = Modifier.padding(start = 4.dp))
        PredictaCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
        ) {
            Text("Tema", style = PredictaTypography.rowTitle, color = PredictaColors.ink)
            Text(
                "Sistema sigue el modo claro u oscuro de tu teléfono.",
                style = PredictaTypography.rowSubtitle,
                color = PredictaColors.inkSecondary,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
            )
            SegmentedToggle(
                options = ThemeMode.entries.map { it.label },
                selectedIndex = mode.ordinal,
                onSelect = { i -> scope.launch { AppSettings.setThemeMode(context, ThemeMode.entries[i]) } },
            )
        }
    }
}
