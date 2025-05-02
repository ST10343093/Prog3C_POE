package vcmsa.projects.prog3c.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing an expense category in the database.
 * Categories allow expenses to be organized by type (e.g., groceries, utilities, entertainment).
 * Each category has a name and an associated color for visual identification.
 */
@Entity(tableName = "categories") // Defines the table name in the SQLite database
data class Category(
    @PrimaryKey(autoGenerate = true) // Primary key that automatically increments
    val id: Long = 0,                // Unique identifier, defaults to 0 for new records
    val name: String,                // The name of the category (e.g., "Groceries", "Rent")
    val color: Int                   // Store color as an integer representation
)