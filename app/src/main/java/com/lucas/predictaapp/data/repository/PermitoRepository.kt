package com.lucas.predictaapp.data.repository

import android.util.Log
import com.lucas.predictaapp.data.model.ActiveGoal
import com.lucas.predictaapp.data.model.BreakdownRow
import com.lucas.predictaapp.data.model.CategorySpending
import com.lucas.predictaapp.data.model.ParsedPurchase
import com.lucas.predictaapp.data.model.PermitoContext
import com.lucas.predictaapp.data.model.PermitoOutput
import com.lucas.predictaapp.data.model.PermitoSuggestion
import com.lucas.predictaapp.data.model.PermitoVerdict
import com.lucas.predictaapp.data.remote.ApiProvider
import com.lucas.predictaapp.data.remote.ChatCompletionRequest
import com.lucas.predictaapp.data.remote.ChatMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate

private const val TAG = "PermitoRepository"
private const val MODEL = "llama-3.3-70b-versatile"

private val SYSTEM_PROMPT = """
Sos Predicta, un copiloto financiero argentino. Hablás con vos, sos directo, sin
condescendencia, con humor leve y rioplatense natural.

Recibís en el mensaje del usuario:
- intent: lo que dijo el usuario (texto libre, puede tener slang, errores, omisiones)
- context: su situación financiera del mes
- today: fecha actual (YYYY-MM-DD)
- verdict_rules: umbrales numéricos para decidir verdict

Devolvés UN JSON con dos formatos posibles según el caso:

═══ FORMATO A — verdict (caso normal: entendiste intención y monto) ═══
{
  "kind": "verdict",
  "verdict": "yes" | "wait" | "no",
  "parsed_purchase": { "item": string, "amount": number },
  "reasoning": "1-2 oraciones explicando por qué (rioplatense, anclado en números)",
  "breakdown": [
    {"label":"Disponible hoy","value":number},
    {"label":"Fijos pendientes","value":-number},
    {"label":"Esta compra","value":-number},
    {"label":"Después","value":number}
  ],
  "alternative_advice": "OPCIONAL — solo si podés ofrecer algo concreto",
  "suggested_wait_until": "YYYY-MM-DD si verdict==='wait'",
  "promo_store": "tienda AR donde se consigue",
  "promo_store_url": "URL base",
  "promo_category": "3-4 palabras",
  "promo_from_price": number
}

═══ FORMATO B — clarify (cuando hay ambigüedad real) ═══
{
  "kind": "clarify",
  "user_echo": "el texto original del usuario, tal cual",
  "reason": "qué no cerró, 1 frase corta",
  "question": "pregunta concreta al usuario",
  "suggestions": [
    { "label": "$80.000", "rewritten_intent": "play 5 a 80000 pesos" }
  ]
}

═══ Cuándo usar clarify ═══
SÍ usá clarify cuando el monto es ambiguo o en moneda extranjera.
NO uses clarify si podés inferir un monto razonable.

═══ Reglas del verdict ═══
1. Calculá: después = context.availableNow - parsed_purchase.amount
2. Si después >= verdict_rules.yes_if_after_gte → "yes"
3. Si después >= verdict_rules.wait_if_after_gte → "wait"
4. Si después < verdict_rules.no_if_after_lt → "no"

═══ Reglas del reasoning ═══
- Rioplatense, usá "vos" nunca "tú"
- 1-2 oraciones, máximo 30 palabras
- Anclá en al menos 1 número real del context

Devolvés SOLO el JSON. Sin markdown. Sin texto extra.
""".trimIndent()

object PermitoRepository {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun ask(userText: String, context: PermitoContext): PermitoOutput {
        return try {
            callGroq(userText, context)
        } catch (e: Exception) {
            Log.w(TAG, "Groq call failed, using mock fallback", e)
            mockFallback(userText, context)
        }
    }

