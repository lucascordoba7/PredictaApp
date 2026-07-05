package com.lucas.predictaapp.data.repository

import android.util.Log
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.data.model.ExpenseExtraction
import com.lucas.predictaapp.data.model.ExpenseSuggestion
import com.lucas.predictaapp.data.remote.AnthropicMessage
import com.lucas.predictaapp.data.remote.AnthropicMessageRequest
import com.lucas.predictaapp.data.remote.ApiProvider
import com.lucas.predictaapp.data.remote.ChatCompletionRequest
import com.lucas.predictaapp.data.remote.ChatMessage
import com.lucas.predictaapp.data.remote.ContentBlock
import com.lucas.predictaapp.data.remote.ImageSource
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

// Visión para tickets: Haiku lee comprobantes bien y es el más barato con imagen.
private const val VISION_MODEL = "claude-haiku-4-5-20251001"

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

FORMATO F — suscripción recurrente:
{"kind":"subscription","service":"nombre del servicio","monthly":number}

Categorías válidas (usá EXACTAMENTE una de estas, sin variantes):
${expenseCategories.joinToString("|")}

Cómo clasificar la categoría:
- Elegí SIEMPRE una de la lista de "Categorías válidas" de arriba, la que mejor describa el gasto. Nunca inventes ni uses una que no esté en la lista, ni una variante (ni singular/plural distinto).
- Clasificá por el rubro real del comercio/gasto (qué se compra), no por el nombre literal del comercio. Ej: una parrilla, restaurant, bar o café es comida fuera de casa, no supermercado; un super (Carrefour, Coto, Día) es compra de supermercado; Uber/SUBE/peaje es transporte; Netflix/Spotify es un servicio digital recurrente.
- Si ninguna categoría calza con claridad, elegí la más cercana de la lista; si no hay ninguna razonable, usá una categoría genérica de la lista (p. ej. "Otros" si existe).

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

Reglas para el campo "merchant":
- Si el usuario menciona un comercio o local concreto → usá ese nombre (ej: "Don Julio" → "Don Julio", "Carrefour" → "Carrefour")
- Si el usuario NO menciona un comercio → usá sus palabras como descripción, nunca infertas el tipo de local
  Ej: "compré cigarrillos 500" → merchant: "Cigarrillos" (no "Kiosco")
  Ej: "café 1200" → merchant: "Café" (no "Cafetería")
  Ej: "cargué nafta 20k" → merchant: "Nafta" (no "Shell" ni "YPF")
  Ej: "farmacia 3800" → merchant: "Farmacia" (no "Farmacity")

Reglas:
- Si no se menciona monto → usá FORMATO D (clarify), nunca inventes un número
- Si mencionan USD/dólar → pedí aclaración
- Si hay 2 o más gastos en el mensaje → usá FORMATO C (expenses), nunca respondas solo el primero
- Monto siempre en pesos argentinos
- Usás "vos", español rioplatense natural
- 1 solo JSON, sin texto extra
- La categoría debe ser exactamente una de la lista
- Si en mensajes anteriores ya se aclaró el comercio o el monto, usá esa info para completar el gasto sin volver a preguntar

Regla de suscripciones (FORMATO F):
- Usá FORMATO F SOLO si el usuario menciona explícitamente que es recurrente, con frases como: "/mes", "al mes", "mensual", "suscripción", "me suscribí a", "me suscribo a", "tengo X por Y al mes", "abono", "abonar a", "pagar todos los meses".
  Ej: "Me suscribí a Netflix 4500" → FORMATO F (service: "Netflix", monthly: 4500)
  Ej: "Spotify 2500 al mes" → FORMATO F (service: "Spotify", monthly: 2500)
  Ej: "Tengo HBO por 3200/mes" → FORMATO F (service: "HBO", monthly: 3200)
- Si el usuario solo dice "Pagué Netflix 4500" o "Netflix 4500" SIN palabras de recurrencia → es un gasto puntual: usá FORMATO A y clasificalo con la categoría de la lista que mejor corresponda a servicios digitales/suscripciones (si no hay una específica, la más cercana o "Otros").
- Si menciona suscripción/recurrencia PERO falta el servicio o el monto → FORMATO D (clarify).
- En FORMATO F el campo "service" lleva el nombre del servicio capitalizado (Ej: "Netflix", "Spotify", "Disney+", "Apple Music").
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

        val (extraction, reply) = parseRaw(raw)
        return Triple(extraction, reply, raw)
    }

    /**
     * Extrae un gasto desde la FOTO de un ticket/comprobante (Claude con visión).
     * Devuelve el mismo Triple que [extract]: el resto del pipeline no distingue origen.
     */
    suspend fun extractFromTicket(
        imageBase64: String,
        expenseCategories: List<String> = ExpenseCategories.expenseNames,
    ): Triple<ExpenseExtraction, String?, String> {
        return try {
            val today = LocalDate.now().format(isoFmt)
            val system = buildSystemPrompt(today, expenseCategories) + """

El usuario manda una FOTO de un ticket, recibo o comprobante de pago.
- Extraé comercio, monto TOTAL (no los ítems) y fecha si aparece → FORMATO A.
- Si la imagen no es un comprobante o el monto no se lee → FORMATO D (clarify) explicando qué falta.
"""
            val response = ApiProvider.anthropicApi.createMessage(
                AnthropicMessageRequest(
                    model = VISION_MODEL,
                    maxTokens = 1024,
                    system = system,
                    messages = listOf(
                        AnthropicMessage(
                            role = "user",
                            content = listOf(
                                ContentBlock.Image(ImageSource(mediaType = "image/jpeg", data = imageBase64)),
                                ContentBlock.Text("Extraé el gasto de este comprobante. Un solo JSON."),
                            ),
                        ),
                    ),
                ),
            )
            val raw = response.content.firstNotNullOfOrNull { it.text }?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Empty response")
            val (extraction, reply) = parseRaw(raw)
            Triple(extraction, reply, raw)
        } catch (e: Exception) {
            Log.w(TAG, "Vision call failed", e)
            Triple(ExpenseExtraction.Unknown, "No pude leer el ticket. Probá con una foto más nítida y de frente.", "")
        }
    }

    private fun parseRaw(raw: String): Pair<ExpenseExtraction, String?> {
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
            "subscription" -> {
                val service = obj["service"]?.jsonPrimitive?.contentOrNull ?: "Suscripción"
                val monthly = obj["monthly"]?.jsonPrimitive?.intOrNull ?: 0
                if (monthly <= 0) {
                    ExpenseExtraction.Clarify(
                        reason = "Falta monto mensual",
                        question = "¿Cuánto pagás al mes por $service?",
                        suggestions = emptyList(),
                    ) to null
                } else {
                    ExpenseExtraction.Subscription(service = service, monthly = monthly) to null
                }
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
        return result
    }
}
