package com.happypuppy.daycounter.util

import android.annotation.SuppressLint
import java.util.concurrent.TimeUnit
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

@SuppressLint("DefaultLocale")
fun formatTime(seconds: Int): String {
    val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong())
    val remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

fun formatDateToString(date: String): String {
    // Define the desired date format pattern.
    val pattern = "yyyy-MM-dd HH:mm:ss"

    // Create a SimpleDateFormat instance with the specified pattern and locale.
    // Using a specific locale (e.g., Locale.getDefault()) ensures consistent formatting across different devices.
    val dateFormat = SimpleDateFormat(pattern, Locale.getDefault())

    // Format the Date object into a String.
    return dateFormat.format(date)
}

fun calculateDaysLeft(targetDate: Date): Int {
    // Create calendar instances
    val targetCalendar = Calendar.getInstance()
    targetCalendar.time = targetDate

    // Set time to beginning of day
    targetCalendar.set(Calendar.HOUR_OF_DAY, 0)
    targetCalendar.set(Calendar.MINUTE, 0)
    targetCalendar.set(Calendar.SECOND, 0)
    targetCalendar.set(Calendar.MILLISECOND, 0)

    // Current date with time set to beginning of day
    val currentCalendar = Calendar.getInstance()
    currentCalendar.set(Calendar.HOUR_OF_DAY, 0)
    currentCalendar.set(Calendar.MINUTE, 0)
    currentCalendar.set(Calendar.SECOND, 0)
    currentCalendar.set(Calendar.MILLISECOND, 0)

    // Calculate difference in days
    val millisecondsInDay = 24 * 60 * 60 * 1000L
    val diffInMillis = targetCalendar.timeInMillis - currentCalendar.timeInMillis
    return (diffInMillis / millisecondsInDay).toInt()
}

fun isColorBright(color: Color): Boolean {
    // Calculate perceived brightness using the formula
    // (0.299*R + 0.587*G + 0.114*B)
    val brightness = (0.299f * color.red + 0.587f * color.green + 0.114f * color.blue)
    return brightness > 0.5f
}