package vcmsa.projects.prog3c.data

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room database type converter class for custom data types.
 * Room can only store primitive types, so this class helps convert
 * complex objects like Date to primitives and back for database storage.
 */
class Converters {
    /**
     * Converts a timestamp (Long) value from the database to a Date object.
     * Used when reading date values from the database.
     * @param value The timestamp in milliseconds since epoch
     * @return A Date object, or null if the input was null
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Converts a Date object to a timestamp (Long) for database storage.
     * Used when saving date values to the database.
     * @param date The Date object to convert
     * @return The timestamp in milliseconds since epoch, or null if the input was null
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}