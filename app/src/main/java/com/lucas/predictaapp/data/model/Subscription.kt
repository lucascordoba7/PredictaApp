package com.lucas.predictaapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
