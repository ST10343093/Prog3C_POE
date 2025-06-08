package vcmsa.projects.prog3c

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import vcmsa.projects.prog3c.data.FirestoreRepository
import vcmsa.projects.prog3c.data.FirestoreBudget
import vcmsa.projects.prog3c.data.FirestoreCategory
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Activity for displaying and managing all budgets
 * Shows details of budget targets and actual spending for each category
 */
class BudgetListActivity : AppCompatActivity() {

    // Tag for logging
    private val TAG = "BudgetListActivity"

    // Firestore repository and UI components
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BudgetAdapter
    private lateinit var btnBack: MaterialButton
    private lateinit var emptyState: LinearLayout
    private var categoryMap: Map<String, FirestoreCategory> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_list)

        Log.d(TAG, "=== TESTING LOGS - BudgetListActivity started ===")

        // Initialize Firestore repository
        firestoreRepository = FirestoreRepository.getInstance()

        // Initialize UI components
        recyclerView = findViewById(R.id.rvBudgets)
        btnBack = findViewById(R.id.btnBackFromBudgetList)
        emptyState = findViewById(R.id.llEmptyState)

        // Set up RecyclerView
        adapter = BudgetAdapter(emptyList(), categoryMap, ::deleteBudget)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Load data
        loadCategories()

        Log.d(TAG, "onCreate completed")
    }

    /**
     * Loads all categories from Firestore for budget lookups - FIXED VERSION
     */
    private fun loadCategories() {
        Log.d(TAG, "Loading categories")
        lifecycleScope.launch {
            try {
                firestoreRepository.getAllCategories().collectLatest { categories ->
                    // Check if activity is still active
                    if (!isFinishing && !isDestroyed) {
                        Log.d(TAG, "Categories loaded: ${categories.size}")
                        // Create a map of category ID to Category object for quick lookup
                        categoryMap = categories.associateBy { it.id }

                        // Debug log category IDs
                        categories.forEach { category ->
                            Log.d(TAG, "Category ID: '${category.id}', Name: '${category.name}'")
                        }

                        // Load budgets after categories are loaded
                        loadBudgets()
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Category loading cancelled (normal when leaving screen)")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading categories", e)
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this@BudgetListActivity, "Error loading categories", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Loads all active budgets and calculates current spending - FIXED VERSION
     */
    private fun loadBudgets() {
        Log.d(TAG, "Loading budgets")
        lifecycleScope.launch {
            try {
                firestoreRepository.getAllBudgets().collectLatest { budgets ->
                    // Check if activity is still active
                    if (!isFinishing && !isDestroyed) {
                        Log.d(TAG, "Budgets loaded: ${budgets.size}")

                        if (budgets.isEmpty()) {
                            // Show empty state
                            recyclerView.visibility = View.GONE
                            emptyState.visibility = View.VISIBLE
                        } else {
                            // Show budgets
                            recyclerView.visibility = View.VISIBLE
                            emptyState.visibility = View.GONE

                            // Create a list to hold budget items with spending data
                            val budgetItems = budgets.map { budget ->
                                Log.d(TAG, "Processing budget for category: '${budget.categoryId}'")
                                Log.d(TAG, "Budget period: ${budget.getStartDate()} to ${budget.getEndDate()}")

                                // FIXED: Calculate spending with enhanced debugging and fallback
                                val spending = try {
                                    // Method 1: Try using the repository method
                                    val repositoryTotal = firestoreRepository.getTotalExpenseByCategory(
                                        budget.categoryId,
                                        budget.getStartDate(),
                                        budget.getEndDate()
                                    )

                                    Log.d(TAG, "Repository calculation for category '${budget.categoryId}': $repositoryTotal")

                                    // If repository method returns 0, try manual calculation
                                    if (repositoryTotal == 0.0) {
                                        Log.d(TAG, "Repository returned 0, trying manual calculation...")

                                        // Method 2: Manual calculation as fallback
                                        val allExpenses = firestoreRepository.getAllExpenses().first()
                                        Log.d(TAG, "Total expenses in database: ${allExpenses.size}")

                                        // Debug: Show all expense category IDs
                                        allExpenses.forEach { expense ->
                                            Log.d(TAG, "Expense category ID: '${expense.categoryId}', Amount: ${expense.amount}, Date: ${expense.getDate()}")
                                        }

                                        val categoryExpenses = allExpenses.filter { expense ->
                                            val matchesCategory = expense.categoryId == budget.categoryId
                                            val withinDateRange = expense.getDate() >= budget.getStartDate() &&
                                                    expense.getDate() <= budget.getEndDate()

                                            Log.d(TAG, "Expense check - Category match: $matchesCategory, Date match: $withinDateRange")

                                            matchesCategory && withinDateRange
                                        }

                                        val manualTotal = categoryExpenses.sumOf { it.amount }
                                        Log.d(TAG, "Manual calculation: Found ${categoryExpenses.size} matching expenses, Total: $manualTotal")

                                        manualTotal
                                    } else {
                                        repositoryTotal
                                    }

                                } catch (e: CancellationException) {
                                    Log.d(TAG, "Spending calculation cancelled (normal when leaving screen)")
                                    0.0
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error calculating spending for budget ${budget.id}", e)

                                    // Method 3: Final fallback - simple category matching without date filtering
                                    try {
                                        val allExpenses = firestoreRepository.getAllExpenses().first()
                                        val categoryExpenses = allExpenses.filter { it.categoryId == budget.categoryId }
                                        val fallbackTotal = categoryExpenses.sumOf { it.amount }
                                        Log.d(TAG, "Fallback calculation (no date filter): $fallbackTotal")
                                        fallbackTotal
                                    } catch (e2: Exception) {
                                        Log.e(TAG, "All calculation methods failed", e2)
                                        0.0
                                    }
                                }

                                Log.d(TAG, "Final spending calculation for budget ${budget.id}: $spending")

                                // Create a BudgetItem with the budget and its spending
                                BudgetAdapter.BudgetItem(
                                    budget = budget,
                                    currentSpending = spending
                                )
                            }

                            // Update the adapter with the new data
                            adapter.updateBudgets(budgetItems, categoryMap)
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Budget loading cancelled (normal when leaving screen)")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading budgets", e)
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this@BudgetListActivity, "Error loading budgets", Toast.LENGTH_SHORT).show()

                    // Show empty state on error
                    recyclerView.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * Deletes a budget when the user clicks the delete button
     */
    private fun deleteBudget(budget: FirestoreBudget) {
        Log.d(TAG, "Deleting budget: ${budget.id}")
        lifecycleScope.launch {
            try {
                firestoreRepository.deleteBudget(budget.id)
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this@BudgetListActivity, "Budget deleted", Toast.LENGTH_SHORT).show()
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Budget deletion cancelled (normal when leaving screen)")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting budget", e)
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this@BudgetListActivity, "Error deleting budget: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Adapter for displaying budget items in the RecyclerView - Updated for modern layout
     */
    class BudgetAdapter(
        private var budgetItems: List<BudgetItem>,
        private var categoryMap: Map<String, FirestoreCategory>,
        private val onDeleteClick: (FirestoreBudget) -> Unit
    ) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

        /**
         * Data class to hold a budget and its current spending amount
         */
        data class BudgetItem(
            val budget: FirestoreBudget,
            val currentSpending: Double
        )

        /**
         * Updates the adapter with new budget data
         */
        fun updateBudgets(newBudgetItems: List<BudgetItem>, newCategoryMap: Map<String, FirestoreCategory>) {
            budgetItems = newBudgetItems
            categoryMap = newCategoryMap
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_budget, parent, false)
            return BudgetViewHolder(view)
        }

        override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
            val budgetItem = budgetItems[position]
            val budget = budgetItem.budget
            val category = categoryMap[budget.categoryId]
            holder.bind(budgetItem, category)
        }

        override fun getItemCount(): Int = budgetItems.size

        /**
         * ViewHolder for budget items - Updated for modern layout
         */
        inner class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvCategory: TextView = itemView.findViewById(R.id.tvBudgetCategory)
            private val tvAmount: TextView = itemView.findViewById(R.id.tvBudgetAmount)
            private val tvPeriod: TextView = itemView.findViewById(R.id.tvBudgetPeriod)
            private val tvSpent: TextView = itemView.findViewById(R.id.tvBudgetSpent)
            private val tvRemaining: TextView = itemView.findViewById(R.id.tvBudgetRemaining)
            private val tvBudgetStatus: TextView = itemView.findViewById(R.id.tvBudgetStatus)
            private val progressBarContainer: MaterialCardView = itemView.findViewById(R.id.progressBarContainer)
            private val progressBar: View = itemView.findViewById(R.id.progressBar)
            private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteBudget)

            /**
             * Binds budget data to the view with modern styling
             */
            fun bind(budgetItem: BudgetItem, category: FirestoreCategory?) {
                val budget = budgetItem.budget
                val spending = budgetItem.currentSpending
                val remaining = budget.maximumAmount - spending
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                // Set category name
                if (category != null) {
                    tvCategory.text = category.name
                } else {
                    tvCategory.text = "Unknown Category"
                }

                // Format budget display with Rand (R) currency
                tvAmount.text = "Budget: R%.2f - R%.2f".format(budget.minimumAmount, budget.maximumAmount)
                tvPeriod.text = "%s - %s".format(
                    dateFormat.format(budget.getStartDate()),
                    dateFormat.format(budget.getEndDate())
                )
                tvSpent.text = "Spent: R%.2f".format(spending)

                // Calculate progress percentage for progress bar
                val progressPercentage = if (budget.maximumAmount > 0) {
                    (spending / budget.maximumAmount * 100).coerceAtMost(100.0)
                } else {
                    0.0
                }

                // Update progress bar width using ViewGroup.LayoutParams
                progressBarContainer.post {
                    val containerWidth = progressBarContainer.width
                    if (containerWidth > 0) {
                        val layoutParams = progressBar.layoutParams
                        layoutParams.width = (containerWidth * progressPercentage / 100).toInt().coerceAtLeast(1)
                        progressBar.layoutParams = layoutParams
                    }
                }

                // Set status and colors based on spending vs budget
                when {
                    spending > budget.maximumAmount -> {
                        // Over budget - show in red
                        tvRemaining.setTextColor(android.graphics.Color.RED)
                        tvRemaining.text = "Over Budget: R%.2f".format(spending - budget.maximumAmount)
                        tvBudgetStatus.text = "OVER BUDGET"
                        tvBudgetStatus.setBackgroundResource(R.drawable.pill_background_red)
                        progressBar.setBackgroundColor(android.graphics.Color.RED)
                    }
                    spending > budget.minimumAmount -> {
                        // Between min and max - show in orange
                        tvRemaining.setTextColor(android.graphics.Color.rgb(255, 165, 0)) // Orange
                        tvRemaining.text = "Remaining: R%.2f".format(remaining)
                        tvBudgetStatus.text = "CLOSE"
                        tvBudgetStatus.setBackgroundResource(R.drawable.pill_background_orange)
                        progressBar.setBackgroundColor(android.graphics.Color.rgb(255, 165, 0))
                    }
                    else -> {
                        // Under minimum - show in green
                        tvRemaining.setTextColor(android.graphics.Color.GREEN)
                        tvRemaining.text = "Under Target: R%.2f".format(budget.minimumAmount - spending)
                        tvBudgetStatus.text = "ON TRACK"
                        tvBudgetStatus.setBackgroundResource(R.drawable.pill_background_green)
                        progressBar.setBackgroundColor(android.graphics.Color.GREEN)
                    }
                }

                // Set up delete button
                btnDelete.setOnClickListener {
                    onDeleteClick(budget)
                }
            }
        }
    }
}