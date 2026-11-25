package com.happypuppy.daycounter.util

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.ui.graphics.Color
import java.text.BreakIterator
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

/** One-shot haptic feedback. minSdk 31 guarantees [VibratorManager]. */
fun vibrate(context: Context, durationMs: Long = 100) {
    val vibratorManager =
        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            ?: return
    vibratorManager.defaultVibrator.vibrate(
        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
    )
}

/**
 * Utility class to get app version information without direct BuildConfig dependency
 */
object AppVersionInfo {
    /**
     * Get the version name of the app
     * @param context Application context
     * @return Version name as String
     */
    fun getVersionName(context: Context): String {
        return try {
            val packageInfo = getPackageInfo(context)
            packageInfo.versionName
        } catch (_: Exception) {
            "1.0.0" // Fallback version
        }.toString()
    }

    /**
     * Get the version code of the app
     * @param context Application context
     * @return Version code as String
     */
    fun getVersionCode(context: Context): String {
        return try {
            val packageInfo = getPackageInfo(context)
            packageInfo.longVersionCode.toString()
        } catch (_: Exception) {
            "1" // Fallback version code
        }
    }

    /**
     * Get full version information as a formatted string
     * @param context Application context
     * @return Formatted version string: "versionName (versionCode)"
     */
    fun getFullVersionInfo(context: Context): String {
        return "${getVersionName(context)} (${getVersionCode(context)})"
    }

    private fun getPackageInfo(context: Context): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    }
}

/**
 * Computes the signed calendar-day difference between [today] and [targetDate] in [zoneId].
 * Uses `java.time.LocalDate` + [ChronoUnit.DAYS] so DST spring-forward (23h) and fall-back (25h)
 * transitions never cause off-by-one errors.
 */
fun calculateDaysLeft(
    targetDate: Date,
    today: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): Int {
    val targetLocalDate = targetDate.toInstant().atZone(zoneId).toLocalDate()
    return ChronoUnit.DAYS.between(today, targetLocalDate).toInt()
}

/**
 * Converts a local [Date] into UTC midnight milliseconds for the same calendar date in [zoneId],
 * matching what Material3's `DatePicker` expects for `initialSelectedDateMillis`.
 */
fun localDateToUtcPickerMillis(
    date: Date,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val localDate = date.toInstant().atZone(zoneId).toLocalDate()
    return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

/**
 * Anchors [localDate] at 12:00 local time in [zoneId] so calendar-day conversions stay resilient
 * across DST transitions and timezone offset boundaries.
 */
fun localDateToNoonDate(
    localDate: LocalDate,
    zoneId: ZoneId = ZoneId.systemDefault()
): Date = Date.from(localDate.atTime(12, 0).atZone(zoneId).toInstant())

/**
 * Returns today's calendar date from [clock] anchored at 12:00 local time in `clock.zone`.
 */
fun todayAtNoon(clock: Clock = Clock.systemDefaultZone()): Date =
    localDateToNoonDate(LocalDate.now(clock), clock.zone)

/**
 * Converts a UTC midnight millisecond timestamp from Material3's `DatePicker` into a [Date]
 * anchored at 12:00 local time in [zoneId] on that calendar date.
 */
fun utcPickerMillisToLocalDate(
    utcMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Date {
    val localDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return localDateToNoonDate(localDate, zoneId)
}

/**
 * Returns true if [text] is empty or contains exactly one user-perceived grapheme cluster
 * (supporting variation-selector emojis like ❤️/☀️, flags like 🇺🇸, skin-tone modifiers like 👍🏽,
 * and ZWJ sequences like 👨‍👩‍👧).
 */
fun isSingleGrapheme(text: String): Boolean {
    if (text.isEmpty()) return true
    val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
    iterator.setText(text)
    val firstEnd = iterator.next()
    return firstEnd != BreakIterator.DONE && iterator.next() == BreakIterator.DONE
}

fun isColorBright(color: Color): Boolean {
    // Calculate perceived brightness using the formula
    // (0.299*R + 0.587*G + 0.114*B)
    val brightness = (0.299f * color.red + 0.587f * color.green + 0.114f * color.blue)
    return brightness > 0.5f
}
