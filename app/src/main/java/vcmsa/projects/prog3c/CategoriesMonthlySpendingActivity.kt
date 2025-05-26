package vcmsa.projects.prog3c

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import vcmsa.projects.prog3c.data.FirestoreRepository
import vcmsa.projects.prog3c.data.FirestoreExpense
import vcmsa.projects.prog3c.data.FirestoreCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity for displaying monthly spending summaries categorized by expense categories.
 * Allows users to filter spending by date range and view total spending per category.
 */
class CategoriesMonthlySpendingActivity : AppCompatActivity() {

    // Tag for logging
    private val TAG = "CategoriesMonthlySpending"

    // Firestore repository for data access
    private lateinit var firestoreRepository: FirestoreRepository

    // Date range filter variables
    private var startDate: Date? = null
    private var endDate: Date? = null

    // UI component declarations
    private lateinit var btnBackFromCategories: Button
    private lateinit var btnClearFilter: Button
    private lateinit var btnApplyFilter: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategorySummaryAdapter
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText

    /**
     * Initializes the activity, sets up UI components and event listeners
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_categories_monthly)

        Log.d(TAG, "onCreate started")

        // Initialize Firestore repository
        firestoreRepository = FirestoreRepository.getInstance()

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

        // Set default dates (last 30 days)
        setDefaultDates()
        updateDateText()

        // Load initial data (all categories)
        loadCategories()

        // Set up button click listeners
        btnClearFilter.setOnClickListener {
            clearFilters()
        }
        btnApplyFilter.setOnClickListener {
            filterCategories()
        }
        btnBackFromCategories.setOnClickListener {
            finish()
        }

        Log.d(TAG, "onCreate completed")
    }

    /**
     * Sets default date range to last 30 days
     */
    private fun setDefaultDates() {
        val calendar = Calendar.getInstance()
        endDate = calendar.time // Today

        calendar.add(Calendar.DAY_OF_MONTH, -30) // 30 days ago
        startDate = calendar.time
    }

    /**
     * Clears date filters and shows all time data
     */
    private fun clearFilters() {
        startDate = null
        endDate = null
        etStartDate.setText("")
        etEndDate.setText("")
        loadCategories() // Load all categories without date filter
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
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
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
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
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
        Log.d(TAG, "Loading all categories (no date filter)")
        lifecycleScope.launch {
            try {
                // Combine expenses and categories data flows from Firestore
                combine(
                    firestoreRepository.getAllExpenses(),
                    firestoreRepository.getAllCategories()
                ) { expenses, categories ->
                    Log.d(TAG, "Received ${expenses.size} expenses and ${categories.size} categories")

                    // Create a map of category ID to Category object
                    val categoryMap = categories.associateBy { it.id }

                    // Group expenses by category ID and calculate totals
                    val summaries = expenses.groupBy { it.categoryId }.mapNotNull { (categoryId, expenseGroup) ->
                        val category = categoryMap[categoryId]
                        if (category != null) {
                            val total = expenseGroup.sumOf { it.amount }
                            Log.d(TAG, "Category ${category.name}: R$total")
                            CategorySummaryAdapter.CategorySummary(
                                name = category.name,
                                totalSpent = total,
                                color = category.color
                            )
                        } else {
                            Log.w(TAG, "Category not found for ID: $categoryId")
                            null
                        }
                    }.sortedByDescending { it.totalSpent } // Sort by highest spending first

                    summaries
                }.collect { summaries ->
                    Log.d(TAG, "Updating adapter with ${summaries.size} summaries")
                    // Update the RecyclerView with the new data
                    adapter.submitList(summaries)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading categories", e)
                Toast.makeText(this@CategoriesMonthlySpendingActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
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

        if (start == null || end == null) {
            Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Filtering categories from $start to $end")

        lifecycleScope.launch {
            try {
                combine(
                    firestoreRepository.getExpensesByDateRange(start, end),
                    firestoreRepository.getAllCategories()
                ) { expenses, categories ->
                    Log.d(TAG, "Filtered ${expenses.size} expenses and ${categories.size} categories")

                    val categoryMap = categories.associateBy { it.id }

                    // Group filtered expenses by category
                    val summaries = expenses.groupBy { it.categoryId }.mapNotNull { (categoryId, expenseGroup) ->
                        val category = categoryMap[categoryId]
                        if (category != null) {
                            val total = expenseGroup.sumOf { it.amount }
                            Log.d(TAG, "Filtered - Category ${category.name}: R$total")
                            CategorySummaryAdapter.CategorySummary(
                                name = category.name,
                                totalSpent = total,
                                color = category.color
                            )
                        } else {
                            Log.w(TAG, "Category not found for ID: $categoryId")
                            null
                        }
                    }.sortedByDescending { it.totalSpent } // Sort by highest spending first

                    summaries
                }.collect { summaries ->
                    Log.d(TAG, "Updating adapter with ${summaries.size} filtered summaries")
                    adapter.submitList(summaries)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error filtering categories", e)
                Toast.makeText(this@CategoriesMonthlySpendingActivity, "Error filtering data: ${e.message}", Toast.LENGTH_SHORT).show()
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
        data class CategorySummary(
            val name: String,
            val totalSpent: Double,
            val color: Int
        )

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