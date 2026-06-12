package com.lucas.predictaapp.notifications

import com.lucas.predictaapp.data.model.ExpenseWithCategory
import com.lucas.predictaapp.data.model.FixedExpense
import com.lucas.predictaapp.data.model.FixedExpenseStatus
import com.lucas.predictaapp.data.model.NotificationType
import com.lucas.predictaapp.data.model.Subscription
import com.lucas.predictaapp.data.model.computeStatus
import com.lucas.predictaapp.data.model.currentMonthKey
import com.lucas.predictaapp.data.model.effectiveBillingDay
import com.lucas.predictaapp.ui.utils.fmtArs
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields

/** A qué pantalla lleva el tap de la notificación. */
enum class NotifTarget { DASHBOARD, SUBSCRIPTIONS, FIXED_EXPENSES, CHAT, NOTIFICATIONS }

/** Momento del día en que corre el worker. */
enum class NotifSlot { MORNING, EVENING }

/** Una notificación lista para postear (sistema + feed in-app). El id es determinista → dedupe. */
data class PendingNotif(
    val id: String,
    val channel: String,
    val emoji: String,
    val typeLabel: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val target: NotifTarget = NotifTarget.DASHBOARD,
)

object NotificationRules {

    // Umbrales (ajustables).
    private const val SPIKE_RATIO = 1.5            // gasto semanal vs promedio para considerar pico
    private const val SPIKE_MIN_AMOUNT = 10_000    // piso para evitar ruido
    private const val FORGOT_DAYS = 3              // días sin registrar para avisar
    private const val HABIT_LOOKBACK_WEEKS = 6     // ventana para detectar hábito
    private const val HABIT_MIN_HITS = 3           // cuántas de esas semanas tuvieron el gasto

    fun evaluate(
        slot: NotifSlot,
        today: LocalDate,
        expenses: List<ExpenseWithCategory>,
        subscriptions: List<Subscription>,
        fixedExpenses: List<FixedExpense>,
        income: Int,
        paydayDay: Int,
    ): List<PendingNotif> = when (slot) {
        NotifSlot.MORNING -> buildList {
            addAll(subscriptionTomorrow(today, subscriptions))
            addAll(fixedExpenseDue(today, fixedExpenses))
            categorySpike(today, expenses)?.let { add(it) }
            weeklySummary(today, expenses)?.let { add(it) }
        }
        NotifSlot.EVENING -> buildList {
            forgotToLog(today, expenses)?.let { add(it) }
            habitReminder(today, expenses)?.let { add(it) }
        }
    }

    // ── Suscripción se cobra mañana ──────────────────────────────
    private fun subscriptionTomorrow(today: LocalDate, subs: List<Subscription>): List<PendingNotif> {
        val tomorrow = today.plusDays(1)
        val month = YearMonth.from(tomorrow)
        return subs.filter { it.active && it.effectiveBillingDay(month) == tomorrow.dayOfMonth }
            .map { sub ->
                PendingNotif(
                    id = "sub_due_${sub.id}_$tomorrow",
                    channel = NotificationChannels.COBROS,
                    emoji = "🔁",
                    typeLabel = "Cobro mañana",
                    type = NotificationType.WARNING,
                    title = "Mañana se cobra ${sub.service}",
                    body = "Se te va a debitar $ ${sub.monthly.fmtArs()}. Si no la usás, podés cancelarla hoy.",
                    target = NotifTarget.SUBSCRIPTIONS,
                )
            }
    }

