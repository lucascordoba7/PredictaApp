package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.IBMPlexMono
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.fmtArs

@Composable
fun AvailableNowCard(
    availableNow: Int,
    income: Int = 0,
    totalSpent: Int = 0,
    totalFixedPaid: Int = 0,
    totalFixedPending: Int = 0,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
            .background(PredictaColors.surface)
            .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(PredictaDimensions.Radius.card))
            .padding(horizontal = PredictaDimensions.Spacing.base, vertical = PredictaDimensions.Spacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "HOY TENÉS",
                style = PredictaTypography.monoCap.copy(fontSize = 9.sp, letterSpacing = 1.sp),
                color = PredictaColors.cream35,
            )
            if (income > 0) {
                Text(
                    text = "ingreso  $ ${income.fmtArs()}",
                    style = PredictaTypography.monoCap.copy(
                        fontFamily = IBMPlexMono,
                        fontSize = 9.sp,
                        letterSpacing = 0.sp,
                        color = PredictaColors.cream35,
                    ),
                )
            }
        }

        Text(
            text = "$ ${availableNow.fmtArs()}",
            style = PredictaTypography.bodyTight.copy(
                fontFamily = IBMPlexMono,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-1).sp,
                color = PredictaColors.cream,
            ),
            modifier = Modifier.padding(bottom = 14.dp),
        )

        if (income > 0) {
            val total = income.toFloat()
            val paidFrac    = (totalFixedPaid    / total).coerceIn(0f, 1f)
            val pendingFrac = (totalFixedPending / total).coerceIn(0f, 1f - paidFrac)
            val spentFrac   = (totalSpent        / total).coerceIn(0f, 1f - paidFrac - pendingFrac)
            val remainFrac  = (1f - paidFrac - pendingFrac - spentFrac).coerceAtLeast(0f)
            val fixedFrac   = paidFrac + pendingFrac
            val charcoal    = PredictaColors.charcoal

            // Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .drawWithContent {
                        drawContent()
                        if (fixedFrac > 0.02f && fixedFrac < 0.98f) {
                            val x = fixedFrac * size.width
                            drawRect(
                                color = charcoal,
                                topLeft = Offset(x - 1f, 0f),
                                size = Size(2.dp.toPx(), size.height),
                            )
                        }
                    },
            ) {
                if (paidFrac > 0f)
                    Box(Modifier.weight(paidFrac).height(6.dp).background(PredictaColors.green))
                if (pendingFrac > 0f)
                    Box(Modifier.weight(pendingFrac).height(6.dp).background(PredictaColors.pending))
                if (spentFrac > 0f)
                    Box(Modifier.weight(spentFrac).height(6.dp).background(PredictaColors.cream60))
                if (remainFrac > 0f)
                    Box(Modifier.weight(remainFrac).height(6.dp).background(PredictaColors.surfaceHigh))
            }

            // Group labels below bar, aligned to each section
            if (fixedFrac > 0.02f && fixedFrac < 0.98f) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) {
                    Text(
                        text = "FIJOS",
                        style = PredictaTypography.monoCap.copy(
                            fontSize = 8.sp,
                            letterSpacing = 0.8.sp,
                            color = PredictaColors.cream35,
                        ),
                        modifier = Modifier.weight(fixedFrac),
                    )
                    Text(
                        text = "GASTOS DEL MES",
                        style = PredictaTypography.monoCap.copy(
                            fontSize = 8.sp,
                            letterSpacing = 0.8.sp,
                            color = PredictaColors.cream35,
                        ),
                        modifier = Modifier.weight(1f - fixedFrac),
                    )
                }
            }

        } else {
            Text(
                text = "DISPONIBLE",
                style = PredictaTypography.monoCap.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                    color = PredictaColors.cream35,
                ),
            )
        }
    }
}
