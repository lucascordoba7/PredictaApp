package com.lucas.predictaapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle

@Composable
fun AnimatedAmount(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    formatter: (Int) -> String = { it.toString() },
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val goingUp = targetState > initialState
            val enter = slideInVertically(
                animationSpec = tween(durationMillis = 320),
                initialOffsetY = { full -> if (goingUp) full else -full },
            ) + fadeIn(tween(durationMillis = 220))
            val exit = slideOutVertically(
                animationSpec = tween(durationMillis = 320),
                targetOffsetY = { full -> if (goingUp) -full else full },
            ) + fadeOut(tween(durationMillis = 180))
            (enter togetherWith exit).using(
                androidx.compose.animation.SizeTransform(clip = false),
            )
        },
        modifier = modifier.graphicsLayer { clip = false },
        label = "AnimatedAmount",
    ) { current ->
        Text(
            text = formatter(current),
            style = style,
            color = color,
        )
    }
}
