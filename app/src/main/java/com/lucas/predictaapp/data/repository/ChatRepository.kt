package com.lucas.predictaapp.data.repository

import android.util.Log
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.data.model.ExpenseExtraction
import com.lucas.predictaapp.data.model.ExpenseSuggestion
import com.lucas.predictaapp.data.remote.ApiProvider
import com.lucas.predictaapp.data.remote.ChatCompletionRequest
import com.lucas.predictaapp.data.remote.ChatMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "ChatRepository"
private const val MODEL = "llama-3.3-70b-versatile"

private val SYSTEM_PROMPT = """
Sos Predicta, un asistente financiero argentino. Extraés gastos e ingresos de texto natural.

Devolvés UN JSON puro, sin markdown, de uno de estos formatos:

FORMATO A — gasto:
{"kind":"expense","merchant":"nombre","category":"CATEGORÍA","amount":number}

FORMATO B — ingreso:
{"kind":"income","merchant":"descripción","amount":number}

FORMATO C — necesita aclaración:
{"kind":"clarify","reason":"qué falta","question":"pregunta","suggestions":[{"label":"...","rewritten_intent":"..."}]}

FORMATO D — no financiero:
{"kind":"unknown","reply":"respuesta amigable en 1 oración, rioplatense"}

Categorías válidas (usá EXACTAMENTE una de estas, sin variantes):
${ExpenseCategories.promptList}

Reglas:
- Monto siempre en pesos argentinos
- Si mencionan USD/dólar pedí aclaración
- Usás "vos", español rioplatense natural
- 1 solo JSON, sin texto extra
- La categoría debe ser exactamente una de la lista
""".trimIndent()

object ChatRepository {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun extract(userText: String): Pair<ExpenseExtraction, String?> {
        return try {
            callGroq(userText)
        } catch (e: Exception) {
            Log.w(TAG, "Groq call failed", e)
            ExpenseExtraction.Unknown to "No pude conectarme. Intentá de nuevo."
        }
    }

    private suspend fun callGroq(userText: String): Pair<ExpenseExtraction, String?> {
        val response = ApiProvider.groqApi.chatCompletion(
            ChatCompletionRequest(
                model = MODEL,
                messages = listOf(
                    ChatMessage("system", SYSTEM_PROMPT),
                    ChatMessage("user", userText),
                ),
            ),
        )
        val raw = response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Empty response")

        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        val obj = json.parseToJsonElement(cleaned).jsonObject
        val kind = obj["kind"]?.jsonPrimitive?.contentOrNull ?: "unknown"

        return when (kind) {
            "expense" -> {
                val merchant = obj["merchant"]?.jsonPrimitive?.contentOrNull ?: "Gasto"
                val category = obj["category"]?.jsonPrimitive?.contentOrNull ?: "Otro"
                val amount = obj["amount"]?.jsonPrimitive?.intOrNull ?: 0
                ExpenseExtraction.Expense(merchant, category, amount) to null
            }
            "income" -> {
                val merchant = obj["merchant"]?.jsonPrimitive?.contentOrNull ?: "Ingreso"
                val amount = obj["amount"]?.jsonPrimitive?.intOrNull ?: 0
                ExpenseExtraction.Income(merchant, amount = amount) to null
            }
            "clarify" -> {
                val reason = obj["reason"]?.jsonPrimitive?.contentOrNull ?: ""
                val question = obj["question"]?.jsonPrimitive?.contentOrNull ?: "¿Podés aclarar?"
                val suggestions = obj["suggestions"]?.jsonArray?.mapNotNull { el ->
                    val o = el.jsonObject
                    val label = o["label"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val intent = o["rewritten_intent"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    ExpenseSuggestion(label, intent)
                } ?: emptyList()
                ExpenseExtraction.Clarify(reason, question, suggestions) to null
            }
            else -> {
                val reply = obj["reply"]?.jsonPrimitive?.contentOrNull
                    ?: "Solo puedo ayudarte con gastos e ingresos."
                ExpenseExtraction.Unknown to reply
            }
        }
    }
}
