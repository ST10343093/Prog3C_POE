package vcmsa.projects.prog3c

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import vcmsa.projects.prog3c.data.AppDatabase
import vcmsa.projects.prog3c.data.Expense
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity for displaying monthly spending summaries categorized by expense categories.
 * Allows users to filter spending by date range and view total spending per category.
 */
class CategoriesMonthlySpendingActivity : AppCompatActivity() {

    // Database instance for data access
    private lateinit var database: AppDatabase

    // Date range filter variables
    private var startDate: Date? = Date()
    private var endDate: Date? = Date()

    // UI component declarations
    private lateinit var btnBackFromCategories: Button
    private lateinit var btnClearFilter: Button
    private lateinit var btnApplyFilter: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategorySummaryAdapter
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText

    // Storage for expense data
    private var expenseList: List<Expense> = emptyList()

    /**
     * Initializes the activity, sets up UI components and event listeners
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_categories_monthly)

        // Initialize Room database
        database = AppDatabase.getDatabase(this)

        // Initialize UI components
        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)
        btnBackFromCategories = findViewById(R.id.btnBack)
        btnClearFilter = findViewById(R.id.buttonClear)
        btnApplyFilter = findViewById(R.id.buttonApplyFilter)

        // Set up RecyclerView with adapter
        recyclerView = findViewById(R.id.rvExpenses)
        adapter = CategorySummaryAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Configure date pickers
        setupDatePicker()
        updateDateText()

        // Load initial data
        loadCategories()

        // Set up button click listeners
        btnClearFilter.setOnClickListener {
            loadCategories()
        }
        btnApplyFilter.setOnClickListener {
            filterCategories()
        }
        btnBackFromCategories.setOnClickListener {
            finish()
        }
    }

    /**
     * Configures date picker dialogs for the start and end date inputs
     */
    private fun setupDatePicker() {
        // Configure start date picker dialog
        etStartDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = startDate ?: Date()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    startDate = calendar.time
                    updateDateText()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        // Configure end date picker dialog
        etEndDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = endDate ?: Date()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    endDate = calendar.time
                    updateDateText()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    /**
     * Updates the text fields with formatted dates
     */
    private fun updateDateText() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        startDate?.let { etStartDate.setText(dateFormat.format(it)) }
        endDate?.let { etEndDate.setText(dateFormat.format(it)) }
    }

    /**
     * Loads all categories and their associated expenses
     * Calculates the total spent in each category and displays the summaries
     */
    private fun loadCategories() {
        lifecycleScope.launch {
            // Combine expenses and categories data flows
            combine(
                database.expenseDao().getAllExpenses(),
                database.categoryDao().getAllCategories()
            ) { expenses, categories ->
                // Create a map of category ID to Category object
                val categoryMap = categories.associateBy({ it.id }, { it })

                // Group expenses by category ID and calculate totals
                val summaries = expenses.groupBy { it.categoryId }.mapNotNull { (categoryId, expenseGroup) ->
                    val category = categoryMap[categoryId] ?: return@mapNotNull null
                    val total = expenseGroup.sumOf { it.amount }
                    CategorySummaryAdapter.CategorySummary(
                        name = category.name,
                        totalSpent = total,
                        color = category.color
                    )
                }
                summaries
            }.collect { summaries ->
                // Update the RecyclerView with the new data
                adapter.submitList(summaries)
            }
        }
    }

    /**
     * Filters expenses by the selected date range
     * Only includes expenses with dates between startDate and endDate
     */
    private fun filterCategories() {
        val start = startDate
        val end = endDate
        if (start == null || end == null) return

        lifecycleScope.launch {
            combine(
                database.expenseDao().getAllExpenses(),
                database.categoryDao().getAllCategories()
            ) { expenses, categories ->
                val categoryMap = categories.associateBy({ it.id }, { it })

                // Filter expenses within the selected date range
                val filtered = expenses.filter { it.date in start..end }

                // Group filtered expenses by category
                val summaries = filtered.groupBy { it.categoryId }.mapNotNull { (categoryId, expenseGroup) ->
                    val category = categoryMap[categoryId] ?: return@mapNotNull null
                    val total = expenseGroup.sumOf { it.amount }
                    CategorySummaryAdapter.CategorySummary(
                        name = category.name,
                        totalSpent = total,
                        color = category.color
                    )
                }
                summaries
            }.collect { summaries ->
                adapter.submitList(summaries)
            }
        }
    }

    /**
     * RecyclerView adapter for displaying category spending summaries
     */
    class CategorySummaryAdapter : RecyclerView.Adapter<CategorySummaryAdapter.SummaryViewHolder>() {

        private var items = listOf<CategorySummary>()

        /**
         * Data class representing a category spending summary
         */
        data class CategorySummary(val name: String, val totalSpent: Double, val color: Int)

        /**
         * ViewHolder for category summary items
         */
        inner class SummaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val categoryName: TextView = view.findViewById(R.id.textCategoryName)
            val totalAmount: TextView = view.findViewById(R.id.textAmount)
            val colorView: View = view.findViewById(R.id.viewCategoryColor)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_categroty_expense, parent, false)
            return SummaryViewHolder(view)
        }

        /**
         * Binds category summary data to the ViewHolder
         */
        override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
            val item = items[position]
            holder.categoryName.text = item.name
            // Use Rand (R) symbol for South African currency
            holder.totalAmount.text = "R%.2f".format(item.totalSpent)
            holder.colorView.setBackgroundColor(item.color)
        }

        override fun getItemCount(): Int = items.size

        /**
         * Updates the adapter with a new list of category summaries
         */
        fun submitList(newList: List<CategorySummary>) {
            items = newList
            notifyDataSetChanged()
        }
    }
}