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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import vcmsa.projects.prog3c.data.FirestoreRepository
import vcmsa.projects.prog3c.data.FirestoreExpense
import vcmsa.projects.prog3c.data.FirestoreCategory

// Pie Chart imports
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate

import java.text.SimpleDateFormat
import java.util.*

/**
 * Visual Spending Breakdown Activity - Shows pie chart and detailed breakdown of spending by category
 * FEATURES:
 * - Smart legend handling that adapts to category count and name lengths
 * - Intelligent category name truncation for pie chart labels
 * - Responsive grid layout for many categories
 * - Custom legend implementation below the chart
 */
class SpendingBreakdownActivity : AppCompatActivity() {

    private val TAG = "SpendingBreakdown"

    // Firestore repository for data access
    private lateinit var firestoreRepository: FirestoreRepository

    // Date range filter variables
    private var startDate: Date? = null
    private var endDate: Date? = null

    // UI components
    private lateinit var btnBack: MaterialButton
    private lateinit var btnClearFilter: MaterialButton
    private lateinit var btnApplyFilter: MaterialButton
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText

    // Pie chart and breakdown components
    private lateinit var pieChart: PieChart
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategoryBreakdownAdapter
    private lateinit var cardChart: MaterialCardView
    private lateinit var cardBreakdown: MaterialCardView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvDateRange: TextView

    // NEW: Custom legend RecyclerView
    private lateinit var rvCustomLegend: RecyclerView
    private lateinit var legendAdapter: CustomLegendAdapter

