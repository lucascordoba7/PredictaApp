package com.lucas.predictaapp.features.permito

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.lucas.predictaapp.data.model.ActiveGoal
import com.lucas.predictaapp.data.model.CategorySpending
import com.lucas.predictaapp.data.model.Fixtures
import com.lucas.predictaapp.data.model.PermitoContext
import com.lucas.predictaapp.data.model.PermitoOutput
import com.lucas.predictaapp.data.repository.PermitoRepository
import com.lucas.predictaapp.features.permito.components.ClarifyCard
import com.lucas.predictaapp.features.permito.components.PermitoInput
import com.lucas.predictaapp.features.permito.components.PermitoLoading
import com.lucas.predictaapp.features.permito.components.VerdictCard
import kotlinx.coroutines.launch

private sealed class PermitoUiState {
    data class Input(val text: String = "") : PermitoUiState()
    data object Loading : PermitoUiState()
    data class Result(val output: PermitoOutput) : PermitoUiState()
}

private fun buildContext(): PermitoContext {
    val user = Fixtures.user
    val spending = Fixtures.expenses
        .groupBy { it.category }
        .map { (cat, list) -> CategorySpending(cat, list.sumOf { it.amount }) }
    return PermitoContext(
        availableNow = user.availableNow,
        fixedPending = user.fixedMonthly,
        daysToPayday = user.daysToPayday,
        monthlyBudgetLeft = user.monthIncome - user.monthSpent,
        activeGoals = listOf(ActiveGoal("Viaje a Chile", Fixtures.groupGoal.current, Fixtures.groupGoal.target)),
        monthlySpending = spending,
    )
}

@Composable
fun PermitoScreen() {
    var state: PermitoUiState by remember { mutableStateOf(PermitoUiState.Input()) }
    val scope = rememberCoroutineScope()

    fun submit(text: String) {
        if (text.isBlank()) return
        state = PermitoUiState.Loading
        scope.launch {
            val output = PermitoRepository.ask(text, buildContext())
            state = PermitoUiState.Result(output)
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
        }
    }
}
