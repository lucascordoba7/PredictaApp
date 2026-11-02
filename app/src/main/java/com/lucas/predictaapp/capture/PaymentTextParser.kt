package com.lucas.predictaapp.capture

import com.lucas.predictaapp.data.remote.ApiProvider
import com.lucas.predictaapp.data.remote.ChatCompletionRequest
import com.lucas.predictaapp.data.remote.ChatMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import kotlin.math.roundToInt

private const val MODEL = "openai/gpt-oss-120b"

/** De dónde salió el texto: cambia el contexto que recibe la IA. */
enum class CaptureOrigin {
    /** Notificación push de la app de pagos (PaymentNotificationListener). */
    NOTIFICATION,

    /** Texto visible en pantalla, leído por accesibilidad (PaymentScreenReader). */
    SCREEN,
}

/** Un movimiento de plata ya realizado, leído de una notificación o de una pantalla. */
data class DetectedPayment(
    val incoming: Boolean,
    val amount: Int,
    val counterparty: String,
    val toPerson: Boolean,
    val category: String,
)

private fun notificationIntro(appName: String) = """
Recibís el texto de una notificación de $appName, una app de pagos/banco argentina.
Decidí si informa un movimiento de plata YA REALIZADO en la cuenta del usuario.
""".trimIndent()

private fun screenIntro(appName: String, today: String) = """
Recibís el texto visible en pantalla de $appName, una app de pagos/banco argentina (leído por accesibilidad,
un fragmento por línea). Hoy es $today.
Decidí si es la pantalla de CONFIRMACIÓN de una operación que el usuario ACABA de completar
(ej. "¡Listo! Transferiste...", "Pagaste...", el comprobante que aparece al terminar).

Si el texto empieza con "OPERACIÓN CONFIRMADA", la operación ya se completó y lo que sigue es su pantalla de
revisión: es "payment", con el monto (el "Total") y el destinatario de esa revisión.

Además de las reglas de abajo, es "ignore":
- Pantallas de revisión ANTES de confirmar ("Vas a transferir", "Revisá los datos", botones "Transferir"/"Confirmar"/"Pagar"),
  salvo que vengan después de "OPERACIÓN CONFIRMADA".
- Inicio, saldo, listados de actividad o movimientos (varias operaciones), búsqueda de contactos.
- El detalle o comprobante de una operación con fecha distinta de hoy.
""".trimIndent()

private fun buildPrompt(
    intro: String,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
) = intro + "\n\n" + """

Devolvés UN JSON puro, de uno de estos formatos:

{"kind":"payment","direction":"out"|"in","amount":number,"counterparty":"nombre","counterparty_type":"merchant"|"person","category":"CATEGORÍA"}
{"kind":"ignore"}

Es "payment":
- Pagos con QR, tarjeta de débito o crédito, link de pago, débitos automáticos ya cobrados → direction "out".
- Transferencias enviadas → "out". Transferencias o dinero recibido, sueldo acreditado → "in".

Es "ignore" (no inventes un pago):
- Promos, descuentos, ofertas, cuotas sin interés, publicidad.
- Recordatorios o avisos de vencimiento, pagos programados o futuros, resumen disponible.
- Pago del resumen de la tarjeta de crédito (las compras ya se registraron una por una).
- Movimientos entre cuentas propias, inversiones, plazo fijo, fondos, rendimientos diarios.
- Pagos rechazados, fallidos o pendientes; solicitudes o pedidos de dinero.
- Códigos de seguridad, inicios de sesión, cambios de datos.
- Montos en dólares u otra moneda que no sea pesos.
- Si no hay un monto claro.

Campos:
- amount: pesos enteros. Formato argentino: "$ 4.500,50" → 4501, "$12.300" → 12300.
  También puede venir en palabras: "1500 pesos con 50 centavos" → 1501, "un Peso con 50 Centavos" → 2.
- counterparty: el comercio o la persona, nombre limpio y Capitalizado: sin "a", "en", "de" adelante y sin
  sufijos de sucursal, terminal, ciudad o códigos ("COTO SUC 123" → "Coto", "MERPAGO*KIOSCOLUNA" → "Kiosco Luna").
- counterparty_type: "person" si es una persona (nombre y apellido, transferencia a alguien); "merchant" si es comercio, empresa o servicio.
- category, si direction es "out": EXACTAMENTE una de: ${expenseCategories.joinToString("|")}
  Clasificá por el rubro real del comercio. Transferencia a una persona sin más contexto → "Otros".
- category, si direction es "in": EXACTAMENTE una de: ${incomeCategories.joinToString("|")}

Un solo JSON, sin texto extra.
""".trimIndent()

