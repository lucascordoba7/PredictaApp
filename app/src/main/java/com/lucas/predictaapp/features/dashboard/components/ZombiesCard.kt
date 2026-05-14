package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs

@Composable
fun ZombiesCard(
    count: Int,
    services: List<String>,
    monthlySaving: Int,
    onCancel: () -> Unit,
) {
    val list = when (services.size) {
        0 -> ""
        1 -> services[0]
        else -> "${services.dropLast(1).joinToString(", ")} ni ${services.last()}"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(PredictaColors.surfaceMid, PredictaColors.surface)))
            .border(
                width = 1.dp,
                color = PredictaColors.lineStrong,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable { onCancel() }
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    append("🧟 ")
                    withStyle(SpanStyle(color = PredictaColors.coral, fontWeight = FontWeight.SemiBold)) {
                        append("$count zombis")
                    }
                    append(" detectados")
                },
                style = PredictaTypography.small.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = PredictaColors.cream,
                    lineHeight = 18.sp,
                ),
            )
            Text(
                text = buildAnnotatedString {
                    append("No usás $list hace 60 días. ")
                    withStyle(SpanStyle(color = PredictaColors.green, fontWeight = FontWeight.Medium)) {
                        append("Ahorrás \$${monthlySaving.fmtArs()}/mes")
                    }
                    append(".")
                },
                style = PredictaTypography.small.copy(
                    fontSize = 12.sp,
                    color = PredictaColors.cream60,
                    lineHeight = 17.sp,
                ),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, PredictaColors.coral.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
                .background(PredictaColors.coral.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Cancelar",
                style = PredictaTypography.small.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = PredictaColors.coral,
                ),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = PredictaColors.coral,
                modifier = Modifier
                    .width(11.dp)
                    .padding(0.dp),
            )
        }
    }
}
