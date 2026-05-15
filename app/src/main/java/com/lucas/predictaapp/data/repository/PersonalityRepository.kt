package com.lucas.predictaapp.data.repository

import android.util.Log
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.model.Fixtures
import com.lucas.predictaapp.data.model.Personality
import com.lucas.predictaapp.data.remote.ApiProvider
import com.lucas.predictaapp.data.remote.ChatCompletionRequest
import com.lucas.predictaapp.data.remote.ChatMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "PersonalityRepository"
private const val MODEL = "llama-3.3-70b-versatile"

const val MIN_EXPENSES_FOR_PERSONALITY = 3

private val VALID_COLORS = setOf("amber", "coral", "green", "blue", "purple", "teal", "pink")

private val SYSTEM_PROMPT = """
Sos Predicta, un copiloto financiero argentino. Analizás los gastos de un usuario y construís su
"personalidad financiera": un arquetipo que captura quién es esta persona a través de lo que gasta.

Recibís el historial de gastos del mes: comercios, categorías, montos, días.

Tu trabajo es encontrar EL patrón más revelador. Puede ser cualquier cosa:
- Un comercio que aparece mucho (Rappi todas las noches → "El Delivery Addict")
- Una categoría dominante (70% en salidas → "La Anfitriona")
- Un comportamiento de ahorro o exceso
- Un estilo de vida que emerge de la combinación (gym + suplementos + ropa deportiva → "El Atleta de Domingo")
- Una contradicción interesante (muy ordenado en semana, caótico los fines de semana)
- El momento del mes donde más gasta (principio de mes vs fin)
- La frecuencia: muchas compras pequeñas vs pocas grandes

No estés limitado a días de semana. Buscá lo que hace única a esta persona.

Devolvés UN JSON con este formato exacto:
{
  "name": "nombre del arquetipo (máximo 5 palabras, evocador y específico, puede tener artículo)",
  "description": "2 oraciones cortas en rioplatense, tono de broma cómplice, máximo 140 caracteres en total. Capturá la vibe del arquetipo, no hagas un reporte de datos. Ej: 'Tus viernes valen más que toda la semana. Cuando salís, salís en serio.' / 'El delivery es tu lengua materna. Rappi te conoce mejor que tu familia.'",
  "pattern_label": "el patrón dominante en 2-4 palabras (ej: 'salidas nocturnas', 'delivery diario', 'compras hormiga', 'ahorro constante')",
  "pattern_stat": "estadística corta que ilustra ese patrón (ej: '+280% vs promedio', '4x por semana', '60% del gasto')",
  "emoji": "1 solo emoji que representa el arquetipo",
  "accent_color_key": "uno de exactamente: amber, coral, green, blue, purple, teal, pink"
}

Reglas irrompibles:
- Usá vos, rioplatense natural
- El name tiene que ser memorable, no genérico. "El Gastador" es pésimo. "El Noctámbulo de Palermo" es bueno.
- La description son 2 oraciones, máximo 140 caracteres, tono divertido — no menciona montos ni porcentajes exactos
- accent_color_key: amber=salidas/diversión/social, coral=overspending/deuda/riesgo, green=ahorro/equilibrio/control, blue=tech/digital/suscripciones, purple=cultura/educación/arte, teal=viajes/transporte/movilidad, pink=moda/belleza/lifestyle
- Devolvés SOLO el JSON. Sin markdown. Sin explicaciones. Sin texto extra.
""".trimIndent()

object PersonalityRepository {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private var cachedPersonality: Personality? = null
    private var cacheFingerprint: String = "v3"

    suspend fun analyze(expenses: List<Expense>, income: Int, fixedMonthly: Int): Personality {
        val fingerprint = "${expenses.size}_${expenses.sumOf { it.amount }}"
        cachedPersonality?.let { cached ->
            if (fingerprint == cacheFingerprint) return cached
        }

        return try {
            val result = callGroq(expenses, income, fixedMonthly)
            cachedPersonality = result
            cacheFingerprint = fingerprint
            result
        } catch (e: Exception) {
            Log.w(TAG, "Personality analysis failed, using fallback", e)
            Fixtures.user.personality
        }
    }

