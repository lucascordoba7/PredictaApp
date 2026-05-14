package com.lucas.predictaapp.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaTypography

@Composable
fun BudgetAlert(
    title: String,
    description: String,
    highlight: String? = null,
    onDismiss: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = PredictaColors.line,
                shape = RoundedCornerShape(14.dp),
            )
            .background(PredictaColors.surface)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(PredictaColors.pendingSoft),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = PredictaColors.pending,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PredictaTypography.small.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = PredictaColors.cream,
                    lineHeight = 18.sp,
                ),
            )
            Text(
                text = buildAnnotatedString {
                    append(description)
                    if (highlight != null) {
                        append(" ")
                        withStyle(SpanStyle(color = PredictaColors.coral, fontWeight = FontWeight.Medium)) {
                            append(highlight)
                        }
                    }
                    append(".")
                },
                style = PredictaTypography.small.copy(
                    fontSize = 12.5.sp,
                    color = PredictaColors.cream60,
                    lineHeight = 19.sp,
                ),
            )
        }
        if (onDismiss != null) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = PredictaColors.cream35,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
