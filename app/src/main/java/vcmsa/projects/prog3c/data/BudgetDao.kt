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
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND startDate <= :date AND endDate >= :date")
    suspend fun getBudgetForCategoryAndDate(categoryId: Long, date: Date): Budget?

    @Query("SELECT * FROM budgets WHERE startDate <= :date AND endDate >= :date")
    fun getBudgetsForDate(date: Date): Flow<List<Budget>>
}