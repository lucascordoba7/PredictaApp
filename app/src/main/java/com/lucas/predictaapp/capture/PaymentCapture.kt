package com.lucas.predictaapp.capture

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lucas.predictaapp.BuildConfig
import com.lucas.predictaapp.data.local.Session
import com.lucas.predictaapp.data.local.UserPreferencesRepository
import com.lucas.predictaapp.data.model.CategoryType
import com.lucas.predictaapp.data.model.Expense
import com.lucas.predictaapp.data.model.ExpenseCategories
import com.lucas.predictaapp.data.model.ExpenseSource
import com.lucas.predictaapp.data.repository.CategoryRepository
import com.lucas.predictaapp.data.repository.ExpensesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.Normalizer
import kotlin.math.abs

private const val TAG = "PaymentCapture"

// El mismo pago puede llegar por MP y por el banco, o haberse cargado a mano.
private const val DUPLICATE_WINDOW_MS = 3 * 60 * 60 * 1000L
private const val CROSS_CHANNEL_WINDOW_MS = 10 * 60 * 1000L
private const val MAX_SEEN = 300
private val RETRY_DELAYS_MS = listOf(0L, 5_000L, 20_000L, 60_000L)
private const val FREQUENT_WINDOW_MS = 60 * 24 * 60 * 60 * 1000L

/**
 * Pipeline de captura automática: texto (notificación o pantalla) → IA → gasto guardado →
 * aviso. Se guarda sin pedir confirmación (decisión del usuario: menos fricción, se corrige
 * después). En transferencias a personas el aviso pregunta en qué fue la plata.
 */
object PaymentCapture {
    private val Context.store by preferencesDataStore("payment_capture")
    private val SEEN = stringSetPreferencesKey("seen")

    // Serializa: si el mismo pago llega por dos vías a la vez, el segundo tiene que
    // ver ya insertado al primero para que el dedupe por monto lo descarte.
    private val mutex = Mutex()

    suspend fun process(
        context: Context,
        origin: CaptureOrigin,
        packageName: String,
        text: String,
        postTime: Long,
    ) = mutex.withLock {
        // Android re-postea la misma notificación al actualizarla, y una pantalla se vuelve a
        // mostrar al navegar: mismo texto = ya procesado.
        val fingerprint = "$packageName|$text".hashCode().toString()
        if (alreadySeen(context, fingerprint)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Ya procesado, se ignora")
            return@withLock
        }

        // El listener puede despertar el proceso antes de que PredictaApp restaure la sesión.
        if (!Session.isActive && !UserPreferencesRepository.restoreSession(context)) {
            Log.w(TAG, "Sin sesión: no se puede guardar el pago")
            return@withLock
        }

        val appName = PaymentApps.displayName(packageName)
        if (BuildConfig.DEBUG) Log.d(TAG, "[$appName/$origin] $text")

        val categories = CategoryRepository.categories.first()
        val expenseNames = categories.filter { it.type == CategoryType.EXPENSE }.map { it.name }
            .ifEmpty { ExpenseCategories.expenseNames }
        val incomeNames = categories.filter { it.type == CategoryType.INCOME }.map { it.name }
            .ifEmpty { listOf("Otros ingresos") }

        // Solo se marca como visto si la IA contestó: si falló la red o la API, que el próximo
        // evento de la misma pantalla/notificación lo pueda reintentar en vez de perderlo.
        val parsed = parseWithRetry(origin, appName, text, expenseNames, incomeNames) ?: return@withLock
        markSeen(context, fingerprint, postTime)
        val payment = parsed.getOrNull() ?: run {
            if (BuildConfig.DEBUG) Log.d(TAG, "La IA no lo tomó como pago")
            return@withLock
        }

        val source = when (origin) {
            CaptureOrigin.NOTIFICATION -> ExpenseSource.NOTIFICATION
            CaptureOrigin.SCREEN -> ExpenseSource.SCREEN
        }
        val key = counterpartyKey(payment.counterparty)
        findDuplicate(payment, key, postTime, source)?.let {
            if (BuildConfig.DEBUG) Log.d(TAG, "Duplicado de #${it.id} (${it.merchant}), se ignora")
            return@withLock
        }

        // Transferencia a una persona: se pregunta en qué fue. Se guarda igual con la mejor
        // categoría disponible para que el gasto no se pierda si el usuario no contesta.
        // Si ya se le transfirió, se repite cómo quedó la última vez (nombre y categoría).
        val asksPurpose = payment.toPerson && !payment.incoming
        val remembered = if (asksPurpose) ExpensesRepository.lastCaptureFor(key) else null
        val suggestions = if (asksPurpose) suggestCategories(payment, postTime, remembered?.category) else emptyList()
        val category = suggestions.firstOrNull() ?: payment.category
        val merchant = remembered?.merchant ?: payment.counterparty

        val id = ExpensesRepository.addByName(
            merchant = merchant,
            categoryName = category,
            amount = payment.amount,
            dateMillis = postTime,
            income = payment.incoming,
            source = source,
        )
        if (asksPurpose) {
            ExpensesRepository.rememberCapture(id, key, postTime)
            CaptureNotifications.askPurpose(
                context, id, payment, category, suggestions.drop(1),
                savedAs = merchant.takeIf { it != payment.counterparty },
            )
        } else {
            CaptureNotifications.showCaptured(context, id, payment, category, appName)
        }
    }

