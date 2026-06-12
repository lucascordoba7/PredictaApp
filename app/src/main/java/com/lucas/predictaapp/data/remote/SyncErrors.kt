package com.lucas.predictaapp.data.remote

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Canal único para los errores de sincronización con Supabase.
 *
 * Antes los catches eran silenciosos (PTA-008): si una escritura a la nube fallaba,
 * la app seguía como si nada. Ahora cada catch reporta acá, que:
 *  - loguea a Logcat (tag "PredictaSync") para debug con la notebook, y
 *  - emite un evento que el root composable muestra como Toast en el celular.
 *
 * Solo informa: la escritura local (Room) ya ocurrió, así que un fallo de sync
 * no rompe la UI, solo avisa que el espejo en la nube no se actualizó.
 */
object SyncErrors {
    private const val TAG = "PredictaSync"

    // extraBufferCapacity para que tryEmit nunca se pierda aunque no haya colector aún.
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** @param op identificador legible de la operación, p.ej. "expenses.upsert". */
    fun report(op: String, e: Throwable) {
        Log.e(TAG, "Sync falló en $op", e)
        _events.tryEmit("Sync falló: $op — ${e.message ?: e::class.simpleName}")
    }
}