    // ── Gasto fijo por vencer / vencido ──────────────────────────
    private fun fixedExpenseDue(today: LocalDate, fixed: List<FixedExpense>): List<PendingNotif> {
        val monthKey = currentMonthKey()
        return fixed.filter { it.active }.mapNotNull { fx ->
            when (fx.computeStatus(todayDay = today.dayOfMonth, monthKey = monthKey)) {
                FixedExpenseStatus.PENDIENTE -> {
                    val daysUntil = fx.dueDayOfMonth - today.dayOfMonth
                    if (daysUntil in 0..2) {
                        val whenTxt = when (daysUntil) {
                            0 -> "vence hoy"
                            1 -> "vence mañana"
                            else -> "vence en $daysUntil días"
                        }
                        PendingNotif(
                            id = "fixed_due_${fx.id}_$monthKey",
                            channel = NotificationChannels.COBROS,
                            emoji = "📌",
                            typeLabel = "Gasto fijo",
                            type = NotificationType.WARNING,
                            title = "${fx.name} $whenTxt",
                            body = "$ ${fx.amount.fmtArs()} · día ${fx.dueDayOfMonth}. Acordate de pagarlo.",
                            target = NotifTarget.FIXED_EXPENSES,
                        )
                    } else null
                }
                FixedExpenseStatus.VENCIDO -> PendingNotif(
                    id = "fixed_overdue_${fx.id}_$monthKey",
                    channel = NotificationChannels.COBROS,
                    emoji = "⚠️",
                    typeLabel = "Vencido",
                    type = NotificationType.WARNING,
                    title = "Se venció ${fx.name}",
                    body = "$ ${fx.amount.fmtArs()} · vencía el día ${fx.dueDayOfMonth}.",
                    target = NotifTarget.FIXED_EXPENSES,
                )
                FixedExpenseStatus.PAGADO -> null
            }
        }
    }

    // ── Pico en una categoría (semana actual vs promedio) ────────
    private fun categorySpike(today: LocalDate, expenses: List<ExpenseWithCategory>): PendingNotif? {
        // Necesita varios días de la semana para ser significativo.
        if (today.dayOfWeek.value < DayOfWeek.THURSDAY.value) return null
        val weekField = WeekFields.ISO.weekOfWeekBasedYear()
        val thisWeekKey = today.year to today.get(weekField)

        data class Wk(val cat: String, val weekKey: Pair<Int, Int>, val amount: Int)
        val spend = expenses.filter { !it.isIncome }.map {
            val d = it.dateMillis.toLocalDate()
            Wk(it.category, d.year to d.get(weekField), it.amount)
        }
        val byCatWeek = spend.groupBy { it.cat to it.weekKey }
            .mapValues { (_, v) -> v.sumOf { it.amount } }

        var best: PendingNotif? = null
        var bestPct = 0
        val categories = spend.map { it.cat }.toSet()
        for (cat in categories) {
            val thisWeek = byCatWeek[cat to thisWeekKey] ?: 0
            if (thisWeek < SPIKE_MIN_AMOUNT) continue
            // Promedio de las 4 semanas previas (con gasto en la categoría).
            val priorWeeks = (1..4).mapNotNull { back ->
                val d = today.minusWeeks(back.toLong())
                byCatWeek[cat to (d.year to d.get(weekField))]
            }.filter { it > 0 }
            if (priorWeeks.isEmpty()) continue
            val avg = priorWeeks.average()
            if (thisWeek >= avg * SPIKE_RATIO) {
                val pct = (((thisWeek / avg) - 1) * 100).toInt()
                if (pct > bestPct) {
                    bestPct = pct
                    best = PendingNotif(
                        id = "spike_${cat}_${thisWeekKey.first}_${thisWeekKey.second}",
                        channel = NotificationChannels.PATRONES,
                        emoji = "📈",
                        typeLabel = "Pico de gasto",
                        type = NotificationType.PATTERN,
                        title = "Se te disparó $cat",
                        body = "Esta semana llevás $ ${thisWeek.fmtArs()} en $cat, +$pct% vs tu promedio.",
                        target = NotifTarget.DASHBOARD,
                    )
                }
            }
        }
        return best
    }