    /**
     * Destinatario conocido → la categoría de su última transferencia ([remembered]) y las
     * que ya se usaron con él. Nuevo → lo que adivinó la IA y, de relleno, las categorías más
     * usadas últimamente. La primera de la lista es la que se guarda; las siguientes van como botones.
     */
    private suspend fun suggestCategories(payment: DetectedPayment, now: Long, remembered: String?): List<String> {
        val known = (listOfNotNull(remembered) + ExpensesRepository.topCategoriesFor(payment.counterparty, limit = 3))
            .distinct()
        val guess = payment.category.takeIf { it != "Otros" }
        val frequent = ExpensesRepository.topCategoriesSince(now - FREQUENT_WINDOW_MS, limit = 3)
        val ordered = (known + listOfNotNull(guess) + frequent).distinct()
        // Sin historial ni pista de la IA, no se inventa: queda en "Otros" y los botones sugieren.
        return if (known.isEmpty() && guess == null) listOf("Otros") + ordered else ordered
    }

    /**
     * Mismo monto y (a) nombre parecido dentro de 3 h: lo cargó a mano o ya se capturó; o
     * (b) otra vía (aviso vs. pantalla, MP vs. banco) en pocos minutos: la misma operación
     * con el nombre escrito distinto. Mismo monto a otra persona por la misma vía no es duplicado.
     * El nombre se compara también contra el destinatario original: el gasto pudo renombrarse.
     */
    private suspend fun findDuplicate(payment: DetectedPayment, key: String, postTime: Long, source: ExpenseSource): Expense? {
        val sameRecipient = ExpensesRepository.capturedIdsFor(key).toSet()
        return ExpensesRepository.sameAmountNear(payment.amount, postTime, DUPLICATE_WINDOW_MS).firstOrNull {
            it.id in sameRecipient ||
                namesOverlap(it.merchant, payment.counterparty) ||
                (it.source != source && abs(it.dateMillis - postTime) < CROSS_CHANNEL_WINDOW_MS)
        }
    }

    private fun words(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .split(Regex("[^a-z0-9]+"))
        .filter { it.isNotEmpty() }

    /** "Adam Gonzalo Javier" y "Gonzalo Javier Adam" → "adam gonzalo javier". */
    private fun counterpartyKey(name: String) = words(name).sorted().joinToString(" ")

    /** "Cordoba Lucas Emmanuel" ~ "Lucas Emmanuel", "Café" ~ "Cafe Martinez". */
    private fun namesOverlap(a: String, b: String): Boolean {
        fun significant(s: String) = words(s).filter { it.length >= 3 }.toSet()
        return (significant(a) intersect significant(b)).isNotEmpty()
    }

    /**
     * Reintenta ante 429 o red caída. null = fallaron todos los intentos; un Result con
     * null adentro = la IA contestó que no es un pago.
     */
    private suspend fun parseWithRetry(
        origin: CaptureOrigin,
        appName: String,
        text: String,
        expenseNames: List<String>,
        incomeNames: List<String>,
    ): Result<DetectedPayment?>? {
        RETRY_DELAYS_MS.forEachIndexed { attempt, wait ->
            delay(wait)
            try {
                return Result.success(PaymentTextParser.parse(origin, appName, text, expenseNames, incomeNames))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Intento ${attempt + 1} fallido interpretando $appName", e)
            }
        }
        return null
    }

    private suspend fun alreadySeen(context: Context, fingerprint: String): Boolean =
        context.store.data.first()[SEEN].orEmpty().any { it.endsWith("|$fingerprint") }

    /** Guarda "postTime|fingerprint" y conserva solo los [MAX_SEEN] más recientes. */
    private suspend fun markSeen(context: Context, fingerprint: String, postTime: Long) {
        context.store.edit { prefs ->
            val updated = prefs[SEEN].orEmpty() + "$postTime|$fingerprint"
            prefs[SEEN] = updated
                .sortedByDescending { it.substringBefore('|').toLongOrNull() ?: 0L }
                .take(MAX_SEEN)
                .toSet()
        }
    }
}
