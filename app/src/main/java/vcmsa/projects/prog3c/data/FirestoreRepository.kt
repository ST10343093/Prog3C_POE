package vcmsa.projects.prog3c.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Repository class for Firestore operations
 * This replaces the Room DAOs for online database storage
 */
class FirestoreRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Get current user ID
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
    }

    // Collection references for current user
    private fun getCategoriesCollection() =
        firestore.collection("users").document(getCurrentUserId()).collection("categories")

    private fun getExpensesCollection() =
        firestore.collection("users").document(getCurrentUserId()).collection("expenses")

    private fun getBudgetsCollection() =
        firestore.collection("users").document(getCurrentUserId()).collection("budgets")

    // ==================== CATEGORY OPERATIONS ====================

    /**
     * Insert a new category
     */
    suspend fun insertCategory(category: FirestoreCategory): String {
        val userId = getCurrentUserId()
        val categoryWithUser = category.copy(userId = userId)
        val docRef = getCategoriesCollection().add(categoryWithUser.toMap()).await()
        return docRef.id
    }

    /**
     * Update an existing category
     */
    suspend fun updateCategory(category: FirestoreCategory) {
        val userId = getCurrentUserId()
        val updatedCategory = category.copy(userId = userId)
        getCategoriesCollection().document(category.id).set(updatedCategory.toMap()).await()
    }

    /**
     * Delete a category
     */
    suspend fun deleteCategory(categoryId: String) {
        getCategoriesCollection().document(categoryId).delete().await()
    }

    /**
     * Get all categories as Flow
     */
    fun getAllCategories(): Flow<List<FirestoreCategory>> = callbackFlow {
        val listener = getCategoriesCollection()
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreRepository", "Error loading categories", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val categories = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data
                        if (data != null) {
                            FirestoreCategory(
                                id = doc.id,
                                name = data["name"] as? String ?: "",
                                color = when (val colorValue = data["color"]) {
                                    is Long -> colorValue.toInt()
                                    is Int -> colorValue
                                    else -> 0
                                },
                                userId = data["userId"] as? String ?: ""
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreRepository", "Error parsing category: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                android.util.Log.d("FirestoreRepository", "Loaded ${categories.size} categories")
                trySend(categories)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get category by ID
     */
    suspend fun getCategoryById(categoryId: String): FirestoreCategory? {
        return try {
            val doc = getCategoriesCollection().document(categoryId).get().await()
            val data = doc.data
            if (data != null) {
                FirestoreCategory(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    color = when (val colorValue = data["color"]) {
                        is Long -> colorValue.toInt()
                        is Int -> colorValue
                        else -> 0
                    },
                    userId = data["userId"] as? String ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Error getting category by ID: $categoryId", e)
            null
        }
    }

    // ==================== EXPENSE OPERATIONS ====================

    /**
     * Insert a new expense
     */
    suspend fun insertExpense(expense: FirestoreExpense): String {
        val userId = getCurrentUserId()
        val expenseWithUser = expense.copy(userId = userId)
        val docRef = getExpensesCollection().add(expenseWithUser.toMap()).await()
        return docRef.id
    }

    /**
     * Update an existing expense
     */
    suspend fun updateExpense(expense: FirestoreExpense) {
        val userId = getCurrentUserId()
        val updatedExpense = expense.copy(userId = userId)
        getExpensesCollection().document(expense.id).set(updatedExpense.toMap()).await()
    }

    /**
     * Delete an expense
     */
    suspend fun deleteExpense(expenseId: String) {
        getExpensesCollection().document(expenseId).delete().await()
    }

    /**
     * Get all expenses as Flow, ordered by date (newest first)
     */
    fun getAllExpenses(): Flow<List<FirestoreExpense>> = callbackFlow {
        val listener = getExpensesCollection()
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreRepository", "Error loading expenses", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data
                        if (data != null) {
                            FirestoreExpense(
                                id = doc.id,
                                amount = data["amount"] as? Double ?: 0.0,
                                description = data["description"] as? String ?: "",
                                date = data["date"] as? Timestamp ?: Timestamp.now(),
                                categoryId = data["categoryId"] as? String ?: "",
                                photoPath = data["photoPath"] as? String,
                                userId = data["userId"] as? String ?: ""
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreRepository", "Error parsing expense: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(expenses)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get expenses by date range
     */
    fun getExpensesByDateRange(startDate: Date, endDate: Date): Flow<List<FirestoreExpense>> = callbackFlow {
        val listener = getExpensesCollection()
            .whereGreaterThanOrEqualTo("date", Timestamp(startDate))
            .whereLessThanOrEqualTo("date", Timestamp(endDate))
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreRepository", "Error loading expenses by date range", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data
                        if (data != null) {
                            FirestoreExpense(
                                id = doc.id,
                                amount = data["amount"] as? Double ?: 0.0,
                                description = data["description"] as? String ?: "",
                                date = data["date"] as? Timestamp ?: Timestamp.now(),
                                categoryId = data["categoryId"] as? String ?: "",
                                photoPath = data["photoPath"] as? String,
                                userId = data["userId"] as? String ?: ""
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreRepository", "Error parsing expense: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(expenses)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get expenses by category
     */
    fun getExpensesByCategory(categoryId: String): Flow<List<FirestoreExpense>> = callbackFlow {
        val listener = getExpensesCollection()
            .whereEqualTo("categoryId", categoryId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreRepository", "Error loading expenses by category", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data
                        if (data != null) {
                            FirestoreExpense(
                                id = doc.id,
                                amount = data["amount"] as? Double ?: 0.0,
                                description = data["description"] as? String ?: "",
                                date = data["date"] as? Timestamp ?: Timestamp.now(),
                                categoryId = data["categoryId"] as? String ?: "",
                                photoPath = data["photoPath"] as? String,
                                userId = data["userId"] as? String ?: ""
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreRepository", "Error parsing expense: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(expenses)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get expense by ID
     */
    suspend fun getExpenseById(expenseId: String): FirestoreExpense? {
        return try {
            val doc = getExpensesCollection().document(expenseId).get().await()
            val data = doc.data
            if (data != null) {
                FirestoreExpense(
                    id = doc.id,
                    amount = data["amount"] as? Double ?: 0.0,
                    description = data["description"] as? String ?: "",
                    date = data["date"] as? Timestamp ?: Timestamp.now(),
                    categoryId = data["categoryId"] as? String ?: "",
                    photoPath = data["photoPath"] as? String,
                    userId = data["userId"] as? String ?: ""
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Error getting expense by ID: $expenseId", e)
            null
        }
    }

    /**
     * Get total expense amount by category and date range
     */
    suspend fun getTotalExpenseByCategory(categoryId: String, startDate: Date, endDate: Date): Double {
        return try {
            val snapshot = getExpensesCollection()
                .whereEqualTo("categoryId", categoryId)
                .whereGreaterThanOrEqualTo("date", Timestamp(startDate))
                .whereLessThanOrEqualTo("date", Timestamp(endDate))
                .get()
                .await()

            snapshot.documents.sumOf { doc ->
                doc.getDouble("amount") ?: 0.0
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Error calculating total expense by category", e)
            0.0
        }
    }

    /**
     * Get total expenses for date range
     */
    suspend fun getTotalExpenses(startDate: Date, endDate: Date): Double {
        return try {
            val snapshot = getExpensesCollection()
                .whereGreaterThanOrEqualTo("date", Timestamp(startDate))
                .whereLessThanOrEqualTo("date", Timestamp(endDate))
                .get()
                .await()

            snapshot.documents.sumOf { doc ->
                doc.getDouble("amount") ?: 0.0
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Error calculating total expenses", e)
            0.0
        }
    }

    // ==================== BUDGET OPERATIONS ====================

    /**
     * Insert a new budget
     */
    suspend fun insertBudget(budget: FirestoreBudget): String {
        val userId = getCurrentUserId()
        val budgetWithUser = budget.copy(userId = userId)
        val docRef = getBudgetsCollection().add(budgetWithUser.toMap()).await()
        return docRef.id
    }

    /**
     * Update an existing budget
     */
    suspend fun updateBudget(budget: FirestoreBudget) {
        val userId = getCurrentUserId()
        val updatedBudget = budget.copy(userId = userId)
        getBudgetsCollection().document(budget.id).set(updatedBudget.toMap()).await()
    }

    /**
     * Delete a budget
     */
    suspend fun deleteBudget(budgetId: String) {
        getBudgetsCollection().document(budgetId).delete().await()
    }

    /**
     * Get all budgets as Flow
     */
    fun getAllBudgets(): Flow<List<FirestoreBudget>> = callbackFlow {
        val listener = getBudgetsCollection()
            .orderBy("startDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreRepository", "Error loading budgets", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val budgets = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data
                        if (data != null) {
                            FirestoreBudget(
                                id = doc.id,
                                minimumAmount = data["minimumAmount"] as? Double ?: 0.0,
                                maximumAmount = data["maximumAmount"] as? Double ?: 0.0,
                                categoryId = data["categoryId"] as? String ?: "",
                                startDate = data["startDate"] as? Timestamp ?: Timestamp.now(),
                                endDate = data["endDate"] as? Timestamp ?: Timestamp.now(),
                                userId = data["userId"] as? String ?: ""
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreRepository", "Error parsing budget: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(budgets)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get budget for category and date
     */
    suspend fun getBudgetForCategoryAndDate(categoryId: String, date: Date): FirestoreBudget? {
        return try {
            val timestamp = Timestamp(date)
            val snapshot = getBudgetsCollection()
                .whereEqualTo("categoryId", categoryId)
                .whereLessThanOrEqualTo("startDate", timestamp)
                .whereGreaterThanOrEqualTo("endDate", timestamp)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.let { doc ->
                val data = doc.data
                if (data != null) {
                    FirestoreBudget(
                        id = doc.id,
                        minimumAmount = data["minimumAmount"] as? Double ?: 0.0,
                        maximumAmount = data["maximumAmount"] as? Double ?: 0.0,
                        categoryId = data["categoryId"] as? String ?: "",
                        startDate = data["startDate"] as? Timestamp ?: Timestamp.now(),
                        endDate = data["endDate"] as? Timestamp ?: Timestamp.now(),
                        userId = data["userId"] as? String ?: ""
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreRepository", "Error getting budget for category and date", e)
            null
        }
    }

    /**
     * Get budgets for specific date
     */
    fun getBudgetsForDate(date: Date): Flow<List<FirestoreBudget>> = callbackFlow {
        val timestamp = Timestamp(date)
        val listener = getBudgetsCollection()
            .whereLessThanOrEqualTo("startDate", timestamp)
            .whereGreaterThanOrEqualTo("endDate", timestamp)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreRepository", "Error loading budgets for date", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val budgets = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data
                        if (data != null) {
                            FirestoreBudget(
                                id = doc.id,
                                minimumAmount = data["minimumAmount"] as? Double ?: 0.0,
                                maximumAmount = data["maximumAmount"] as? Double ?: 0.0,
                                categoryId = data["categoryId"] as? String ?: "",
                                startDate = data["startDate"] as? Timestamp ?: Timestamp.now(),
                                endDate = data["endDate"] as? Timestamp ?: Timestamp.now(),
                                userId = data["userId"] as? String ?: ""
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreRepository", "Error parsing budget: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(budgets)
            }

        awaitClose { listener.remove() }
    }

    companion object {
        @Volatile
        private var INSTANCE: FirestoreRepository? = null

        fun getInstance(): FirestoreRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = FirestoreRepository()
                INSTANCE = instance
                instance
            }
        }
    }
}