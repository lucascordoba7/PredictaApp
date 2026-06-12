package com.lucas.predictaapp.ui.utils

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

fun formatGreetingEyebrow(): String {
    val cal = Calendar.getInstance()
    val dayName = SimpleDateFormat("EEEE", Locale("es", "AR")).format(cal.time).uppercase()
    val dayNum = cal.get(Calendar.DAY_OF_MONTH)
    val monthName = SimpleDateFormat("MMMM", Locale("es", "AR")).format(cal.time).uppercase()
    return "$dayName · $dayNum DE $monthName"
}

fun formatMonthYear(): String =
    SimpleDateFormat("MMMM yyyy", Locale("es", "AR"))
        .format(Calendar.getInstance().time)
        .uppercase()

fun getDayOfMonth(): Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

fun getDaysInMonth(): Int = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)

fun getDaysRemainingInMonth(): Int =
    getDaysInMonth() - getDayOfMonth()

fun getDaysUntil(dateStr: String): Int = try {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val target = sdf.parse(dateStr) ?: return 0
    val diff = target.time - System.currentTimeMillis()
    ((diff / (1000L * 60 * 60 * 24)).toInt()).coerceAtLeast(0)
} catch (e: Exception) {
    0
}

/**
 * Label relativo de una fecha, calculado desde el millis real (no un string guardado):
 * hoy / ayer / anteayer / hace N días (3-6) / "el d/m". Mismo formato que usa el chat.
 */
fun relativeDateLabel(millis: Long): String {
    if (millis == 0L) return "hoy"
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    val daysAgo = ChronoUnit.DAYS.between(date, LocalDate.now())
    return when {
        daysAgo == 0L -> "hoy"
        daysAgo == 1L -> "ayer"
        daysAgo == 2L -> "anteayer"
        daysAgo in 3..6 -> "hace $daysAgo días"
        else -> "el ${date.dayOfMonth}/${date.monthValue}"
    }
}

fun isCurrentMonth(millis: Long): Boolean {
    if (millis == 0L) return false
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().also { it.timeInMillis = millis }
    return now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
           now.get(Calendar.MONTH) == target.get(Calendar.MONTH)
}

fun getDaysToPayday(paydayDay: Int): Int {
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_MONTH)
    return when {
        paydayDay > today -> paydayDay - today
        paydayDay == today -> 0
        else -> {
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val nextCal = Calendar.getInstance().also { it.add(Calendar.MONTH, 1) }
            val nextPayday = paydayDay.coerceAtMost(nextCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            (daysInMonth - today) + nextPayday
        }
    }
}