    private suspend fun callGroq(userText: String, context: PermitoContext): PermitoOutput {
        val available = context.availableNow
        val userMessage = buildUserMessage(userText, context, available)

        val response = ApiProvider.groqApi.chatCompletion(
            ChatCompletionRequest(
                model = MODEL,
                messages = listOf(
                    ChatMessage("system", SYSTEM_PROMPT),
                    ChatMessage("user", userMessage),
                ),
            ),
        )

        val raw = response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Empty response from Groq")

        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        val jsonObj = json.parseToJsonElement(cleaned).jsonObject
        return sanitize(jsonObj, userText, context)
    }

    private fun buildUserMessage(userText: String, ctx: PermitoContext, available: Int): String {
        val today = LocalDate.now().toString()
        return """{"intent":"$userText","context":{"availableNow":${ctx.availableNow},"fixedPending":${ctx.fixedPending},"daysToPayday":${ctx.daysToPayday},"monthlyBudgetLeft":${ctx.monthlyBudgetLeft},"activeGoals":${goalsJson(ctx.activeGoals)},"monthlySpending":${spendingJson(ctx.monthlySpending)}},"today":"$today","verdict_rules":{"yes_if_after_gte":${(available * 0.35).toInt()},"wait_if_after_gte":${(available * 0.15).toInt()},"no_if_after_lt":${(available * 0.15).toInt()}}}"""
    }

