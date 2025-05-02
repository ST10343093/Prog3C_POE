package vcmsa.projects.prog3c.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object (DAO) interface for the Budget entity.
 * Provides methods to interact with the budget data in the database.
 * Uses Room annotations to define database operations.
 */
@Dao
interface BudgetDao {
    /**
     * Inserts a new budget into the database.
     * If a budget with the same ID already exists, it will be replaced.
     * @param budget The budget object to insert
     * @return The row ID of the newly inserted budget
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    /**
     * Updates an existing budget in the database.
     * @param budget The budget object with updated values
     */
    @Update
    suspend fun updateBudget(budget: Budget)

    /**
     * Deletes a budget from the database.
     * @param budget The budget object to delete
     */
    @Delete
    suspend fun deleteBudget(budget: Budget)

    /**
     * Retrieves all budgets from the database.
     * @return A Flow list of all budgets, which updates when data changes
     */
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    /**
     * Finds a budget for a specific category that is active on a given date.
     * Used to check if a category has an active budget for expense calculations.
     * @param categoryId The category ID to search for
     * @param date The date to check if it falls within budget period
     * @return The matching budget or null if none exists
     */
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND startDate <= :date AND endDate >= :date")
    suspend fun getBudgetForCategoryAndDate(categoryId: Long, date: Date): Budget?

    /**
     * Retrieves all budgets that are active on a specific date.
     * @param date The date to check for active budgets
     * @return A Flow list of all active budgets for the date
     */
    @Query("SELECT * FROM budgets WHERE startDate <= :date AND endDate >= :date")
    fun getBudgetsForDate(date: Date): Flow<List<Budget>>
}