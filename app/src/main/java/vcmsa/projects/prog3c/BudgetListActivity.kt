package vcmsa.projects.prog3c

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import vcmsa.projects.prog3c.data.AppDatabase
import vcmsa.projects.prog3c.data.Budget
import vcmsa.projects.prog3c.data.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity for displaying and managing all budgets
 * Shows details of budget targets and actual spending for each category
 */
class BudgetListActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BudgetAdapter
    private lateinit var btnBack: androidx.appcompat.widget.AppCompatButton
    private var categoryMap: Map<Long, Category> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget_list)

        // Initialize database
        database = AppDatabase.getDatabase(this)

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
    }

    /**
     * Loads all categories from the database for budget lookups
     */
    private fun loadCategories() {
        lifecycleScope.launch {
            database.categoryDao().getAllCategories().collectLatest { categories ->
                categoryMap = categories.associateBy { it.id }
                loadBudgets()
            }
        }
    }

    /**
     * Loads all active budgets and calculates current spending
     */
    private fun loadBudgets() {
        lifecycleScope.launch {
            database.budgetDao().getAllBudgets().collectLatest { budgets ->
                // Create a list to hold budget items with spending data
                val budgetItems = budgets.map { budget ->
                    // Calculate total spending for this category in the budget period
                    val spending = withContext(Dispatchers.IO) {
                        database.expenseDao().getTotalExpenseByCategory(
                            budget.categoryId,
                            budget.startDate,
                            budget.endDate
                        ) ?: 0.0
                    }

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

    /**
     * Deletes a budget when the user clicks the delete button
     *
     * @param budget The budget to delete
     */
    private fun deleteBudget(budget: Budget) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.budgetDao().deleteBudget(budget)
            }
        }
    }

    /**
     * Adapter for displaying budget items in the RecyclerView
     */
    class BudgetAdapter(
        private var budgetItems: List<BudgetItem>,
        private var categoryMap: Map<Long, Category>,
        private val onDeleteClick: (Budget) -> Unit
    ) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

        /**
         * Data class to hold a budget and its current spending amount
         */
        data class BudgetItem(
            val budget: Budget,
            val currentSpending: Double
        )

        fun updateBudgets(newBudgetItems: List<BudgetItem>, newCategoryMap: Map<Long, Category>) {
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
            fun bind(budgetItem: BudgetItem, category: Category?) {
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
                    dateFormat.format(budget.startDate),
                    dateFormat.format(budget.endDate)
                )
                tvSpent.text = "Spent: R%.2f".format(spending)
                tvRemaining.text = "Remaining: R%.2f".format(remaining)

                // Set text color based on spending vs budget
                if (spending > budget.maximumAmount) {
                    // Over budget - show in red
                    tvRemaining.setTextColor(android.graphics.Color.RED)
                } else if (spending > budget.minimumAmount) {
                    // Between min and max - show in orange
                    tvRemaining.setTextColor(android.graphics.Color.rgb(255, 165, 0)) // Orange
                } else {
                    // Under minimum - show in green
                    tvRemaining.setTextColor(android.graphics.Color.GREEN)
                }

                // Set up delete button
                btnDelete.setOnClickListener {
                    onDeleteClick(budget)
                }
            }
        }
    }
}