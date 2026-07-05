package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.monthYearLabel
import java.time.YearMonth

/**
 * Navegador de mes del dashboard: ‹ Julio 2026 ›. Stateless — el mes vive en el caller.
 * El chevron derecho se apaga en el mes actual (no se navega al futuro) y el chip HOY
 * vuelve de un salto cuando estás mirando un mes pasado.
 */
@Composable
fun MonthSelector(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCurrent = month == YearMonth.now()

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.ChevronLeft,
            contentDescription = "Mes anterior",
            tint = PredictaColors.cream60,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable(onClick = onPrev),
        )
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                // El mes entra desde el lado hacia el que navegás (‹ pasado / futuro ›).
                val forward = targetState > initialState
                val enter = slideInHorizontally { w -> if (forward) w else -w } + fadeIn()
                val exit = slideOutHorizontally { w -> if (forward) -w else w } + fadeOut()
                enter togetherWith exit
            },
            label = "monthLabel",
        ) { m ->
            Text(
                text = monthYearLabel(m),
                style = PredictaTypography.monoCap.copy(
                    letterSpacing = 1.4.sp,
                    color = if (m == YearMonth.now()) PredictaColors.cream35 else PredictaColors.amber,
                ),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Mes siguiente",
            tint = if (isCurrent) PredictaColors.cream35.copy(alpha = 0.25f) else PredictaColors.cream60,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable(enabled = !isCurrent, onClick = onNext),
        )
        if (!isCurrent) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "HOY",
                style = PredictaTypography.monoCap.copy(
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp,
                    color = PredictaColors.amber,
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(PredictaColors.amber.copy(alpha = 0.12f))
                    .clickable(onClick = onReset)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}
