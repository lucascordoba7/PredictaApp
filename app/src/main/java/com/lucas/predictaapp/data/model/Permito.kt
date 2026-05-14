package com.lucas.predictaapp.data.model

sealed class PermitoOutput {
    data class Verdict(
        val verdict: PermitoVerdict,
        val parsedPurchase: ParsedPurchase,
        val reasoning: String,
        val breakdown: List<BreakdownRow>,
        val alternativeAdvice: String? = null,
        val suggestedWaitUntil: String? = null,
        val promoStore: String? = null,
        val promoStoreUrl: String? = null,
        val promoCategory: String? = null,
        val promoFromPrice: Int? = null,
    ) : PermitoOutput()

    data class Clarify(
        val userEcho: String,
        val reason: String,
        val question: String,
        val suggestions: List<PermitoSuggestion>,
    ) : PermitoOutput()
}

enum class PermitoVerdict { YES, WAIT, NO }

data class ParsedPurchase(val item: String, val amount: Int)
data class BreakdownRow(val label: String, val value: Int)
data class PermitoSuggestion(val label: String, val rewrittenIntent: String)

data class PermitoContext(
    val availableNow: Int,
    val fixedPending: Int,
    val daysToPayday: Int,
    val monthlyBudgetLeft: Int,
    val activeGoals: List<ActiveGoal>,
    val monthlySpending: List<CategorySpending>,
)

data class ActiveGoal(val name: String, val progress: Int, val target: Int)
data class CategorySpending(val category: String, val amount: Int)
