package com.lucas.predictaapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey
    val id: String,
    val service: String,
    val initial: String,
    val usagePct: Int = 0,
    val monthly: Int,
    val zombie: Boolean = false,
    val lastUsedDate: String? = null,
    // Día del mes en que se cobra (1-31; se recorta al largo del mes).
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val billingDay: Int = 1,
    // "YYYY-MM" del último mes en que ya se generó el cargo. Idempotencia mensual.
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val lastChargedMonthKey: String = "",
    // Pausar sin borrar: si está inactiva no genera cargos.
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val active: Boolean = true,
    // Categoría del Expense que se genera cada ciclo.
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val categoryId: Long = 0,
)

const val ZOMBIE_THRESHOLD_DAYS = 30

fun Subscription.daysSinceLastUsed(today: LocalDate = LocalDate.now()): Int? =
    lastUsedDate?.let { raw ->
        runCatching { LocalDate.parse(raw) }.getOrNull()?.let { d ->
            ChronoUnit.DAYS.between(d, today).toInt().coerceAtLeast(0)
        }
    }

fun Subscription.computedUsagePct(today: LocalDate = LocalDate.now()): Int {
    val days = daysSinceLastUsed(today) ?: return 0
    return (100 - days * 100 / ZOMBIE_THRESHOLD_DAYS).coerceIn(0, 100)
}

fun Subscription.isZombie(today: LocalDate = LocalDate.now()): Boolean =
    computedUsagePct(today) == 0

// ───────────────────────── facturación recurrente ─────────────────────────

/** Día de cobro recortado al largo del mes (ej: 31 en febrero → 28/29). */
fun Subscription.effectiveBillingDay(month: YearMonth = YearMonth.now()): Int =
    billingDay.coerceIn(1, month.lengthOfMonth())

/** Millis del día de cobro de [month] (mediodía local, estable para mostrar). */
fun Subscription.chargeDateMillis(month: YearMonth = YearMonth.now()): Long =
    month.atDay(effectiveBillingDay(month))
        .atTime(12, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

/** ¿Ya se generó el cargo de este mes? */
fun Subscription.isChargedThisMonth(monthKey: String = currentMonthKey()): Boolean =
    lastChargedMonthKey == monthKey

/** ¿Corresponde generar el cargo hoy? (activa, no cobrada este mes y ya pasó el día). */
fun Subscription.isChargeDue(today: LocalDate = LocalDate.now()): Boolean {
    if (!active) return false
    if (lastChargedMonthKey == currentMonthKey()) return false
    return today.dayOfMonth >= effectiveBillingDay(YearMonth.from(today))
}