    // Data storage
    private var currentCategories: List<CategoryBreakdownItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spending_breakdown)

        Log.d(TAG, "onCreate started")

        // Initialize Firestore repository
        firestoreRepository = FirestoreRepository.getInstance()

        // Initialize views
        initializeViews()

        // Set up event listeners
        setupEventListeners()

        // Configure pie chart
        setupPieChart()

        // Set default dates (current month)
        setDefaultDates()
        updateDateText()

        // Load initial data
        loadSpendingData()

        Log.d(TAG, "onCreate completed")
    }

    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBackFromBreakdown)
        btnClearFilter = findViewById(R.id.btnClearFilter)
        btnApplyFilter = findViewById(R.id.btnApplyFilter)
        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)

        pieChart = findViewById(R.id.pieChart)
        recyclerView = findViewById(R.id.rvCategoryBreakdown)
        cardChart = findViewById(R.id.cardChart)
        cardBreakdown = findViewById(R.id.cardBreakdown)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvDateRange = findViewById(R.id.tvDateRange)

        // NEW: Initialize custom legend
        rvCustomLegend = findViewById(R.id.rvCustomLegend)

        // Set up RecyclerView for breakdown
        adapter = CategoryBreakdownAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Set up custom legend adapter
        legendAdapter = CustomLegendAdapter()
        rvCustomLegend.adapter = legendAdapter
    }

    /**
     * Set up event listeners for buttons and date pickers
     */
    private fun setupEventListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnClearFilter.setOnClickListener {
            clearFilters()
        }

        btnApplyFilter.setOnClickListener {
            applyDateFilter()
        }

        // Set up date pickers
        etStartDate.setOnClickListener {
            showDatePicker(true)
        }

        etEndDate.setOnClickListener {
            showDatePicker(false)
        }
    }

    /**
     * Configure the pie chart appearance and behavior - OPTIMIZED FOR SPACE
     */
    private fun setupPieChart() {
        Log.d(TAG, "Setting up pie chart optimized for space and readability")

        // General chart settings
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.holeRadius = 30f // Smaller hole for more chart space
        pieChart.transparentCircleRadius = 35f
        pieChart.isRotationEnabled = true
        pieChart.isHighlightPerTapEnabled = true
        pieChart.animateY(1000)

        // OPTIMIZED PADDING: Minimal padding since we have custom legend below
        pieChart.setExtraOffsets(15f, 15f, 15f, 20f) // Much tighter margins

        // Remove description
        val description = Description()
        description.text = ""
        pieChart.description = description

        // DISABLE BUILT-IN LEGEND: We'll use our custom one
        pieChart.legend.isEnabled = false

        Log.d(TAG, "Pie chart setup completed - using custom legend system")
    }

    /**
     * Set default date range to current month
     */
    private fun setDefaultDates() {
        val calendar = Calendar.getInstance()
        endDate = calendar.time // Today

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        startDate = calendar.time // First day of current month
    }

    /**
     * Clear date filters and show all-time data
     */
    private fun clearFilters() {
        startDate = null
        endDate = null
        etStartDate.setText("")
        etEndDate.setText("")
        tvDateRange.text = "All Time"
        loadSpendingData()
    }

    /**
     * Apply the selected date filter
     */
    private fun applyDateFilter() {
        if (startDate != null && endDate != null) {
            updateDateText()
            loadSpendingData()
        } else {
            Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Show date picker dialog
     */
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        calendar.time = if (isStartDate) startDate ?: Date() else endDate ?: Date()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                if (isStartDate) {
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    startDate = calendar.time
                } else {
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    endDate = calendar.time
                }
                updateDateText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    /**
     * Update date text displays
     */
    private fun updateDateText() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        if (startDate != null && endDate != null) {
            etStartDate.setText(dateFormat.format(startDate!!))
            etEndDate.setText(dateFormat.format(endDate!!))
            tvDateRange.text = "${dateFormat.format(startDate!!)} - ${dateFormat.format(endDate!!)}"
        } else {
            tvDateRange.text = "All Time"
        }
    }

    /**
     * Load spending data and update charts
     */
    private fun loadSpendingData() {
        Log.d(TAG, "Loading spending data")
        lifecycleScope.launch {
            try {
                // Get expenses and categories
                val expensesFlow = if (startDate != null && endDate != null) {
                    firestoreRepository.getExpensesByDateRange(startDate!!, endDate!!)
                } else {
                    firestoreRepository.getAllExpenses()
                }

                combine(
                    expensesFlow,
                    firestoreRepository.getAllCategories()
                ) { expenses, categories ->
                    Log.d(TAG, "Received ${expenses.size} expenses and ${categories.size} categories")

                    // Create category map
                    val categoryMap = categories.associateBy { it.id }

                    // Calculate spending by category
                    val categoryTotals = expenses.groupBy { it.categoryId }.mapNotNull { (categoryId, expenseGroup) ->
                        val category = categoryMap[categoryId]
                        if (category != null) {
                            val total = expenseGroup.sumOf { it.amount }
                            CategoryBreakdownItem(
                                categoryName = category.name,
                                amount = total,
                                color = category.color,
                                percentage = 0.0 // Will be calculated later
                            )
                        } else {
                            null
                        }
                    }.sortedByDescending { it.amount }

                    // Calculate percentages
                    val totalAmount = categoryTotals.sumOf { it.amount }
                    val categoriesWithPercentages = categoryTotals.map { item ->
                        item.copy(
                            percentage = if (totalAmount > 0) (item.amount / totalAmount) * 100 else 0.0
                        )
                    }

                    Pair(categoriesWithPercentages, totalAmount)
                }.collect { (categories, totalAmount) ->
                    Log.d(TAG, "Updating UI with ${categories.size} categories, total: R$totalAmount")

                    currentCategories = categories

                    // Update UI
                    updatePieChart(categories)
                    updateCustomLegend(categories)
                    updateBreakdownList(categories)
                    updateTotalAmount(totalAmount)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading spending data", e)
                Toast.makeText(this@SpendingBreakdownActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * SMART CATEGORY NAME TRUNCATION - Handles long names intelligently
     */
    private fun smartTruncateCategory(name: String, maxLength: Int = 10): String {
        if (name.length <= maxLength) return name

        // Try to find a good breaking point (space, etc.)
        val words = name.split(" ", "-", "_")
        if (words.size > 1) {
            // Take first word if it's reasonable length
            val firstWord = words[0]
            if (firstWord.length <= maxLength && firstWord.length >= 3) {
                return firstWord
            }
        }

        // Fallback to truncation with ellipsis
        return "${name.take(maxLength - 1)}."
    }

    /**
     * Update the pie chart with smart labeling
     */
    private fun updatePieChart(categories: List<CategoryBreakdownItem>) {
        Log.d(TAG, "Updating pie chart with ${categories.size} categories")

        if (categories.isEmpty()) {
            pieChart.clear()
            pieChart.invalidate()
            return
        }

        // SMART ENTRY CREATION based on category count
        val entries = when {
            categories.size <= 3 -> {
                // 3 or fewer: use full names
                categories.map { category ->
                    PieEntry(category.amount.toFloat(), category.categoryName)
                }
            }
            categories.size <= 6 -> {
                // 4-6 categories: use smart truncation
                categories.map { category ->
                    val shortName = smartTruncateCategory(category.categoryName, 8)
                    PieEntry(category.amount.toFloat(), shortName)
                }
            }
            else -> {
                // 7+ categories: no labels on chart (use legend only)
                categories.map { category ->
                    PieEntry(category.amount.toFloat(), "")
                }
            }
        }

        // Create dataset
        val dataSet = PieDataSet(entries, "")

        // Enhanced color handling
        val colors = mutableListOf<Int>()
        categories.forEachIndexed { index, category ->
            if (category.color != 0) {
                colors.add(category.color)
            } else {
                val defaultColors = listOf(
                    Color.parseColor("#2196F3"), // Blue
                    Color.parseColor("#FF6B35"), // Orange
                    Color.parseColor("#4CAF50"), // Green
                    Color.parseColor("#9C27B0"), // Purple
                    Color.parseColor("#FF9800"), // Deep Orange
                    Color.parseColor("#00BCD4"), // Cyan
                    Color.parseColor("#8BC34A"), // Light Green
                    Color.parseColor("#E91E63"), // Pink
                    Color.parseColor("#607D8B"), // Blue Grey
                    Color.parseColor("#795548")  // Brown
                )
                colors.add(defaultColors[index % defaultColors.size])
            }
        }

        dataSet.colors = colors
        dataSet.valueTextSize = if (categories.size <= 5) 11f else 0f // Hide values for crowded charts
        dataSet.valueTextColor = Color.WHITE
        dataSet.sliceSpace = 1f
        dataSet.selectionShift = 6f

        // Smart value display
        if (categories.size <= 5) {
            dataSet.setDrawValues(true)
        } else {
            dataSet.setDrawValues(false)
        }

        // Create pie data
        val pieData = PieData(dataSet)

        if (categories.size <= 5) {
            pieData.setValueFormatter(PercentFormatter())
        }

        // Set data to chart
        pieChart.data = pieData
        pieChart.invalidate()

        Log.d(TAG, "Pie chart update completed with smart labeling")
    }

    /**
     * NEW: Update custom legend with smart grid layout
     */
    private fun updateCustomLegend(categories: List<CategoryBreakdownItem>) {
        Log.d(TAG, "Updating custom legend with ${categories.size} categories")

        if (categories.isEmpty()) {
            rvCustomLegend.visibility = View.GONE
            return
        }

        rvCustomLegend.visibility = View.VISIBLE

        // SMART GRID LAYOUT based on category count
        val spanCount = when {
            categories.size <= 2 -> 1  // Single column for 1-2 items
            categories.size <= 4 -> 2  // Two columns for 3-4 items
            categories.size <= 8 -> 2  // Two columns for 5-8 items
            else -> 3                  // Three columns for 9+ items
        }

        rvCustomLegend.layoutManager = GridLayoutManager(this, spanCount)
        legendAdapter.updateCategories(categories)

        Log.d(TAG, "Custom legend updated with $spanCount columns")
    }

    /**
     * Update the breakdown list
     */
    private fun updateBreakdownList(categories: List<CategoryBreakdownItem>) {
        adapter.updateCategories(categories)
    }

    /**
     * Update the total amount display
     */
    private fun updateTotalAmount(totalAmount: Double) {
        tvTotalAmount.text = "R${String.format("%.2f", totalAmount)}"
    }

    /**
     * Data class for category breakdown items
     */
    data class CategoryBreakdownItem(
        val categoryName: String,
        val amount: Double,
        val color: Int,
        val percentage: Double
    )

    /**
     * NEW: Custom Legend Adapter for smart grid layout
     */
    class CustomLegendAdapter : RecyclerView.Adapter<CustomLegendAdapter.LegendViewHolder>() {

        private var categories = listOf<CategoryBreakdownItem>()

        fun updateCategories(newCategories: List<CategoryBreakdownItem>) {
            categories = newCategories
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LegendViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_legend, parent, false)
            return LegendViewHolder(view)
        }

        override fun onBindViewHolder(holder: LegendViewHolder, position: Int) {
            holder.bind(categories[position])
        }

        override fun getItemCount(): Int = categories.size

        class LegendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val colorIndicator: View = itemView.findViewById(R.id.viewLegendColor)
            private val categoryText: TextView = itemView.findViewById(R.id.tvLegendText)

            fun bind(item: CategoryBreakdownItem) {
                colorIndicator.setBackgroundColor(item.color)

                // Smart text truncation for legend
                val displayText = if (item.categoryName.length > 12) {
                    "${item.categoryName.take(10)}..."
                } else {
                    item.categoryName
                }

                categoryText.text = displayText
            }
        }
    }

    /**
     * RecyclerView adapter for category breakdown list
     */
    class CategoryBreakdownAdapter : RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder>() {

        private var categories = listOf<CategoryBreakdownItem>()

        fun updateCategories(newCategories: List<CategoryBreakdownItem>) {
            categories = newCategories
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category_breakdown, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(categories[position])
        }

        override fun getItemCount(): Int = categories.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val colorIndicator: View = itemView.findViewById(R.id.viewCategoryColor)
            private val categoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
            private val categoryAmount: TextView = itemView.findViewById(R.id.tvCategoryAmount)
            private val categoryPercentage: TextView = itemView.findViewById(R.id.tvCategoryPercentage)

            fun bind(item: CategoryBreakdownItem) {
                colorIndicator.setBackgroundColor(item.color)
                categoryName.text = item.categoryName
                categoryAmount.text = "R${String.format("%.2f", item.amount)}"
                categoryPercentage.text = "${String.format("%.1f", item.percentage)}%"
            }
        }
    }
}