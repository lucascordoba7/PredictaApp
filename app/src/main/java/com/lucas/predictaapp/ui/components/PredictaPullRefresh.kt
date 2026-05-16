package com.lucas.predictaapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lucas.predictaapp.ui.theme.PredictaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pull-to-refresh wrapper themed for Predicta. Use it to wrap the LazyColumn of
 * a screen. If [onRefresh] is null the gesture still shows feedback (~700ms)
 * which is enough to acknowledge the user even when the data source is local.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictaPullRefresh(
    modifier: Modifier = Modifier,
    onRefresh: (suspend () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val state = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                try {
                    onRefresh?.invoke()
                    delay(700L)
                } finally {
                    refreshing = false
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = refreshing,
                containerColor = PredictaColors.surface,
                color = PredictaColors.amber,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
