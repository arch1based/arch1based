package com.arch1based.lensy.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class LogicTest {
    private val start = LocalDate.of(2026, 1, 1)
    private val monthly = LensPlan(start, 30)
    private val nine = LocalTime.of(9, 0)

    @Test
    fun dueAndDaysLeft() {
        assertEquals(LocalDate.of(2026, 1, 31), monthly.due)
        assertEquals(30, monthly.daysLeft(start))
        assertEquals(0, monthly.daysLeft(monthly.due))
        assertEquals(-3, monthly.daysLeft(monthly.due.plusDays(3)))
    }

    @Test
    fun leadDaysScaleWithInterval() {
        assertEquals(2, monthly.leadDays)
        assertEquals(2, LensPlan(start, 14).leadDays)
        assertEquals(1, LensPlan(start, 7).leadDays)
        assertEquals(0, LensPlan(start, 1).leadDays)
    }

    @Test
    fun firstReminderIsTwoDaysBefore() {
        val next = nextReminder(monthly, LocalDateTime.of(2026, 1, 5, 12, 0), nine)
        assertEquals(LocalDateTime.of(2026, 1, 29, 9, 0), next)
    }

    @Test
    fun reminderRollsToNextDayAfterTimePassed() {
        val next = nextReminder(monthly, LocalDateTime.of(2026, 1, 29, 9, 0), nine)
        assertEquals(LocalDateTime.of(2026, 1, 30, 9, 0), next)
    }

    @Test
    fun keepsNaggingWhenOverdue() {
        val next = nextReminder(monthly, LocalDateTime.of(2026, 2, 10, 8, 0), nine)
        assertEquals(LocalDateTime.of(2026, 2, 10, 9, 0), next)
    }

    @Test
    fun moods() {
        assertEquals(Mood.FRESH, moodFor(monthly, start))
        assertEquals(Mood.OK, moodFor(monthly, start.plusDays(15)))
        assertEquals(Mood.TIRED, moodFor(monthly, start.plusDays(28)))
        assertEquals(Mood.DUE, moodFor(monthly, start.plusDays(30)))
        assertEquals(Mood.OVERDUE, moodFor(monthly, start.plusDays(31)))
    }

    @Test
    fun trial() {
        assertEquals(40, trialDaysLeft(start, start))
        assertEquals(1, trialDaysLeft(start, start.plusDays(39)))
        assertEquals(0, trialDaysLeft(start, start.plusDays(40)))
        assertEquals(0, trialDaysLeft(start, start.plusDays(400)))
        assertEquals(40, trialDaysLeft(start, start.minusDays(5)))
    }
}
