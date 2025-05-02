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
 * Data Access Object (DAO) interface for the Expense entity.
 * Provides methods to interact with the expenses data in the database.
 * Uses Room annotations to define database operations.
 */
@Dao
interface ExpenseDao {
    /**
     * Inserts a new expense into the database.
     * If an expense with the same ID already exists, it will be replaced.
     * @param expense The expense object to insert
     * @return The row ID of the newly inserted expense
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    /**
     * Updates an existing expense in the database.
     * @param expense The expense object with updated values
     */
    @Update
    suspend fun updateExpense(expense: Expense)

    /**
     * Deletes an expense from the database.
     * @param expense The expense object to delete
     */
    @Delete
    suspend fun deleteExpense(expense: Expense)

    /**
     * Retrieves all expenses from the database, ordered by date (newest first).
     * @return A Flow list of all expenses, which updates when data changes
     */
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    /**
     * Retrieves expenses within a specific date range.
     * Used for filtering expenses by time period (e.g., monthly reports).
     * @param startDate The beginning of the date range
     * @param endDate The end of the date range
     * @return A Flow list of filtered expenses, ordered by date (newest first)
     */
    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Date, endDate: Date): Flow<List<Expense>>

    /**
     * Retrieves all expenses for a specific category.
     * Used for category-specific expense analysis.
     * @param categoryId The category ID to filter by
     * @return A Flow list of expenses in the specified category
     */
    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId")
    fun getExpensesByCategory(categoryId: Long): Flow<List<Expense>>

    /**
     * Finds a specific expense by its ID.
     * Used when viewing or editing expense details.
     * @param expenseId The ID of the expense to retrieve
     * @return The matching expense or null if none exists
     */
    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: Long): Expense?

    /**
     * Calculates the total amount spent in a specific category within a date range.
     * Used for budget analysis and reporting.
     * @param categoryId The category ID to sum expenses for
     * @param startDate The beginning of the date range
     * @param endDate The end of the date range
     * @return The sum of expenses or null if no expenses found
     */
    @Query("SELECT SUM(amount) FROM expenses WHERE categoryId = :categoryId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpenseByCategory(categoryId: Long, startDate: Date, endDate: Date): Double?

    /**
     * Calculates the total amount of all expenses within a date range.
     * Used for overall spending analysis.
     * @param startDate The beginning of the date range
     * @param endDate The end of the date range
     * @return The sum of all expenses or null if no expenses found
     */
    @Query("SELECT SUM(amount) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpenses(startDate: Date, endDate: Date): Double?
}