    private fun goalsJson(goals: List<ActiveGoal>) =
        "[${goals.joinToString(",") { """{"name":"${it.name}","progress":${it.progress},"target":${it.target}}""" }}]"

    private fun spendingJson(spending: List<CategorySpending>) =
        "[${spending.joinToString(",") { """{"category":"${it.category}","amount":${it.amount}}""" }}]"

    private fun sanitize(r: JsonObject, userText: String, ctx: PermitoContext): PermitoOutput {
        val kind = r["kind"]?.jsonPrimitive?.contentOrNull ?: "verdict"

        if (kind == "clarify") {
            val rawSugg = r["suggestions"]?.jsonArray ?: return clarifyDefault(userText)
            val suggestions = rawSugg.mapNotNull { el ->
                val obj = el.jsonObject
                val label = obj["label"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val intent = obj["rewritten_intent"]?.jsonPrimitive?.contentOrNull
                    ?: obj["rewrittenIntent"]?.jsonPrimitive?.contentOrNull
                    ?: return@mapNotNull null
                PermitoSuggestion(label.take(40), intent.take(200))
            }.take(3)

            if (suggestions.isEmpty()) return clarifyDefault(userText)

            return PermitoOutput.Clarify(
                userEcho = r["user_echo"]?.jsonPrimitive?.contentOrNull?.take(200) ?: userText,
                reason = r["reason"]?.jsonPrimitive?.contentOrNull?.take(140)
                    ?: "No me cerró del todo lo que me pasaste.",
                question = r["question"]?.jsonPrimitive?.contentOrNull?.take(160)
                    ?: "¿Lo querés reescribir?",
                suggestions = suggestions,
            )
        }

        val verdictRaw = r["verdict"]?.jsonPrimitive?.contentOrNull ?: "no"
        val verdict = when (verdictRaw) {
            "yes" -> PermitoVerdict.YES
            "wait" -> PermitoVerdict.WAIT
            else -> PermitoVerdict.NO
        }

        val purchaseObj = r["parsed_purchase"]?.jsonObject ?: r["parsedPurchase"]?.jsonObject
        val item = purchaseObj?.get("item")?.jsonPrimitive?.contentOrNull?.take(80) ?: userText.take(80)
        val amount = parseAmount(purchaseObj?.get("amount"))

        val reasoning = r["reasoning"]?.jsonPrimitive?.contentOrNull?.take(300) ?: "Sin detalle."

        val rawBreakdown = r["breakdown"]?.jsonArray ?: emptyList<JsonObject>()
        val breakdown = rawBreakdown.mapNotNull { el ->
            val obj = el.jsonObject
            val label = obj["label"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val value = obj["value"]?.jsonPrimitive?.doubleOrNull?.toInt()
                ?: obj["value"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            BreakdownRow(label, value)
        }

        val finalBreakdown = breakdown.ifEmpty {
            val after = ctx.availableNow - ctx.fixedPending - amount
            listOf(
                BreakdownRow("Disponible hoy", ctx.availableNow),
                BreakdownRow("Fijos pendientes", -ctx.fixedPending),
                BreakdownRow("Esta compra", -amount),
                BreakdownRow("Después", after),
            )
        }

        return PermitoOutput.Verdict(
            verdict = verdict,
            parsedPurchase = ParsedPurchase(item, amount),
            reasoning = reasoning,
            breakdown = finalBreakdown,
            alternativeAdvice = r["alternative_advice"]?.jsonPrimitive?.contentOrNull?.take(200),
            suggestedWaitUntil = r["suggested_wait_until"]?.jsonPrimitive?.contentOrNull,
            promoStore = r["promo_store"]?.jsonPrimitive?.contentOrNull,
            promoStoreUrl = r["promo_store_url"]?.jsonPrimitive?.contentOrNull,
            promoCategory = r["promo_category"]?.jsonPrimitive?.contentOrNull,
            promoFromPrice = r["promo_from_price"]?.jsonPrimitive?.intOrNull,
        )
    }

    private fun parseAmount(el: kotlinx.serialization.json.JsonElement?): Int {
        if (el == null) return 0
        val prim = el.jsonPrimitive
        if (prim.isString) {
            val s = prim.content.trim().lowercase().replace("\\s".toRegex(), "")
            val kMatch = Regex("^-?([\\d.,]+)k$").find(s)
            if (kMatch != null) return (kMatch.groupValues[1].replace("[.,]".toRegex(), "").toDoubleOrNull() ?: 0.0).times(1000).toInt()
            val mMatch = Regex("^-?([\\d.,]+)m$").find(s)
            if (mMatch != null) return (mMatch.groupValues[1].replace("[.,]".toRegex(), "").toDoubleOrNull() ?: 0.0).times(1_000_000).toInt()
            return s.replace("[^\\d]".toRegex(), "").toIntOrNull() ?: 0
        }
        return kotlin.math.abs(prim.double.toInt())
    }

    private fun clarifyDefault(userText: String) = PermitoOutput.Clarify(
        userEcho = userText.take(200),
        reason = "No me cerró del todo lo que me pasaste.",
        question = "¿Lo querés reescribir un poco más claro?",
        suggestions = emptyList(),
    )

    private fun mockFallback(userText: String, ctx: PermitoContext): PermitoOutput {
        val amount = Regex("(\\d[\\d.,]*)").find(userText)?.value?.replace("[^\\d]".toRegex(), "")?.toIntOrNull() ?: 30_000
        val after = ctx.availableNow - ctx.fixedPending - amount
        val threshold = ctx.availableNow * 0.15
        val verdict = when {
            after > threshold * 3 -> PermitoVerdict.YES
            after > threshold -> PermitoVerdict.WAIT
            else -> PermitoVerdict.NO
        }
        return PermitoOutput.Verdict(
            verdict = verdict,
            parsedPurchase = ParsedPurchase(userText.take(60), amount),
            reasoning = when (verdict) {
                PermitoVerdict.YES -> "Te quedan \$${after.fmtArs()} después. Entra cómodo."
                PermitoVerdict.WAIT -> "Te quedaría justo antes del cobro. Esperá ${ctx.daysToPayday} días."
                PermitoVerdict.NO -> "Te dejaría en rojo antes de fin de mes. Este mes no."
            },
            breakdown = listOf(
                BreakdownRow("Disponible hoy", ctx.availableNow),
                BreakdownRow("Fijos pendientes", -ctx.fixedPending),
                BreakdownRow("Esta compra", -amount),
                BreakdownRow("Después", after),
            ),
            promoStore = "MercadoLibre",
            promoFromPrice = (amount * 0.75).toInt(),
        )
    }
}

private fun Int.fmtArs(): String = java.text.NumberFormat.getNumberInstance(java.util.Locale("es", "AR")).format(this)
