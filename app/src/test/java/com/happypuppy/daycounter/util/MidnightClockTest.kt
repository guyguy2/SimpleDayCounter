package com.happypuppy.daycounter.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class MidnightClockTest {

    private val newYork = ZoneId.of("America/New_York")

    @Test
    fun `millisUntilNextMidnight returns remaining millis on a normal 24h day`() {
        val now = ZonedDateTime.of(2026, 6, 15, 10, 30, 0, 0, newYork)
        val clock = Clock.fixed(now.toInstant(), newYork)

        val expectedMillis = (13 * 3600L + 30 * 60L) * 1000L
        assertEquals(expectedMillis, millisUntilNextMidnight(clock))
    }

    @Test
    fun `millisUntilNextMidnight returns 1000ms at 23_59_59`() {
        val now = ZonedDateTime.of(2026, 6, 15, 23, 59, 59, 0, newYork)
        val clock = Clock.fixed(now.toInstant(), newYork)

        assertEquals(1_000L, millisUntilNextMidnight(clock))
    }

    @Test
    fun `millisUntilNextMidnight handles DST spring-forward 23h day`() {
        // 2026-03-08 in America/New_York jumps 02:00 -> 03:00 (23h day).
        val midnight = ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, newYork)
        val clock = Clock.fixed(midnight.toInstant(), newYork)

        assertEquals(23L * 3600L * 1000L, millisUntilNextMidnight(clock))
    }

    @Test
    fun `millisUntilNextMidnight handles DST fall-back 25h day`() {
        // 2026-11-01 in America/New_York repeats 01:00 -> 02:00 (25h day).
        val midnight = ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, newYork)
        val clock = Clock.fixed(midnight.toInstant(), newYork)

        assertEquals(25L * 3600L * 1000L, millisUntilNextMidnight(clock))
    }

    @Test
    fun `midnightDateFlow emits new LocalDate each time virtual clock crosses midnight`() = runTest {
        val start = ZonedDateTime.of(2026, 6, 15, 23, 59, 0, 0, newYork).toInstant()
        val scheduler = testScheduler
        val schedulerClock = object : Clock() {
            override fun getZone(): ZoneId = newYork
            override fun withZone(zone: ZoneId): Clock = this
            override fun instant(): Instant = start.plusMillis(scheduler.currentTime)
        }

        val emitted = mutableListOf<LocalDate>()
        midnightDateFlow(schedulerClock)
            .onEach { emitted.add(it) }
            .launchIn(backgroundScope)

        runCurrent()
        assertEquals(emptyList<LocalDate>(), emitted)

        // Advance 59s: still before midnight.
        advanceTimeBy(59_000L)
        runCurrent()
        assertEquals(emptyList<LocalDate>(), emitted)

        // Advance remaining 1s: crosses first midnight -> 2026-06-16.
        advanceTimeBy(1_000L)
        runCurrent()
        assertEquals(listOf(LocalDate.of(2026, 6, 16)), emitted)

        // Advance 24h: crosses second midnight -> 2026-06-17.
        advanceTimeBy(24L * 3600L * 1000L)
        runCurrent()
        assertEquals(
            listOf(LocalDate.of(2026, 6, 16), LocalDate.of(2026, 6, 17)),
            emitted
        )
    }
}
