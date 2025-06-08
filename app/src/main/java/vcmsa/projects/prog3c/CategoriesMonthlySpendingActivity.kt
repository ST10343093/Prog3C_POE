package vcmsa.projects.prog3c

import android.app.DatePickerDialog
import android.graphics.Color
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
import vcmsa.projects.prog3c.data.FirestoreBudget
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Chart imports
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.Description

/**
 * Activity for displaying monthly spending summaries categorized by expense categories.
 * Now includes interactive charts showing spending vs budget goals.
 * Allows users to filter spending by date range and view both chart and table data.
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
    private lateinit var btnToggleView: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategorySummaryAdapter
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText

    // NEW: Chart components
    private lateinit var spendingChart: BarChart
    private lateinit var chartCard: View

    // Data storage for chart
    private var currentCategories: List<CategorySummaryAdapter.CategorySummary> = emptyList()
    private var currentBudgets: List<FirestoreBudget> = emptyList()

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
        btnToggleView = findViewById(R.id.btnToggleView)

        // NEW: Initialize chart components
        spendingChart = findViewById(R.id.spendingChart)
        chartCard = findViewById(R.id.chartCard)

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

        // NEW: Setup chart
        setupChart()

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

        // NEW: Toggle between chart and table view
        btnToggleView.setOnClickListener {
            toggleChartVisibility()
        }

        Log.d(TAG, "onCreate completed")
    }

    /**
     * NEW: Setup and configure the chart appearance
     */
    private fun setupChart() {
        Log.d(TAG, "Setting up chart")

        // Chart general settings
        spendingChart.setDrawBarShadow(false)
        spendingChart.setDrawValueAboveBar(true)
        spendingChart.setPinchZoom(false)
        spendingChart.setDrawGridBackground(false)
        spendingChart.isHighlightPerTapEnabled = true
        spendingChart.setScaleEnabled(false)

        // FIXED: Add extra padding for labels
        spendingChart.setExtraOffsets(10f, 20f, 10f, 80f) // left, top, right, bottom

        // Remove description label
        val description = Description()
        description.text = ""
        spendingChart.description = description

        // FIXED: Configure X-axis (category names) with better spacing
        val xAxis = spendingChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f  // Rotate for better readability
        xAxis.textSize = 11f             // Slightly larger text
        xAxis.textColor = Color.BLACK
        xAxis.setLabelCount(10, false)   // Limit number of labels if too many
        xAxis.isGranularityEnabled = true
        xAxis.setAvoidFirstLastClipping(true) // Prevent clipping of first/last labels
        xAxis.spaceMin = 0.5f            // Add space at beginning
        xAxis.spaceMax = 0.5f            // Add space at end

        // Configure Y-axis (amounts)
        val leftAxis = spendingChart.axisLeft
        leftAxis.setLabelCount(8, false)
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        leftAxis.spaceTop = 15f
        leftAxis.axisMinimum = 0f
        leftAxis.textColor = Color.BLACK
        leftAxis.textSize = 10f

        // Disable right axis
        val rightAxis = spendingChart.axisRight
        rightAxis.isEnabled = false

        // Configure legend
        val legend = spendingChart.legend
        legend.isEnabled = false // We have our own custom legend

        Log.d(TAG, "Chart setup completed")
    }

    /**
     * NEW: Toggle chart visibility and update button text
     */
    private fun toggleChartVisibility() {
        if (chartCard.visibility == View.VISIBLE) {
            chartCard.visibility = View.GONE
            btnToggleView.text = "Show Chart"
        } else {
            chartCard.visibility = View.VISIBLE
            btnToggleView.text = "Hide Chart"
            // Refresh chart when showing
            updateChart()
        }
    }

    /**
     * NEW: Update chart with current data
     */
    private fun updateChart() {
        Log.d(TAG, "Updating chart with ${currentCategories.size} categories")

        if (currentCategories.isEmpty()) {
            spendingChart.clear()
            spendingChart.invalidate()
            return
        }

        // Prepare data for chart
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        currentCategories.forEachIndexed { index, category ->
            entries.add(BarEntry(index.toFloat(), category.totalSpent.toFloat()))

            // FIXED: Shorten category names if they're too long
            val shortName = if (category.name.length > 8) {
                category.name.substring(0, 8) + "..."
            } else {
                category.name
            }
            labels.add(shortName)

            Log.d(TAG, "Chart entry: ${category.name} -> $shortName = R${category.totalSpent}")
        }

        // Create dataset
        val dataSet = BarDataSet(entries, "Spending")
        dataSet.color = Color.parseColor("#2196F3") // Blue color for spending bars
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 10f

        // Format values to show currency
        dataSet.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "R${String.format("%.0f", value)}"
            }
        })

        // Create bar data
        val barData = BarData(dataSet)
        barData.barWidth = 0.7f // Make bars slightly thinner for more spacing

        // Set data to chart
        spendingChart.data = barData

        // FIXED: Set custom labels for X-axis with shortened names
        spendingChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        spendingChart.xAxis.labelCount = labels.size

        // Add budget goal lines if we have budget data
        addBudgetGoalLines()

        // Refresh chart
        spendingChart.animateY(800) // Animate for 800ms
        spendingChart.invalidate()

        Log.d(TAG, "Chart update completed")
    }

    /**
     * NEW: Add budget goal lines to the chart
     */
    private fun addBudgetGoalLines() {
        Log.d(TAG, "Adding budget goal lines, budgets available: ${currentBudgets.size}")

        val leftAxis = spendingChart.axisLeft

        // Clear existing limit lines
        leftAxis.removeAllLimitLines()

        if (currentBudgets.isEmpty()) {
            Log.d(TAG, "No budgets to display")
            return
        }

        // Calculate average budget goals across all categories for display
        val avgMinGoal = currentBudgets.map { it.minimumAmount }.average()
        val avgMaxGoal = currentBudgets.map { it.maximumAmount }.average()

        // Add minimum goal line (green)
        val minLine = LimitLine(avgMinGoal.toFloat(), "Avg Min Goal")
        minLine.lineWidth = 2f
        minLine.lineColor = Color.parseColor("#4CAF50") // Green
        minLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
        minLine.textSize = 10f
        minLine.textColor = Color.parseColor("#4CAF50")

        // Add maximum goal line (red)
        val maxLine = LimitLine(avgMaxGoal.toFloat(), "Avg Max Goal")
        maxLine.lineWidth = 2f
        maxLine.lineColor = Color.parseColor("#F44336") // Red
        maxLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_BOTTOM
        maxLine.textSize = 10f
        maxLine.textColor = Color.parseColor("#F44336")

        leftAxis.addLimitLine(minLine)
        leftAxis.addLimitLine(maxLine)

        Log.d(TAG, "Added budget lines - Min: R$avgMinGoal, Max: R$avgMaxGoal")
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
     * UPDATED: Now also loads budget data for chart
     */
    private fun loadCategories() {
        Log.d(TAG, "Loading all categories (no date filter)")
        lifecycleScope.launch {
            try {
                // Combine expenses, categories, and budgets data flows from Firestore
                combine(
                    firestoreRepository.getAllExpenses(),
                    firestoreRepository.getAllCategories(),
                    firestoreRepository.getAllBudgets()
                ) { expenses, categories, budgets ->
                    Log.d(TAG, "Received ${expenses.size} expenses, ${categories.size} categories, ${budgets.size} budgets")

                    // Store budgets for chart
                    currentBudgets = budgets

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

                    // Store current data for chart
                    currentCategories = summaries

                    // Update the RecyclerView with the new data
                    adapter.submitList(summaries)

                    // Update chart if visible
                    if (chartCard.visibility == View.VISIBLE) {
                        updateChart()
                    }
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
     * UPDATED: Now also filters budget data for the same period
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
                    firestoreRepository.getAllCategories(),
                    firestoreRepository.getAllBudgets()
                ) { expenses, categories, budgets ->
                    Log.d(TAG, "Filtered ${expenses.size} expenses, ${categories.size} categories, ${budgets.size} budgets")

                    // Filter budgets that are active during the selected period
                    currentBudgets = budgets.filter { budget ->
                        val budgetStart = budget.getStartDate()
                        val budgetEnd = budget.getEndDate()
                        // Budget overlaps with selected period
                        (budgetStart <= end && budgetEnd >= start)
                    }

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

                    // Store current data for chart
                    currentCategories = summaries

                    adapter.submitList(summaries)

                    // Update chart if visible
                    if (chartCard.visibility == View.VISIBLE) {
                        updateChart()
                    }
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