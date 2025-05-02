package vcmsa.projects.prog3c.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room Database class for the application's data persistence
 * This class serves as the main access point for the underlying SQLite database
 * Uses Room annotations to define the database configuration
 */
@Database(
    entities = [Category::class, Expense::class, Budget::class], // Defines database tables
    version = 1,                                                // Database version number
    exportSchema = false                                        // Don't export database schema
)
@TypeConverters(Converters::class) // Register type converters for custom data types (e.g. Date)
abstract class AppDatabase : RoomDatabase() {
    // Abstract DAO (Data Access Object) methods that will be implemented by Room
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        // Volatile ensures the INSTANCE is always up to date and same to all execution threads
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Singleton pattern implementation to ensure only one database instance exists
         * @param context Application context used to build the database
         * @return The singleton AppDatabase instance
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Create database only if instance doesn't exist (double-checked locking)
                val instance = Room.databaseBuilder(
                    context.applicationContext,   // Use application context to avoid memory leaks
                    AppDatabase::class.java,      // Reference to this database class
                    "budget_tracker_database"     // Database filename
                )
                    .fallbackToDestructiveMigration() // Recreate database if schema version increases
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}