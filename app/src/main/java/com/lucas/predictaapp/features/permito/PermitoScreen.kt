package com.lucas.predictaapp.features.permito

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucas.predictaapp.data.local.UserPreferencesRepository
import com.lucas.predictaapp.data.model.ActiveGoal
import com.lucas.predictaapp.data.model.CategorySpending
import com.lucas.predictaapp.data.model.Fixtures
import com.lucas.predictaapp.data.model.PermitoContext
import com.lucas.predictaapp.data.model.PermitoOutput
import com.lucas.predictaapp.data.repository.ExpensesRepository
import com.lucas.predictaapp.data.repository.PermitoRepository
import com.lucas.predictaapp.features.permito.components.ClarifyCard
import com.lucas.predictaapp.features.permito.components.PermitoInput
import com.lucas.predictaapp.features.permito.components.PermitoLoading
import com.lucas.predictaapp.features.permito.components.VerdictCard
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography
import com.lucas.predictaapp.ui.utils.isCurrentMonth
import kotlinx.coroutines.launch

private sealed class PermitoUiState {
    data class Input(val text: String = "") : PermitoUiState()
    data object Loading : PermitoUiState()
    data class Result(val output: PermitoOutput) : PermitoUiState()
    data class Error(val lastQuery: String) : PermitoUiState()
}

@Composable
fun PermitoScreen() {
    val context = LocalContext.current
    val userSetup by UserPreferencesRepository.getUserSetup(context).collectAsStateWithLifecycle(null)
    val expenses by ExpensesRepository.expenses.collectAsStateWithLifecycle(emptyList())

    var state: PermitoUiState by remember { mutableStateOf(PermitoUiState.Input()) }
    val scope = rememberCoroutineScope()

    fun buildContext(): PermitoContext {
        val user = Fixtures.user
        val income = userSetup?.income ?: user.monthIncome
        val fixedMonthly = userSetup?.fixedMonthly ?: user.fixedMonthly
        val paydayDay = userSetup?.paydayDay ?: 1

        val monthExpenses = expenses.filter { isCurrentMonth(it.dateMillis) }
        val totalSpent = monthExpenses.filter { it.category != "Ingreso" }.sumOf { it.amount }
        val availableNow = (income - fixedMonthly - totalSpent).coerceAtLeast(0)

        val daysToPayday = com.lucas.predictaapp.ui.utils.getDaysToPayday(paydayDay)
        val monthlyBudgetLeft = income - totalSpent

        val spending = monthExpenses
            .groupBy { it.category }
            .map { (cat, list) -> CategorySpending(cat, list.sumOf { it.amount }) }

        return PermitoContext(
            availableNow = availableNow,
            fixedPending = fixedMonthly,
            daysToPayday = daysToPayday,
            monthlyBudgetLeft = monthlyBudgetLeft,
            activeGoals = listOf(ActiveGoal("Viaje a Chile", Fixtures.groupGoal.current, Fixtures.groupGoal.target)),
            monthlySpending = spending,
        )
    }

    fun submit(text: String) {
        if (text.isBlank()) return
        state = PermitoUiState.Loading
        scope.launch {
            runCatching { PermitoRepository.ask(text, buildContext()) }
                .onSuccess { state = PermitoUiState.Result(it) }
                .onFailure { state = PermitoUiState.Error(text) }
        }
    }

    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "permito_state",
    ) { current ->
        when (current) {
            is PermitoUiState.Input -> PermitoInput(
                text = current.text,
                onTextChange = { state = PermitoUiState.Input(it) },
                onSubmit = { submit((state as? PermitoUiState.Input)?.text ?: "") },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            )

            is PermitoUiState.Loading -> PermitoLoading(
                modifier = Modifier.fillMaxSize(),
            )

            is PermitoUiState.Result -> when (val output = current.output) {
                is PermitoOutput.Verdict -> VerdictCard(
                    result = output,
                    onBack = { state = PermitoUiState.Input() },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                )

                is PermitoOutput.Clarify -> ClarifyCard(
                    result = output,
                    onBack = { state = PermitoUiState.Input() },
                    onSuggestionClick = { rewritten -> submit(rewritten) },
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                )
            }

            is PermitoUiState.Error -> PermitoError(
                onRetry = { submit(current.lastQuery) },
                onBack = { state = PermitoUiState.Input() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PermitoError(
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = PredictaDimensions.Spacing.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.WifiOff,
            contentDescription = null,
            tint = PredictaColors.cream35,
            modifier = Modifier.size(48.dp),
        )

        Spacer(Modifier.height(PredictaDimensions.Spacing.base))

        Text(
            text = "No se pudo conectar",
            style = PredictaTypography.cardTitle,
            color = PredictaColors.cream,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(PredictaDimensions.Spacing.sm))

        Text(
            text = "Verificá tu conexión a internet\ny volvé a intentarlo.",
            style = PredictaTypography.body,
            color = PredictaColors.cream60,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(PredictaDimensions.Spacing.xl))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
                .background(PredictaColors.amber)
                .clickable(onClick = onRetry)
                .padding(vertical = PredictaDimensions.Spacing.base),
            contentAlignment = Alignment.Center,
        ) {
            Text("Reintentar", style = PredictaTypography.bodyTight, color = PredictaColors.onAmber)
        }

        Spacer(Modifier.height(PredictaDimensions.Spacing.sm))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(PredictaDimensions.Radius.card))
                .background(PredictaColors.surface)
                .border(1.dp, PredictaColors.lineStrong, RoundedCornerShape(PredictaDimensions.Radius.card))
                .clickable(onClick = onBack)
                .padding(vertical = PredictaDimensions.Spacing.base),
            contentAlignment = Alignment.Center,
        ) {
            Text("Volver", style = PredictaTypography.bodyTight, color = PredictaColors.cream60)
        }
    }
}
