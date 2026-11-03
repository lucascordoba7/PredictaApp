package com.lucas.predictaapp.capture

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import com.lucas.predictaapp.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "PaymentScreenReader"

// Las pantallas se arman de a pedazos (MP usa webviews): se lee cuando dejan de cambiar.
private const val SETTLE_MS = 800L
private const val LATE_READ_MS = 2500L

// Tope de espera: pantallas con animaciones o banners que rotan disparan eventos sin parar,
// y sin tope el debounce se reiniciaba siempre y la pantalla nunca se leía.
private const val MAX_WAIT_MS = 2000L
private const val COOLDOWN_MS = 2 * 60 * 1000L
private const val MAX_NODES = 1500
private const val MAX_CHARS = 3000

// Una confirmación tiene el "¡Listo!/Transferiste" como título, no perdido en una lista.
private const val HEADER_PIECES = 8
private const val MAX_AMOUNTS = 3

// MP expone los montos para lectores de pantalla en palabras ("un Peso con 50 Centavos",
// "17534 pesos con 83 centavos"), no con "$".
private val AMOUNT = Regex(
    """\$\s?\d[\d.]*(,\d{1,2})?|\b\d[\d.]*(,\d{1,2})?\s+pesos?\b|\bun peso\b""",
    RegexOption.IGNORE_CASE,
)
private val SUCCESS_MARKERS = listOf(
    "listo", "transferiste", "pagaste", "enviaste", "le enviaste", "realizada", "realizado",
    "exitosa", "exitoso", "aprobado", "aprobada", "comprobante",
)

// Botones que solo aparecen cuando la operación ya terminó. En MP el título y el monto de
// la confirmación ("Transferiste $ 0,88") no son accesibles, pero estos botones sí.
private val DONE_ACTIONS = listOf("hacer otra transferencia", "compartir comprobante")

// Pantallas previas de la operación: de acá sale el monto que la confirmación no expone.
private val REVIEW_MARKERS = listOf("ingresá el monto", "revisá", "vas a transferir", "total", "motivo")
private const val REVIEW_TTL_MS = 5 * 60 * 1000L

// Lo entiende el prompt de PaymentTextParser (screenIntro).
const val CONFIRMED_HEADER = "OPERACIÓN CONFIRMADA (la app muestra \"Compartir comprobante\"). Revisión de la operación:"
private const val MAX_REVIEW_PIECES = 40

/**
 * Lee la pantalla de confirmación de Mercado Pago / Santander cuando el usuario termina
 * una transferencia o un pago dentro de la app (esas operaciones no generan notificación).
 *
 * Privacidad: el sistema solo le entrega eventos de los paquetes listados en
 * `res/xml/payment_screen_reader.xml`, y el texto solo sale del teléfono (hacia la IA)
 * si parece una confirmación según [looksLikeConfirmation], junto con la revisión previa
 * de esa operación cuando hace falta el monto. Saldo, inicio y actividad se descartan acá.
 */