    private suspend fun callGroq(expenses: List<Expense>, income: Int, fixedMonthly: Int): Personality {
        val response = ApiProvider.groqApi.chatCompletion(
            ChatCompletionRequest(
                model = MODEL,
                messages = listOf(
                    ChatMessage("system", SYSTEM_PROMPT),
                    ChatMessage("user", buildUserMessage(expenses, income, fixedMonthly)),
                ),
                temperature = 0.85,
            ),
        )

        val raw = response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Empty Groq response")

        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        val obj = json.parseToJsonElement(cleaned).jsonObject

        // pattern_label y pattern_stat mapean a weeklySpike y spikeStat del modelo existente
        return Personality(
            name = obj["name"]?.jsonPrimitive?.contentOrNull?.take(60) ?: "El Explorador",
            description = obj["description"]?.jsonPrimitive?.contentOrNull?.take(160) ?: "",
            weeklySpike = obj["pattern_label"]?.jsonPrimitive?.contentOrNull?.take(40)
                ?: obj["weekly_spike"]?.jsonPrimitive?.contentOrNull?.take(40)
                ?: "fin de semana",
            spikeStat = obj["pattern_stat"]?.jsonPrimitive?.contentOrNull?.take(20)
                ?: obj["spike_stat"]?.jsonPrimitive?.contentOrNull?.take(20)
                ?: "",
            emoji = obj["emoji"]?.jsonPrimitive?.contentOrNull?.take(4) ?: "✨",
            accentColorKey = obj["accent_color_key"]?.jsonPrimitive?.contentOrNull
                ?.let { if (it in VALID_COLORS) it else "amber" } ?: "amber",
        )
    }

    private fun buildUserMessage(expenses: List<Expense>, income: Int, fixedMonthly: Int): String {
        val spendingExpenses = expenses.filter { it.category != "Ingreso" }
        val totalSpent = spendingExpenses.sumOf { it.amount }
        val spendingRatio = if (income > 0) totalSpent.toDouble() / income else 0.0

        val categoryTotals = spendingExpenses
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }

        // Top merchants by frequency + by amount (both signals matter)
        val merchantByFreq = spendingExpenses
            .groupBy { it.merchant }
            .mapValues { (_, list) -> list.size }
            .entries.sortedByDescending { it.value }
            .take(6)

        val merchantByAmount = spendingExpenses
            .groupBy { it.merchant }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .take(6)

        val dayTotals = spendingExpenses
            .groupBy { dayNameEs(it.dateMillis) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }

        val avgTicket = if (spendingExpenses.isNotEmpty()) totalSpent / spendingExpenses.size else 0
        val maxSingleExpense = spendingExpenses.maxOfOrNull { it.amount } ?: 0

        val categoriesJson = categoryTotals.joinToString(",") { (cat, total) ->
            """{"category":"${cat.esc()}","total":$total}"""
        }
        val merchantFreqJson = merchantByFreq.joinToString(",") { (m, count) ->
            """{"merchant":"${m.esc()}","visits":$count}"""
        }
        val merchantAmountJson = merchantByAmount.joinToString(",") { (m, total) ->
            """{"merchant":"${m.esc()}","total":$total}"""
        }
        val daysJson = dayTotals.joinToString(",") { (day, total) ->
            """{"day":"$day","total":$total}"""
        }

        return buildString {
            append("""{"income":$income""")
            append(""","fixed_monthly":$fixedMonthly""")
            append(""","total_spent":$totalSpent""")
            append(""","spending_ratio":${String.format(java.util.Locale.US, "%.3f", spendingRatio)}""")
            append(""","expense_count":${spendingExpenses.size}""")
            append(""","avg_ticket":$avgTicket""")
            append(""","max_single_expense":$maxSingleExpense""")
            append(""","expenses_by_category":[$categoriesJson]""")
            append(""","top_merchants_by_frequency":[$merchantFreqJson]""")
            append(""","top_merchants_by_amount":[$merchantAmountJson]""")
            append(""","expenses_by_day_of_week":[$daysJson]""")
            append("}")
        }
    }

    private fun String.esc() = replace("\"", "'").replace("\\", "")

    private fun dayNameEs(millis: Long): String {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = millis
        return when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> "lunes"
            java.util.Calendar.TUESDAY -> "martes"
            java.util.Calendar.WEDNESDAY -> "miércoles"
            java.util.Calendar.THURSDAY -> "jueves"
            java.util.Calendar.FRIDAY -> "viernes"
            java.util.Calendar.SATURDAY -> "sábado"
            java.util.Calendar.SUNDAY -> "domingo"
            else -> "desconocido"
        }
    }
}
