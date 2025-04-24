package vcmsa.projects.prog3c.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: Date, endDate: Date): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId")
    fun getExpensesByCategory(categoryId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: Long): Expense?

    @Query("SELECT SUM(amount) FROM expenses WHERE categoryId = :categoryId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpenseByCategory(categoryId: Long, startDate: Date, endDate: Date): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpenses(startDate: Date, endDate: Date): Double?
}