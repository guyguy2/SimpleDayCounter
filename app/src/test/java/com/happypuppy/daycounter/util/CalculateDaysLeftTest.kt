package com.happypuppy.daycounter.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * Deterministic unit tests for [calculateDaysLeft], including DST spring-forward (23-hour day)
 * and fall-back (25-hour day) transitions in `America/Chicago`.
 */
class CalculateDaysLeftTest {

    private val utc = ZoneId.of("UTC")
    private val chicago = ZoneId.of("America/Chicago")

    private fun dateAt(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 12,
        minute: Int = 0,
        zone: ZoneId = utc
    ): Date = Date.from(LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant())

    @Test
    fun `today returns zero`() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(0, calculateDaysLeft(dateAt(2026, 6, 15), today = today, zoneId = utc))
    }

    @Test
    fun `tomorrow returns one`() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(1, calculateDaysLeft(dateAt(2026, 6, 16), today = today, zoneId = utc))
    }

    @Test
    fun `yesterday returns minus one`() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(-1, calculateDaysLeft(dateAt(2026, 6, 14), today = today, zoneId = utc))
    }

    @Test
    fun `seven days ahead returns seven`() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(7, calculateDaysLeft(dateAt(2026, 6, 22), today = today, zoneId = utc))
    }

    @Test
    fun `thirty days behind returns minus thirty`() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(-30, calculateDaysLeft(dateAt(2026, 5, 16), today = today, zoneId = utc))
    }

    @Test
    fun `start and end of day within today both return zero`() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(0, calculateDaysLeft(dateAt(2026, 6, 15, hour = 0, minute = 0, zone = chicago), today = today, zoneId = chicago))
        assertEquals(0, calculateDaysLeft(dateAt(2026, 6, 15, hour = 23, minute = 59, zone = chicago), today = today, zoneId = chicago))
    }

    @Test
    fun `spring-forward DST transition in America Chicago does not lose a day`() {
        // US DST starts Sunday, March 8, 2026 at 02:00 CST -> 03:00 CDT (23-hour day).
        val beforeDst = LocalDate.of(2026, 3, 7)
        val afterDstTarget = dateAt(2026, 3, 9, hour = 0, minute = 0, zone = chicago)
        assertEquals(2, calculateDaysLeft(afterDstTarget, today = beforeDst, zoneId = chicago))

        val dstDay = LocalDate.of(2026, 3, 8)
        assertEquals(1, calculateDaysLeft(afterDstTarget, today = dstDay, zoneId = chicago))
    }

    @Test
    fun `spring-forward DST transition backwards returns exact negative days`() {
        val afterDst = LocalDate.of(2026, 3, 9)
        val beforeDstTarget = dateAt(2026, 3, 7, hour = 0, minute = 0, zone = chicago)
        assertEquals(-2, calculateDaysLeft(beforeDstTarget, today = afterDst, zoneId = chicago))
    }

    @Test
    fun `fall-back DST transition in America Chicago does not add an extra day`() {
        // US DST ends Sunday, November 1, 2026 at 02:00 CDT -> 01:00 CST (25-hour day).
        val beforeFallBack = LocalDate.of(2026, 10, 31)
        val afterFallBackTarget = dateAt(2026, 11, 2, hour = 0, minute = 0, zone = chicago)
        assertEquals(2, calculateDaysLeft(afterFallBackTarget, today = beforeFallBack, zoneId = chicago))

        val fallBackDay = LocalDate.of(2026, 11, 1)
        assertEquals(1, calculateDaysLeft(afterFallBackTarget, today = fallBackDay, zoneId = chicago))
    }

    @Test
    fun `fall-back DST transition backwards returns exact negative days`() {
        val afterFallBack = LocalDate.of(2026, 11, 2)
        val beforeFallBackTarget = dateAt(2026, 10, 31, hour = 0, minute = 0, zone = chicago)
        assertEquals(-2, calculateDaysLeft(beforeFallBackTarget, today = afterFallBack, zoneId = chicago))
    }

    @Test
    fun `leap year February has 29 days`() {
        assertEquals(
            2,
            calculateDaysLeft(dateAt(2024, 3, 1), today = LocalDate.of(2024, 2, 28), zoneId = utc)
        )
        assertEquals(
            1,
            calculateDaysLeft(dateAt(2026, 3, 1), today = LocalDate.of(2026, 2, 28), zoneId = utc)
        )
    }

    @Test
    fun `far future date is positive`() {
        val today = LocalDate.of(2026, 1, 1)
        assertTrue(calculateDaysLeft(dateAt(2027, 1, 1), today = today, zoneId = utc) == 365)
    }
}
