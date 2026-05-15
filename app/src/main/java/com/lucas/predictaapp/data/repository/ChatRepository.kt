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
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val TAG = "ChatRepository"
private const val MODEL = "llama-3.3-70b-versatile"

private fun buildSystemPrompt(today: String, expenseCategories: List<String>) = """
Sos Predicta, un asistente financiero argentino. Extraés gastos e ingresos de texto natural.
Hoy es $today.

Devolvés UN JSON puro, sin markdown, de uno de estos formatos:

FORMATO A — un gasto:
{"kind":"expense","merchant":"nombre","category":"CATEGORÍA","amount":number,"date":"YYYY-MM-DD"}

FORMATO B — un ingreso:
{"kind":"income","merchant":"descripción","amount":number,"date":"YYYY-MM-DD"}

FORMATO C — múltiples gastos en un mismo mensaje:
{"kind":"expenses","items":[{"merchant":"nombre","category":"CATEGORÍA","amount":number,"date":"YYYY-MM-DD"},…]}

FORMATO D — necesita aclaración:
{"kind":"clarify","reason":"qué falta","question":"pregunta","suggestions":[{"label":"...","rewritten_intent":"..."}]}

FORMATO E — no financiero:
{"kind":"unknown","reply":"respuesta amigable en 1 oración, rioplatense"}

Categorías válidas (usá EXACTAMENTE una de estas, sin variantes):
${expenseCategories.joinToString("|")}

Guía de categorías con ejemplos de comercios (para ayudarte a clasificar):
- Salidas: restaurantes, parrillas, bares, cafés, boliches, teatro, cine, recitales.
  Ej: Don Julio, El Federal, La Cabrera, Osaka, Chila, Tegui, Cafe Martinez, Starbucks, La Biela, Gran Bar Danzón, Victoria Brown, cualquier parrilla/resto/bar
- Delivery: Rappi, PedidosYa, Glovo, apps de delivery de comida
- Supermercados: Carrefour, Coto, Jumbo, Disco, Día, Vea, La Anónima, Walmart, Lidl
- Transporte: Uber, Cabify, DiDi, InDrive, SUBE, trenes, colectivo, peaje
- Salud: farmacias, médicos, clínicas, Farmacity, laboratorios
- Tecnología: Apple, Samsung, Mercado Libre (tech), Amazon, tiendas de electrónica
- Suscripciones: Netflix, Spotify, Disney+, HBO, servicios digitales recurrentes
- Ropa: Zara, H&M, Nike, Adidas, shoppings, tiendas de indumentaria
- Educación: universidades, cursos, libros, Udemy, Coursera

Regla clave: si el comercio es claramente un restaurant/bar/café/parrilla aunque no lo conozcas, usá la categoría de Salidas (o equivalente en la lista), nunca Supermercados.

Modismos argentinos de montos (convertí antes de poner en "amount"):
- "luca" / "lucas" = 1000 (ej: "10 lucas" → 10000, "media luca" → 500)
- "palo" / "palos" = 1.000.000 (ej: "2 palos" → 2000000)
- "k" / "K" como sufijo = × 1000 (ej: "100k" → 100000)
- "M" como sufijo = × 1.000.000 (ej: "1.5M" → 1500000)
- "mangos" / "pesos" = 1:1 (ej: "200 mangos" → 200)
- Combinaciones: "5 palos y medio" → 5500000, "2 lucas y pico" → ~2100

Fechas (campo "date", formato YYYY-MM-DD, hoy = $today):
- "hoy" / sin referencia temporal → omitir el campo "date" (o poner null)
- "ayer" → fecha de ayer
- "anteayer" → hace 2 días
- "el lunes", "el martes", etc. → el día más reciente con ese nombre
- "hace 3 días" → restar 3 días a hoy
- Si menciona una fecha futura → pedí aclaración

Reglas:
- Si no se menciona monto → usá FORMATO D (clarify), nunca inventes un número
- Si mencionan USD/dólar → pedí aclaración
- Si hay 2 o más gastos en el mensaje → usá FORMATO C (expenses), nunca respondas solo el primero
- Monto siempre en pesos argentinos
- Usás "vos", español rioplatense natural
- 1 solo JSON, sin texto extra
- La categoría debe ser exactamente una de la lista
- Si en mensajes anteriores ya se aclaró el comercio o el monto, usá esa info para completar el gasto sin volver a preguntar
""".trimIndent()

