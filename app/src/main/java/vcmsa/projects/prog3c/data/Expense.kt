package vcmsa.projects.prog3c.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Data class representing an expense entry in the database.
 * Each expense records a financial transaction with amount, description,
 * date, and category details, with optional photo receipt evidence.
 */
@Entity(
    tableName = "expenses",  // Sets the table name in the SQLite database
    foreignKeys = [
        ForeignKey(
            entity = Category::class,        // References the Category entity
            parentColumns = ["id"],          // Uses the id column from Category
            childColumns = ["categoryId"],   // Maps to categoryId in this table
            onDelete = ForeignKey.CASCADE    // Deletes expenses when their category is deleted
        )
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)    // Primary key that automatically increments
    val id: Long = 0,                   // Unique identifier, defaults to 0 for new records
    val amount: Double,                 // The monetary value of the expense
    val description: String,            // Description of what the expense was for
    val date: Date,                     // When the expense occurred
    val categoryId: Long,               // Foreign key linking to the category
    val photoPath: String? = null       // Path to the stored photo, null if no photo
)