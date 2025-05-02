package vcmsa.projects.prog3c.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Data class representing a budget entry in the database.
 * Each budget is tied to a specific expense category and has defined
 * minimum and maximum amounts for a specific time period.
 */
@Entity(
    tableName = "budgets",  // Sets the table name in the SQLite database
    foreignKeys = [
        ForeignKey(
            entity = Category::class,        // References the Category entity
            parentColumns = ["id"],          // Uses the id column from Category
            childColumns = ["categoryId"],   // Maps to categoryId in this table
            onDelete = ForeignKey.CASCADE    // Deletes budgets when their category is deleted
        )
    ]
)
data class Budget(
    @PrimaryKey(autoGenerate = true)    // Primary key that automatically increments
    val id: Long = 0,                   // Unique identifier, defaults to 0 for new records
    val minimumAmount: Double,          // Minimum budget target amount
    val maximumAmount: Double,          // Maximum budget limit amount
    val categoryId: Long,               // Foreign key to the categories table
    val startDate: Date,                // Start date of the budget period
    val endDate: Date                   // End date of the budget period
)