object ChatRepository {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val isoFmt = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun extract(
        userText: String,
        expenseCategories: List<String> = ExpenseCategories.expenseNames,
        history: List<ChatMessage> = emptyList(),
    ): Triple<ExpenseExtraction, String?, String> {
        return try {
            callGroq(userText, expenseCategories, history)
        } catch (e: Exception) {
            Log.w(TAG, "Groq call failed", e)
            Triple(ExpenseExtraction.Unknown, "No pude conectarme. Intentá de nuevo.", "")
        }
    }

    private fun parseDateInfo(dateIso: String?): Pair<Long, String> {
        if (dateIso == null) return System.currentTimeMillis() to "hoy"
        return try {
            val ld = LocalDate.parse(dateIso, isoFmt)
            val today = LocalDate.now()
            val millis = ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val daysAgo = ChronoUnit.DAYS.between(ld, today)
            val label = when (daysAgo) {
                0L -> "hoy"
                1L -> "ayer"
                2L -> "anteayer"
                in 3..6 -> "hace $daysAgo días"
                else -> "el ${ld.dayOfMonth}/${ld.monthValue}"
            }
            millis to label
        } catch (e: Exception) {
            System.currentTimeMillis() to "hoy"
        }
    }

    private fun parseExpenseItem(obj: kotlinx.serialization.json.JsonObject): ExpenseExtraction.Expense {
        val merchant = obj["merchant"]?.jsonPrimitive?.contentOrNull ?: "Gasto"
        val category = obj["category"]?.jsonPrimitive?.contentOrNull ?: "Otro"
        val amount = obj["amount"]?.jsonPrimitive?.intOrNull ?: 0
        val dateIso = obj["date"]?.jsonPrimitive?.contentOrNull
        val (dateMillis, whenLabel) = parseDateInfo(dateIso)
        return ExpenseExtraction.Expense(merchant, category, amount, dateMillis = dateMillis, whenLabel = whenLabel)
    }

    private suspend fun callGroq(
        userText: String,
        expenseCategories: List<String>,
        history: List<ChatMessage>,
    ): Triple<ExpenseExtraction, String?, String> {
        val today = LocalDate.now().format(isoFmt)
        val messages = buildList {
            add(ChatMessage("system", buildSystemPrompt(today, expenseCategories)))
            addAll(history)
            add(ChatMessage("user", userText))
        }
        val response = ApiProvider.groqApi.chatCompletion(
            ChatCompletionRequest(model = MODEL, messages = messages),
        )
        val raw = response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Empty response")

        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        val obj = json.parseToJsonElement(cleaned).jsonObject
        val kind = obj["kind"]?.jsonPrimitive?.contentOrNull ?: "unknown"

        val result: Pair<ExpenseExtraction, String?> = when (kind) {
            "expense" -> parseExpenseItem(obj) to null
            "income" -> {
                val merchant = obj["merchant"]?.jsonPrimitive?.contentOrNull ?: "Ingreso"
                val amount = obj["amount"]?.jsonPrimitive?.intOrNull ?: 0
                val dateIso = obj["date"]?.jsonPrimitive?.contentOrNull
                val (dateMillis, whenLabel) = parseDateInfo(dateIso)
                ExpenseExtraction.Income(merchant, amount = amount, dateMillis = dateMillis, whenLabel = whenLabel) to null
            }
            "expenses" -> {
                val items = obj["items"]?.jsonArray?.map { parseExpenseItem(it.jsonObject) } ?: emptyList()
                if (items.isEmpty()) ExpenseExtraction.Unknown to "No entendí los gastos. ¿Podés reescribirlos?"
                else ExpenseExtraction.MultiExpense(items) to null
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
        return Triple(result.first, result.second, raw)
    }
}
