package vcmsa.projects.prog3c

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    private lateinit var btnBack: androidx.appcompat.widget.AppCompatButton
    private var categoryMap: Map<String, FirestoreCategory> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_list)

        Log.d(TAG, "onCreate started")

        // Initialize Firestore repository
        firestoreRepository = FirestoreRepository.getInstance()

        // Initialize UI components
        recyclerView = findViewById(R.id.rvBudgets)
        btnBack = findViewById(R.id.btnBackFromBudgetList)

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
     * Loads all categories from Firestore for budget lookups
     */
    private fun loadCategories() {
        Log.d(TAG, "Loading categories")
        lifecycleScope.launch {
            try {
                firestoreRepository.getAllCategories().collectLatest { categories ->
                    Log.d(TAG, "Categories loaded: ${categories.size}")
                    // Create a map of category ID to Category object for quick lookup
                    categoryMap = categories.associateBy { it.id }
                    // Load budgets after categories are loaded
                    loadBudgets()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading categories", e)
                Toast.makeText(this@BudgetListActivity, "Error loading categories", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Loads all active budgets and calculates current spending
     */
    private fun loadBudgets() {
        Log.d(TAG, "Loading budgets")
        lifecycleScope.launch {
            try {
                firestoreRepository.getAllBudgets().collectLatest { budgets ->
                    Log.d(TAG, "Budgets loaded: ${budgets.size}")

                    // Create a list to hold budget items with spending data
                    val budgetItems = budgets.map { budget ->
                        // Calculate total spending for this category in the budget period
                        val spending = try {
                            firestoreRepository.getTotalExpenseByCategory(
                                budget.categoryId,
                                budget.getStartDate(),
                                budget.getEndDate()
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error calculating spending for budget ${budget.id}", e)
                            0.0
                        }

                        Log.d(TAG, "Budget ${budget.id}: spending = $spending")

                        // Create a BudgetItem with the budget and its spending
                        BudgetAdapter.BudgetItem(
                            budget = budget,
                            currentSpending = spending
                        )
                    }

                    // Update the adapter with the new data
                    adapter.updateBudgets(budgetItems, categoryMap)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading budgets", e)
                Toast.makeText(this@BudgetListActivity, "Error loading budgets", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Deletes a budget when the user clicks the delete button
     *
     * @param budget The budget to delete
     */
    private fun deleteBudget(budget: FirestoreBudget) {
        Log.d(TAG, "Deleting budget: ${budget.id}")
        lifecycleScope.launch {
            try {
                firestoreRepository.deleteBudget(budget.id)
                Toast.makeText(this@BudgetListActivity, "Budget deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting budget", e)
                Toast.makeText(this@BudgetListActivity, "Error deleting budget: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Adapter for displaying budget items in the RecyclerView
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
         * ViewHolder for budget items
         */
        inner class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvCategory: TextView = itemView.findViewById(R.id.tvBudgetCategory)
            private val tvAmount: TextView = itemView.findViewById(R.id.tvBudgetAmount)
            private val tvPeriod: TextView = itemView.findViewById(R.id.tvBudgetPeriod)
            private val tvSpent: TextView = itemView.findViewById(R.id.tvBudgetSpent)
            private val tvRemaining: TextView = itemView.findViewById(R.id.tvBudgetRemaining)
            private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteBudget)

            /**
             * Binds budget data to the view
             */
            fun bind(budgetItem: BudgetItem, category: FirestoreCategory?) {
                val budget = budgetItem.budget
                val spending = budgetItem.currentSpending
                val remaining = budget.maximumAmount - spending
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                // Set category name and color
                if (category != null) {
                    tvCategory.text = category.name
                    tvCategory.setBackgroundColor(category.color)
                } else {
                    tvCategory.text = "Unknown Category"
                    tvCategory.setBackgroundColor(android.graphics.Color.GRAY)
                }

                // Format budget display with Rand (R) currency
                tvAmount.text = "Budget: R%.2f - R%.2f".format(budget.minimumAmount, budget.maximumAmount)
                tvPeriod.text = "%s - %s".format(
                    dateFormat.format(budget.getStartDate()),
                    dateFormat.format(budget.getEndDate())
                )
                tvSpent.text = "Spent: R%.2f".format(spending)
                tvRemaining.text = "Remaining: R%.2f".format(remaining)

                // Set text color based on spending vs budget
                when {
                    spending > budget.maximumAmount -> {
                        // Over budget - show in red
                        tvRemaining.setTextColor(android.graphics.Color.RED)
                        tvRemaining.text = "Over Budget: R%.2f".format(spending - budget.maximumAmount)
                    }
                    spending > budget.minimumAmount -> {
                        // Between min and max - show in orange
                        tvRemaining.setTextColor(android.graphics.Color.rgb(255, 165, 0)) // Orange
                        tvRemaining.text = "Remaining: R%.2f".format(remaining)
                    }
                    else -> {
                        // Under minimum - show in green
                        tvRemaining.setTextColor(android.graphics.Color.GREEN)
                        tvRemaining.text = "Under Target: R%.2f".format(budget.minimumAmount - spending)
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