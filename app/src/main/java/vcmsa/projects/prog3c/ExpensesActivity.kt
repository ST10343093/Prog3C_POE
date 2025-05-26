package vcmsa.projects.prog3c

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import vcmsa.projects.prog3c.data.FirestoreRepository
import vcmsa.projects.prog3c.data.FirestoreCategory
import vcmsa.projects.prog3c.data.FirestoreExpense
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity for viewing expenses with date range filtering
 * Shows a list of all expenses for the specified period and displays the total amount
 */
class ExpensesActivity : AppCompatActivity() {

    // Tag for logging
    private val TAG = "ExpensesActivity"

    // Firestore repository reference and UI components
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var btnApplyFilter: Button
    private lateinit var btnClearFilter: Button
    private lateinit var tvTotalExpenses: TextView
    private lateinit var rvExpenses: RecyclerView
    private lateinit var btnBack: Button
    private lateinit var fabAddExpense: ExtendedFloatingActionButton  // Added FAB
    private lateinit var adapter: ExpenseAdapter

    // Default date range - previous month to current date
    private var startDate: Date = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1) // Default to 1 month ago
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.time

    private var endDate: Date = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }.time

    // Date formatter for UI
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Map to store category information by ID
    private var categoryMap: Map<String, FirestoreCategory> = emptyMap()

    /**
     * Initialize the activity, set up UI components and event handlers
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_expenses)

        Log.d(TAG, "onCreate started")

        // Initialize Firestore repository
        firestoreRepository = FirestoreRepository.getInstance()

        // Initialize views
        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)
        btnApplyFilter = findViewById(R.id.btnApplyFilter)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        rvExpenses = findViewById(R.id.rvExpenses)
        btnBack = findViewById(R.id.btnBack)

        // Initialize FAB - This was missing in your code
        fabAddExpense = findViewById(R.id.fabAddExpense)

        // Initialize clear filter button if it exists in your layout
        try {
            btnClearFilter = findViewById(R.id.btnClearFilter)
            btnClearFilter?.setOnClickListener {
                clearFilters()
            }
        } catch (e: Exception) {
            Log.d(TAG, "Clear filter button not found in layout")
            // Clear button doesn't exist in layout, that's okay
        }

        // Set up date fields with default values
        updateDateFields()

        // Set up date pickers for input fields
        etStartDate.setOnClickListener { showDatePicker(true) }
        etEndDate.setOnClickListener { showDatePicker(false) }

        // Set up filter button to reload expenses with selected date range
        btnApplyFilter.setOnClickListener {
            loadExpenses()
        }

        // Set up back button to close activity
        btnBack.setOnClickListener {
            finish()
        }

        // **FIX: Set up FAB click listener to add new expense**
        fabAddExpense.setOnClickListener {
            Log.d(TAG, "FAB clicked - navigating to AddExpenseActivity")
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivity(intent)
        }

        // Set up RecyclerView with custom adapter
        adapter = ExpenseAdapter(emptyList(), emptyMap(), ::onExpenseClick)
        rvExpenses.adapter = adapter
        rvExpenses.layoutManager = LinearLayoutManager(this)

        // Load initial data
        loadCategories()

        Log.d(TAG, "onCreate completed")
    }

    /**
     * Clears the date filters and reloads all expenses
     */
    private fun clearFilters() {
        Log.d(TAG, "Clearing filters")
        // Reset to default date range (last month to now)
        startDate = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time

        endDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.time

        updateDateFields()
        loadExpenses()
    }

    /**
     * Updates date input fields with formatted dates
     */
    private fun updateDateFields() {
        etStartDate.setText(dateFormatter.format(startDate))
        etEndDate.setText(dateFormatter.format(endDate))
    }

    /**
     * Shows date picker dialog for selecting start or end date
     *
     * @param isStartDate True if selecting start date, false if selecting end date
     */
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        calendar.time = if (isStartDate) startDate else endDate

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                if (isStartDate) {
                    // Set start date to beginning of day (00:00:00)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    startDate = calendar.time
                    etStartDate.setText(dateFormatter.format(startDate))
                } else {
                    // Set end date to end of day (23:59:59)
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    endDate = calendar.time
                    etEndDate.setText(dateFormatter.format(endDate))
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    /**
     * Loads all categories from Firestore and creates a map for quick lookup
     * Then loads expenses after categories are loaded
     */
    private fun loadCategories() {
        Log.d(TAG, "Loading categories")
        lifecycleScope.launch {
            try {
                firestoreRepository.getAllCategories().collectLatest { categories ->
                    // Create a map of category ID to Category object for quick lookup
                    categoryMap = categories.associateBy { it.id }
                    Log.d(TAG, "Categories loaded: ${categories.size}")
                    // Load expenses after categories are loaded
                    loadExpenses()
                }
            } catch (e: Exception) {
                // Don't show error messages for cancellation (normal when leaving screen)
                if (e is CancellationException) {
                    Log.d(TAG, "Category loading cancelled (normal when leaving screen)")
                    return@launch
                }

                Log.e(TAG, "Error loading categories", e)
                Toast.makeText(this@ExpensesActivity, "Error loading categories", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Loads expenses from Firestore filtered by the current date range
     * Updates the adapter with the loaded expenses and calculates the total
     */
    private fun loadExpenses() {
        Log.d(TAG, "Loading expenses from $startDate to $endDate")
        lifecycleScope.launch {
            try {
                firestoreRepository.getExpensesByDateRange(startDate, endDate).collectLatest { expenses ->
                    Log.d(TAG, "Expenses loaded: ${expenses.size}")
                    // Update the adapter with the new expenses and category map
                    adapter.updateExpenses(expenses, categoryMap)

                    // Calculate and display the total amount in Rand
                    val total = expenses.sumOf { it.amount }
                    tvTotalExpenses.text = "Total: R" + String.format("%.2f", total)
                }
            } catch (e: Exception) {
                // Don't show error messages for cancellation (normal when leaving screen)
                if (e is CancellationException) {
                    Log.d(TAG, "Expense loading cancelled (normal when leaving screen)")
                    return@launch
                }

                Log.e(TAG, "Error loading expenses", e)
                Toast.makeText(this@ExpensesActivity, "Error loading expenses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Handles click events on expense items
     * Opens the ExpenseDetailActivity for the clicked expense
     *
     * @param expense The expense that was clicked
     */
    private fun onExpenseClick(expense: FirestoreExpense) {
        try {
            Log.d(TAG, "Expense clicked: ID=${expense.id}, amount=${expense.amount}, description=${expense.description}")

            // Create intent with String ID (not Long)
            val intent = Intent(this, ExpenseDetailActivity::class.java)
            intent.putExtra("EXPENSE_ID", expense.id) // This is now a String, not Long

            // Add flags to ensure proper navigation
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

            Log.d(TAG, "Starting ExpenseDetailActivity with expense ID: ${expense.id}")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening expense details", e)
            Toast.makeText(this, "Error opening expense details: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Refresh the expenses list when returning from AddExpenseActivity
     */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - refreshing expenses")
        // Reload expenses to show any new additions
        if (::firestoreRepository.isInitialized) {
            loadExpenses()
        }
    }

    /**
     * Custom adapter for displaying expenses in a RecyclerView
     *
     * @param expenses Initial list of expenses to display
     * @param categoryMap Map of category IDs to Category objects
     * @param onExpenseClick Callback function for expense item click events
     */
    class ExpenseAdapter(
        private var expenses: List<FirestoreExpense>,
        private var categoryMap: Map<String, FirestoreCategory>,
        private val onExpenseClick: (FirestoreExpense) -> Unit
    ) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

        /**
         * Updates the adapter with new expense and category data
         *
         * @param newExpenses Updated list of expenses
         * @param newCategoryMap Updated map of category IDs to Category objects
         */
        fun updateExpenses(newExpenses: List<FirestoreExpense>, newCategoryMap: Map<String, FirestoreCategory>) {
            expenses = newExpenses
            categoryMap = newCategoryMap
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_expense, parent, false)
            return ExpenseViewHolder(view)
        }

        override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
            val expense = expenses[position]
            holder.bind(expense, categoryMap[expense.categoryId])
        }

        override fun getItemCount(): Int = expenses.size

        /**
         * ViewHolder for expense items
         * Binds expense data to the item view
         */
        inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val amountTextView: TextView = itemView.findViewById(R.id.tvExpenseAmount)
            private val dateTextView: TextView = itemView.findViewById(R.id.tvExpenseDate)
            private val descriptionTextView: TextView = itemView.findViewById(R.id.tvExpenseDescription)
            private val categoryTextView: TextView = itemView.findViewById(R.id.tvExpenseCategory)
            private val photoIndicator: ImageView = itemView.findViewById(R.id.ivHasPhoto)

            /**
             * Binds expense data to the views
             *
             * @param expense The expense to display
             * @param category The category associated with the expense
             */
            fun bind(expense: FirestoreExpense, category: FirestoreCategory?) {
                // Display amount in Rand with proper formatting
                amountTextView.text = "R" + String.format("%.2f", expense.amount)
                dateTextView.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expense.getDate())
                descriptionTextView.text = expense.description

                if (category != null) {
                    categoryTextView.text = category.name
                    categoryTextView.setBackgroundColor(category.color)
                } else {
                    categoryTextView.text = "Unknown"
                    categoryTextView.setBackgroundColor(android.graphics.Color.GRAY)
                }

                // Show photo indicator icon if the expense has a photo
                if (!expense.photoPath.isNullOrEmpty()) {
                    photoIndicator.visibility = View.VISIBLE
                } else {
                    photoIndicator.visibility = View.GONE
                }

                // Set click listener for the entire expense item
                itemView.setOnClickListener {
                    onExpenseClick(expense)
                }
            }
        }
    }
}