/** Qué dijo el usuario al responder "¿en qué fue?" sobre una transferencia. */
sealed class PurposeAnswer {
    data class Category(val name: String) : PurposeAnswer()
    data object NotExpense : PurposeAnswer()
}

private fun purposePrompt(expenseCategories: List<String>) = """
El usuario hizo una transferencia y te cuenta en pocas palabras para qué fue. Clasificalo.

Devolvés UN JSON puro, de uno de estos formatos:
{"kind":"category","category":"CATEGORÍA"}
{"kind":"not_expense"}

- "not_expense" si dice que no es un gasto: plata a una cuenta propia, un préstamo que va a volver,
  devolverle plata a alguien que la puso por él, "no es gasto", "es mío", "ahorro".
- Si no, category es EXACTAMENTE una de: ${expenseCategories.joinToString("|")}
  Ej: "asado del sábado" → la de comidas/salidas; "alquiler" → la de hogar o vivienda; "regalo de cumple" → Regalos.
  Si nada calza, "Otros".

Un solo JSON, sin texto extra.
""".trimIndent()

object PaymentTextParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** null = el texto no es un movimiento de plata (o no se pudo interpretar). */
    suspend fun parse(
        origin: CaptureOrigin,
        appName: String,
        text: String,
        expenseCategories: List<String>,
        incomeCategories: List<String>,
    ): DetectedPayment? {
        val intro = when (origin) {
            CaptureOrigin.NOTIFICATION -> notificationIntro(appName)
            CaptureOrigin.SCREEN -> screenIntro(appName, LocalDate.now().toString())
        }
        val response = ApiProvider.groqApi.chatCompletion(
            ChatCompletionRequest(
                model = MODEL,
                messages = listOf(
                    ChatMessage("system", buildPrompt(intro, expenseCategories, incomeCategories)),
                    ChatMessage("user", text),
                ),
                temperature = 0.0,
            ),
        )
        val raw = response.choices.firstOrNull()?.message?.content ?: return null
        val obj = json.parseToJsonElement(raw.trim()).jsonObject
        if (obj["kind"]?.jsonPrimitive?.contentOrNull != "payment") return null

        val amount = obj["amount"]?.jsonPrimitive?.doubleOrNull?.roundToInt() ?: return null
        if (amount <= 0) return null
        val incoming = obj["direction"]?.jsonPrimitive?.contentOrNull == "in"
        return DetectedPayment(
            incoming = incoming,
            amount = amount,
            counterparty = obj["counterparty"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                ?: appName,
            toPerson = obj["counterparty_type"]?.jsonPrimitive?.contentOrNull == "person",
            category = obj["category"]?.jsonPrimitive?.contentOrNull
                ?: if (incoming) "Otros ingresos" else "Otros",
        )
    }

    /** null = no se pudo interpretar (el caller deja la categoría como estaba). */
    suspend fun classifyPurpose(
        answer: String,
        counterparty: String,
        expenseCategories: List<String>,
    ): PurposeAnswer? {
        val response = ApiProvider.groqApi.chatCompletion(
            ChatCompletionRequest(
                model = MODEL,
                messages = listOf(
                    ChatMessage("system", purposePrompt(expenseCategories)),
                    ChatMessage("user", "Transferencia a $counterparty. Para qué fue: $answer"),
                ),
                temperature = 0.0,
            ),
        )
        val raw = response.choices.firstOrNull()?.message?.content ?: return null
        val obj = json.parseToJsonElement(raw.trim()).jsonObject
        return when (obj["kind"]?.jsonPrimitive?.contentOrNull) {
            "not_expense" -> PurposeAnswer.NotExpense
            "category" -> obj["category"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it in expenseCategories }
                ?.let { PurposeAnswer.Category(it) }
            else -> null
        }
    }
}
