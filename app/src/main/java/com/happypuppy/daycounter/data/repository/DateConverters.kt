package com.happypuppy.daycounter.data.repository

import androidx.room.TypeConverter
import java.time.LocalDate
import java.util.Date

/**
 * Type converters to allow Room to reference complex data types.
 */
class DateConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromLocalDateString(value: String?): LocalDate? {
        return value?.let(LocalDate::parse)
    }

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        return date?.toString()
    }
}