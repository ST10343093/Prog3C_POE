package vcmsa.projects.prog3c

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vcmsa.projects.prog3c.data.AppDatabase
import vcmsa.projects.prog3c.data.Budget
import vcmsa.projects.prog3c.data.Category
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BudgetActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var spinnerCategory: Spinner
    private lateinit var etBudgetAmount: TextInputEditText
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var btnSaveBudget: Button
    private lateinit var rvBudgets: RecyclerView
    private lateinit var btnBackFromBudget: Button
    private lateinit var adapter: BudgetAdapter

    private var selectedCategoryId: Long = -1
    private var categories: List<Category> = emptyList()
    private var startDate: Date = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1) // First day of month
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.time
    private var endDate: Date = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) // Last day of month
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }.time

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budget)

        // Initialize database
        database = AppDatabase.getDatabase(this)

        // Initialize views
        spinnerCategory = findViewById(R.id.spinnerBudgetCategory)
        etBudgetAmount = findViewById(R.id.etBudgetAmount)
        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)
        btnSaveBudget = findViewById(R.id.btnSaveBudget)
        rvBudgets = findViewById(R.id.rvBudgets)
        btnBackFromBudget = findViewById(R.id.btnBackFromBudget)

        // Update date fields
        updateDateFields()

        // Set up date pickers
        etStartDate.setOnClickListener { showDatePicker(true) }
        etEndDate.setOnClickListener { showDatePicker(false) }

        // Set up RecyclerView
        adapter = BudgetAdapter(emptyList(), emptyMap(), ::deleteBudget)
        rvBudgets.adapter = adapter
        rvBudgets.layoutManager = LinearLayoutManager(this)

        // Set up save button
        btnSaveBudget.setOnClickListener {
            saveBudget()
        }

        // Set up back button
        btnBackFromBudget.setOnClickListener {
            finish()
        }

        // Load categories and budgets
        loadCategories()
    }

    private fun updateDateFields() {
        etStartDate.setText(dateFormatter.format(startDate))
        etEndDate.setText(dateFormatter.format(endDate))
    }

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
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    startDate = calendar.time
                    etStartDate.setText(dateFormatter.format(startDate))
                } else {
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

    private fun loadCategories() {
        lifecycleScope.launch {
            categories = database.categoryDao().getAllCategories().first()

            if (categories.isEmpty()) {
                Toast.makeText(
                    this@BudgetActivity,
                    "Please add categories first",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return@launch
            }

            // Create adapter for spinner
            val adapter = ArrayAdapter(
                this@BudgetActivity,
                android.R.layout.simple_spinner_item,
                categories.map { it.name }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter

            // Set default selection
            selectedCategoryId = categories.first().id

            // Set listener for selection changes
            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedCategoryId = categories[position].id
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Do nothing
                }
            }

            // Load budgets
            loadBudgets()
        }
    }

    private fun loadBudgets() {
        lifecycleScope.launch {
            database.budgetDao().getAllBudgets().collectLatest { budgets ->
                val categoryMap = categories.associateBy { it.id }
                adapter.updateBudgets(budgets, categoryMap)
            }
        }
    }

    private fun saveBudget() {
        val amountText = etBudgetAmount.text.toString().trim()

        // Validate inputs
        if (amountText.isEmpty()) {
            etBudgetAmount.error = "Amount is required"
            etBudgetAmount.requestFocus()
            return
        }

        val amount = try {
            amountText.toDouble()
        } catch (e: NumberFormatException) {
            etBudgetAmount.error = "Invalid amount format"
            etBudgetAmount.requestFocus()
            return
        }

        if (amount <= 0) {
            etBudgetAmount.error = "Amount must be greater than zero"
            etBudgetAmount.requestFocus()
            return
        }

        if (selectedCategoryId == -1L) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        if (startDate.after(endDate)) {
            Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
            return
        }

        // Create budget object
        val budget = Budget(
            amount = amount,
            categoryId = selectedCategoryId,
            startDate = startDate,
            endDate = endDate
        )

        // Save budget to database
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                database.budgetDao().insertBudget(budget)
            }
            Toast.makeText(this@BudgetActivity, "Budget saved", Toast.LENGTH_SHORT).show()
            etBudgetAmount.text?.clear()
            etBudgetAmount.clearFocus()
        }
    }

    private fun deleteBudget(budget: Budget) {
        AlertDialog.Builder(this)
            .setTitle("Delete Budget")
            .setMessage("Are you sure you want to delete this budget?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        database.budgetDao().deleteBudget(budget)
                    }
                    Toast.makeText(this@BudgetActivity, "Budget deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private inner class BudgetAdapter(
        private var budgets: List<Budget>,
        private var categoryMap: Map<Long, Category>,
        private val onDeleteClick: (Budget) -> Unit
    ) : RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

        fun updateBudgets(newBudgets: List<Budget>, newCategoryMap: Map<Long, Category>) {
            budgets = newBudgets
            categoryMap = newCategoryMap
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_budget, parent, false)
            return BudgetViewHolder(view)
        }

        override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
            val budget = budgets[position]
            holder.bind(budget)
        }

        override fun getItemCount(): Int = budgets.size

        inner class BudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvBudgetCategory: TextView = itemView.findViewById(R.id.tvBudgetCategory)
            private val tvBudgetAmount: TextView = itemView.findViewById(R.id.tvBudgetAmount)
            private val tvBudgetPeriod: TextView = itemView.findViewById(R.id.tvBudgetPeriod)
            private val tvBudgetSpent: TextView = itemView.findViewById(R.id.tvBudgetSpent)
            private val tvBudgetRemaining: TextView = itemView.findViewById(R.id.tvBudgetRemaining)
            private val btnDeleteBudget: ImageButton = itemView.findViewById(R.id.btnDeleteBudget)

            fun bind(budget: Budget) {
                val category = categoryMap[budget.categoryId]
                if (category != null) {
                    tvBudgetCategory.text = category.name
                    tvBudgetCategory.setBackgroundColor(category.color)
                } else {
                    tvBudgetCategory.text = "Unknown Category"
                    tvBudgetCategory.setBackgroundColor(android.graphics.Color.GRAY)
                }

                tvBudgetAmount.text = "Budget: $${String.format("%.2f", budget.amount)}"
                tvBudgetPeriod.text = "${dateFormatter.format(budget.startDate)} - ${dateFormatter.format(budget.endDate)}"

                // Calculate amount spent and remaining
                lifecycleScope.launch {
                    val spent = withContext(Dispatchers.IO) {
                        database.expenseDao().getTotalExpenseByCategory(
                            budget.categoryId,
                            budget.startDate,
                            budget.endDate
                        ) ?: 0.0
                    }
                    val remaining = budget.amount - spent
                    tvBudgetSpent.text = "Spent: $${String.format("%.2f", spent)}"
                    tvBudgetRemaining.text = "Remaining: $${String.format("%.2f", remaining)}"

                    // Change color based on budget status
                    if (remaining < 0) {
                        tvBudgetRemaining.setTextColor(android.graphics.Color.RED)
                    } else if (remaining < (budget.amount * 0.2)) {
                        tvBudgetRemaining.setTextColor(android.graphics.Color.parseColor("#FFA500")) // Orange
                    } else {
                        tvBudgetRemaining.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Green
                    }
                }

                btnDeleteBudget.setOnClickListener {
                    onDeleteClick(budget)
                }
            }
        }
    }
}