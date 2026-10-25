package com.sxanalys.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ListeningStreakTest {
    private val today = LocalDate.of(2026, 10, 25)

    @Test
    fun countsConsecutiveDaysEndingToday() {
        val result = computeListeningStreak(listOf(today, today.minusDays(1), today.minusDays(2)), today)
        assertEquals(3, result.current)
        assertEquals(3, result.best)
        assertTrue(result.listenedToday)
    }

    @Test
    fun streakSurvivesUntilEndOfToday() {
        val result = computeListeningStreak(listOf(today.minusDays(1), today.minusDays(2)), today)
        assertEquals(2, result.current)
        assertFalse(result.listenedToday)
    }

    @Test
    fun streakBreaksAfterMissedDay() {
        val result = computeListeningStreak(listOf(today.minusDays(2), today.minusDays(3)), today)
        assertEquals(0, result.current)
        assertEquals(2, result.best)
    }

    @Test
    fun lastSevenDaysIsOrderedOldestFirst() {
        val result = computeListeningStreak(listOf(today, today.minusDays(6)), today)
        assertEquals(listOf(true, false, false, false, false, false, true), result.lastSevenDays)
    }
}
