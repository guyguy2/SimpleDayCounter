package com.happypuppy.daycounter.util

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Returns the milliseconds from `now` (per [clock]) until the next local midnight (`00:00`) in
 * `clock.zone`, coerced to at least `1L`. Using [ZonedDateTime] handles 23-hour spring-forward
 * and 25-hour fall-back DST days accurately.
 */
fun millisUntilNextMidnight(clock: Clock): Long {
    val now = ZonedDateTime.now(clock)
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(clock.zone)
    return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L)
}

/** Emits the new [LocalDate] each time [clock] crosses local midnight. */
fun midnightDateFlow(clock: Clock): Flow<LocalDate> = flow {
    while (currentCoroutineContext().isActive) {
        delay(millisUntilNextMidnight(clock))
        emit(LocalDate.now(clock))
    }
}
