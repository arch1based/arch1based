package com.arch1based.lensy.core

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** Free trial length: one full monthly cycle plus some slack. */
const val TRIAL_DAYS = 40L

data class LensPlan(val start: LocalDate, val intervalDays: Int) {
    val due: LocalDate get() = start.plusDays(intervalDays.toLong())

    /** How many days before the due date reminders begin (2 for monthly, less for short cycles). */
    val leadDays: Long get() = minOf(2, intervalDays / 4).toLong()

    /** Positive while there is time left, 0 on change day, negative when overdue. */
    fun daysLeft(today: LocalDate): Long = ChronoUnit.DAYS.between(today, due)

    /** 0 = just put in, 1 = due (or overdue). */
    fun progress(today: LocalDate): Float {
        val worn = ChronoUnit.DAYS.between(start, today).toFloat()
        return (worn / intervalDays.coerceAtLeast(1)).coerceIn(0f, 1f)
    }
}

enum class Mood { FRESH, OK, TIRED, DUE, OVERDUE }

fun moodFor(plan: LensPlan, today: LocalDate): Mood {
    val left = plan.daysLeft(today)
    val p = plan.progress(today)
    return when {
        left < 0 -> Mood.OVERDUE
        left == 0L -> Mood.DUE
        left <= plan.leadDays || p >= 0.8f -> Mood.TIRED
        p >= 0.4f -> Mood.OK
        else -> Mood.FRESH
    }
}

/**
 * The next moment a reminder should fire: every day at [time], from [LensPlan.leadDays] before
 * the due date onwards (so it keeps nagging daily while overdue).
 */
fun nextReminder(plan: LensPlan, now: LocalDateTime, time: LocalTime): LocalDateTime {
    val first = plan.due.minusDays(plan.leadDays)
    val day = maxOf(first, now.toLocalDate())
    val at = day.atTime(time)
    return if (at.isAfter(now)) at else day.plusDays(1).atTime(time)
}

fun trialDaysLeft(install: LocalDate, today: LocalDate): Long =
    (TRIAL_DAYS - ChronoUnit.DAYS.between(install, today)).coerceIn(0, TRIAL_DAYS)
