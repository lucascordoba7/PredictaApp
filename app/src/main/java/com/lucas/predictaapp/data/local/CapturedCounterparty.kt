package com.lucas.predictaapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * De qué destinatario salió cada transferencia capturada. El gasto guarda el nombre que
 * el usuario termine poniéndole ("Carnicería"), así que el original ("Adam Gonzalo Javier")
 * vive acá para reconocer al destinatario la próxima vez. Solo local, no se sincroniza.
 */
@Entity(tableName = "captured_counterparties", indices = [Index("counterpartyKey")])
data class CapturedCounterparty(
    @PrimaryKey val expenseId: Long,
    /** Nombre normalizado: minúsculas, sin acentos, palabras ordenadas. */
    val counterpartyKey: String,
    val capturedAt: Long,
)

/** Cómo quedó la última captura de un destinatario, con lo que haya corregido el usuario. */
data class RememberedCapture(val merchant: String, val category: String)