class PaymentScreenReader : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pending: Job? = null
    private var burstStartedAt = 0L
    private var lastSignature: String? = null
    private var lastSignatureAt = 0L

    // Última pantalla de revisión/monto vista (solo en memoria). Solo sale del teléfono
    // pegada a la confirmación de esa misma operación.
    private var lastReview: Review? = null

    private class Review(val pkg: String, val pieces: List<String>, val at: Long)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (!PaymentApps.isPaymentApp(pkg)) return
        val now = SystemClock.uptimeMillis()
        val waiting = pending?.isActive == true
        // Si la ráfaga de eventos ya lleva el tope, se deja correr la lectura pendiente.
        if (waiting && now - burstStartedAt >= MAX_WAIT_MS) return
        if (!waiting) burstStartedAt = now
        pending?.cancel()
        pending = scope.launch {
            delay(SETTLE_MS)
            if (readScreen(pkg)) return@launch
            // Las WebViews de MP completan su contenido después de los eventos nativos.
            delay(LATE_READ_MS)
            readScreen(pkg)
        }
    }

    override fun onInterrupt() = Unit

    // Diagnóstico (solo debug): `adb shell am broadcast -a com.lucas.predictaapp.DEBUG_DUMP_SCREEN`
    // vuelca el árbol que recibe el servicio, para compararlo con `uiautomator dump`.
    private val dumpReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = dumpTree()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        if (BuildConfig.DEBUG) {
            ContextCompat.registerReceiver(
                this, dumpReceiver, IntentFilter(ACTION_DEBUG_DUMP), ContextCompat.RECEIVER_EXPORTED,
            )
        }
    }

    override fun onDestroy() {
        isRunning = false
        if (BuildConfig.DEBUG) runCatching { unregisterReceiver(dumpReceiver) }
        scope.cancel()
        super.onDestroy()
    }

    private fun dumpTree() {
        windows.forEach { w ->
            val root = w.root ?: run {
                Log.d(TAG, "DUMP ventana tipo=${w.type} sin root")
                return@forEach
            }
            Log.d(TAG, "DUMP ventana ${root.packageName} tipo=${w.type}")
            val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>().apply { addLast(root to 0) }
            var count = 0
            while (stack.isNotEmpty() && count < 400) {
                val (node, depth) = stack.removeLast()
                count++
                val label = (node.text ?: node.contentDescription)?.toString()?.take(50)?.replace("\n", " ")
                Log.d(
                    TAG,
                    "DUMP ${"  ".repeat(depth.coerceAtMost(20))}${node.className?.toString()?.substringAfterLast('.')}" +
                        " hijos=${node.childCount} visible=${node.isVisibleToUser} ${label ?: ""}",
                )
                for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let { stack.addLast(it to depth + 1) }
            }
        }
    }

    /** true = era una confirmación (ya se mandó a procesar o se descartó por repetida). */
    private fun readScreen(pkg: String): Boolean {
        // No alcanza con rootInActiveWindow: la burbuja flotante "Hablar con Mago" de MP es
        // otra ventana y suele quedar como activa. Se leen todas las de la app, la más grande
        // (el contenido) primero. Si el usuario cambió de app durante la espera, no hay ninguna.
        val roots = windows
            .mapNotNull { w -> w.root?.takeIf { it.packageName?.toString() == pkg }?.let { w to it } }
            .sortedByDescending { (w, _) -> Rect().also(w::getBoundsInScreen).let { it.width() * it.height() } }
            .map { it.second }
        if (roots.isEmpty()) return false

        val pieces = roots.flatMap(::collectText)
        if (BuildConfig.DEBUG) logWindows(pkg, pieces.size)
        val now = System.currentTimeMillis()

        val done = isDone(pieces)
        if (!done && !looksLikeConfirmation(pieces)) {
            if (looksLikeReview(pieces)) lastReview = Review(pkg, pieces.take(MAX_REVIEW_PIECES), now)
            return false
        }

        // Confirmación de MP por sus botones: el título y el monto no son accesibles y la
        // pantalla trae publicidad con precios. Se manda solo la revisión de esa operación,
        // que tiene monto y destinatario, en vez del texto de la confirmación.
        val previous = lastReview?.takeIf { it.pkg == pkg && now - it.at < REVIEW_TTL_MS }
        val (source, text) = when {
            done && previous != null ->
                previous.pieces to "$CONFIRMED_HEADER\n\n${previous.pieces.joinToString("\n")}"
            amountsIn(pieces).isNotEmpty() -> pieces to pieces.joinToString("\n")
            else -> {
                if (BuildConfig.DEBUG) Log.d(TAG, "Confirmación sin monto y sin revisión reciente: se ignora")
                return true
            }
        }

        // La animación de "¡Listo!" re-dispara eventos: misma operación = mismos montos.
        val signature = pkg + amountsIn(source).toSortedSet()
        if (signature == lastSignature && now - lastSignatureAt < COOLDOWN_MS) return true
        lastSignature = signature
        lastSignatureAt = now
        lastReview = null
        scope.launch(Dispatchers.IO) {
            PaymentCapture.process(applicationContext, CaptureOrigin.SCREEN, pkg, text.take(MAX_CHARS), now)
        }
        return true
    }

    private fun amountsIn(pieces: List<String>): Set<String> =
        AMOUNT.findAll(pieces.joinToString(" ")).map { it.value }.toSet()

    private fun looksLikeReview(pieces: List<String>): Boolean {
        val all = pieces.joinToString(" ").lowercase()
        return REVIEW_MARKERS.any { it in all }
    }

    /** Diagnóstico (solo debug): qué ventanas ve el servicio y de qué tamaño. */
    private fun logWindows(pkg: String, pieceCount: Int) {
        val detail = windows.joinToString { w ->
            val r = Rect().also(w::getBoundsInScreen)
            "${w.root?.packageName ?: "-"} tipo=${w.type} ${r.width()}x${r.height()}"
        }
        Log.d(TAG, "[$pkg] textos=$pieceCount ventanas: $detail")
    }

    /** Textos visibles en orden de lectura (DFS), sin repetidos consecutivos. */
    private fun collectText(root: AccessibilityNodeInfo): List<String> {
        val out = mutableListOf<String>()
        val stack = ArrayDeque<AccessibilityNodeInfo>().apply { addLast(root) }
        var visited = 0
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val node = stack.removeLast()
            visited++
            if (!node.isVisibleToUser) continue
            val piece = (node.text ?: node.contentDescription)?.toString()?.trim()
            if (!piece.isNullOrEmpty() && piece != out.lastOrNull()) out += piece
            for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let { stack.addLast(it) }
        }
        return out
    }

    private fun isDone(pieces: List<String>): Boolean {
        val all = pieces.joinToString(" ").lowercase()
        return DONE_ACTIONS.any { it in all }
    }

    private fun looksLikeConfirmation(pieces: List<String>): Boolean {
        val header = pieces.take(HEADER_PIECES).joinToString(" ").lowercase()
        val amounts = amountsIn(pieces)
        val hasMarker = SUCCESS_MARKERS.any { it in header }
        val ok = hasMarker && amounts.isNotEmpty() && amounts.size <= MAX_AMOUNTS
        // Solo en debug y solo local (logcat): sirve para ajustar los filtros con pantallas reales.
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "ok=$ok marker=$hasMarker montos=${amounts.size} | ${pieces.take(HEADER_PIECES)} … ${pieces.drop(HEADER_PIECES).takeLast(6)}")
        }
        return ok
    }

    companion object {
        private const val ACTION_DEBUG_DUMP = "com.lucas.predictaapp.DEBUG_DUMP_SCREEN"

        /**
         * true = el sistema efectivamente conectó el servicio en este proceso. Después de
         * reinstalar o forzar la detención, Ajustes puede mostrarlo activado sin que Android lo
         * esté corriendo: [isEnabled] da true y esto false.
         */
        @Volatile
        var isRunning = false
            private set

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            return enabled.split(':').any {
                it.startsWith(context.packageName + "/") && it.endsWith(PaymentScreenReader::class.java.simpleName)
            }
        }
    }
}