    // ── Resumen semanal (lunes a la mañana) ──────────────────────
    private fun weeklySummary(today: LocalDate, expenses: List<ExpenseWithCategory>): PendingNotif? {
        if (today.dayOfWeek != DayOfWeek.MONDAY) return null
        val start = today.minusDays(7)   // lunes pasado
        val end = today.minusDays(1)     // domingo pasado
        val week = expenses.filter { !it.isIncome }.filter {
            val d = it.dateMillis.toLocalDate()
            !d.isBefore(start) && !d.isAfter(end)
        }
        if (week.isEmpty()) return null
        val total = week.sumOf { it.amount }
        val topCat = week.groupBy { it.category }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
            .maxByOrNull { it.value }
        val topTxt = topCat?.let { " Top: ${it.key} ($ ${it.value.fmtArs()})." } ?: ""
        return PendingNotif(
            id = "weekly_${start}",
            channel = NotificationChannels.RESUMENES,
            emoji = "🗓",
            typeLabel = "Resumen semanal",
            type = NotificationType.PATTERN,
            title = "Tu semana en números",
            body = "Gastaste $ ${total.fmtArs()} en ${week.size} movimientos.$topTxt",
            target = NotifTarget.DASHBOARD,
        )
    }

    // ── Te olvidaste de registrar ────────────────────────────────
    private fun forgotToLog(today: LocalDate, expenses: List<ExpenseWithCategory>): PendingNotif? {
        if (expenses.isEmpty()) return null
        val last = expenses.maxOf { it.dateMillis }.toLocalDate()
        val days = ChronoUnit.DAYS.between(last, today).toInt()
        if (days < FORGOT_DAYS) return null
        return PendingNotif(
            id = "forgot_$today",
            channel = NotificationChannels.RECORDATORIOS,
            emoji = "✏️",
            typeLabel = "Recordatorio",
            type = NotificationType.FORGOT,
            title = "Hace $days días que no anotás gastos",
            body = "¿Cargaste todo? Contame en una línea y lo registro por vos.",
            target = NotifTarget.CHAT,
        )
    }

    // ── Hábito recurrente (día de semana × categoría) faltante hoy ─
    private fun habitReminder(today: LocalDate, expenses: List<ExpenseWithCategory>): PendingNotif? {
        val weekday = today.dayOfWeek
        val spend = expenses.filter { !it.isIncome }.map { it.category to it.dateMillis.toLocalDate() }
        // Fechas de las últimas N ocurrencias de este día de semana (sin contar hoy).
        val pastDates = (1..HABIT_LOOKBACK_WEEKS).map { today.minusWeeks(it.toLong()) }
        val categories = spend.map { it.first }.toSet()

        var bestCat: String? = null
        var bestHits = 0
        for (cat in categories) {
            val datesWithCat = spend.filter { it.first == cat }.map { it.second }.toSet()
            val hits = pastDates.count { it in datesWithCat }
            // Ya cargado hoy en esa categoría → no molestar.
            val loggedToday = spend.any { it.first == cat && it.second == today }
            if (!loggedToday && hits >= HABIT_MIN_HITS && hits > bestHits) {
                bestHits = hits
                bestCat = cat
            }
        }
        val cat = bestCat ?: return null
        return PendingNotif(
            id = "habit_${cat}_$today",
            channel = NotificationChannels.PATRONES,
            emoji = "🧠",
            typeLabel = "Tu patrón",
            type = NotificationType.PATTERN,
            title = "Los ${weekdayPlural(weekday)} solés gastar en $cat",
            body = "¿Hoy gastaste y te olvidaste de cargarlo? Decímelo y lo anoto.",
            target = NotifTarget.CHAT,
        )
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun weekdayPlural(d: DayOfWeek): String = when (d) {
        DayOfWeek.MONDAY -> "lunes"
        DayOfWeek.TUESDAY -> "martes"
        DayOfWeek.WEDNESDAY -> "miércoles"
        DayOfWeek.THURSDAY -> "jueves"
        DayOfWeek.FRIDAY -> "viernes"
        DayOfWeek.SATURDAY -> "sábados"
        DayOfWeek.SUNDAY -> "domingos"
    }
}
