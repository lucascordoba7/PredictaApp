package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography
import java.time.YearMonth

@Composable
fun DashboardHeader(
    name: String,
    month: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetMonth: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 6.dp, end = 6.dp, bottom = 4.dp),
    ) {
        MonthSelector(
            month = month,
            onPrev = onPrevMonth,
            onNext = onNextMonth,
            onReset = onResetMonth,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = PredictaColors.cream60, fontWeight = FontWeight.Bold)) {
                    append("Hola, ")
                }
                withStyle(SpanStyle(color = PredictaColors.amber, fontWeight = FontWeight.Bold)) {
                    append("$name.")
                }
            },
            style = PredictaTypography.titlePage.copy(
                fontSize = 30.sp,
                letterSpacing = (-0.8).sp,
            ),
        )
    }
}
