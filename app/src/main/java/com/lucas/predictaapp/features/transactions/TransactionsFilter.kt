package com.lucas.predictaapp.features.transactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.YearMonth

/**
 * Filtro con el que se abre Actividad desde otra pantalla (p. ej. tocar una categoría en la home).
 * Quien navega lo deja acá; [TransactionsScreen] lo aplica al entrar y lo limpia.
 */
object TransactionsFilter {
    data class Pending(val category: String?, val month: YearMonth)

    var pending: Pending? by mutableStateOf(null)
        private set

    fun open(category: String?, month: YearMonth) {
        pending = Pending(category, month)
    }

    fun consume(): Pending? = pending.also { pending = null }
}
