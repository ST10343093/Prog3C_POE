package vcmsa.projects.prog3c.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) interface for the Category entity.
 * Provides methods to interact with the categories data in the database.
 * Uses Room annotations to define database operations.
 */
@Dao
interface CategoryDao {
    /**
     * Inserts a new category into the database.
     * If a category with the same ID already exists, it will be replaced.
     * @param category The category object to insert
     * @return The row ID of the newly inserted category
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    /**
     * Updates an existing category in the database.
     * @param category The category object with updated values
     */
    @Update
    suspend fun updateCategory(category: Category)

    /**
     * Deletes a category from the database.
     * This will also delete all related expenses and budgets via CASCADE.
     * @param category The category object to delete
     */
    @Delete
    suspend fun deleteCategory(category: Category)

    /**
     * Retrieves all categories from the database.
     * @return A Flow list of all categories, which updates when data changes
     */
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<Category>>

    /**
     * Finds a category by its ID.
     * Used when we need to retrieve a specific category's details.
     * @param categoryId The ID of the category to retrieve
     * @return The matching category or null if none exists
     */
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): Category?
}