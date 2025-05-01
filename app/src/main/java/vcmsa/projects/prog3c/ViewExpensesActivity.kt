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
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import vcmsa.projects.prog3c.data.AppDatabase
import vcmsa.projects.prog3c.data.Category
import vcmsa.projects.prog3c.data.Expense
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ViewExpensesActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var btnApplyFilter: Button
    private lateinit var tvTotalExpenses: TextView
    private lateinit var rvExpenses: RecyclerView
    private lateinit var btnBack: Button
    private lateinit var adapter: ExpenseAdapter

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

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var categoryMap: Map<Long, Category> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_expenses)

        // Initialize database
        database = AppDatabase.getDatabase(this)

        // Initialize views
        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)
        btnApplyFilter = findViewById(R.id.btnApplyFilter)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        rvExpenses = findViewById(R.id.rvExpenses)
        btnBack = findViewById(R.id.btnBack)

        // Set up date fields
        updateDateFields()

        // Set up date pickers
        etStartDate.setOnClickListener { showDatePicker(true) }
        etEndDate.setOnClickListener { showDatePicker(false) }

        // Set up filter button
        btnApplyFilter.setOnClickListener {
            loadExpenses()
        }

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Set up RecyclerView
        adapter = ExpenseAdapter(emptyList(), emptyMap(), ::onExpenseClick)
        rvExpenses.adapter = adapter
        rvExpenses.layoutManager = LinearLayoutManager(this)

        // Load categories and expenses
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
            database.categoryDao().getAllCategories().collectLatest { categories ->
                categoryMap = categories.associateBy { it.id }
                loadExpenses()
            }
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            database.expenseDao().getExpensesByDateRange(startDate, endDate).collectLatest { expenses ->
                adapter.updateExpenses(expenses, categoryMap)

                val total = expenses.sumOf { it.amount }
                tvTotalExpenses.text = "Total: $${String.format("%.2f", total)}"
            }
        }
    }

    private fun onExpenseClick(expense: Expense) {
        // Open detail view for all expenses, not just those with photos
        val intent = Intent(this, ExpenseDetailActivity::class.java)
        intent.putExtra("EXPENSE_ID", expense.id)
        startActivity(intent)
    }

    private class ExpenseAdapter(
        private var expenses: List<Expense>,
        private var categoryMap: Map<Long, Category>,
        private val onExpenseClick: (Expense) -> Unit
    ) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

        fun updateExpenses(newExpenses: List<Expense>, newCategoryMap: Map<Long, Category>) {
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

        inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val amountTextView: TextView = itemView.findViewById(R.id.tvExpenseAmount)
            private val dateTextView: TextView = itemView.findViewById(R.id.tvExpenseDate)
            private val descriptionTextView: TextView = itemView.findViewById(R.id.tvExpenseDescription)
            private val categoryTextView: TextView = itemView.findViewById(R.id.tvExpenseCategory)
            private val photoIndicator: ImageView = itemView.findViewById(R.id.ivHasPhoto)

            fun bind(expense: Expense, category: Category?) {
                amountTextView.text = "$${String.format("%.2f", expense.amount)}"
                dateTextView.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expense.date)
                descriptionTextView.text = expense.description

                if (category != null) {
                    categoryTextView.text = category.name
                    categoryTextView.setBackgroundColor(category.color)
                } else {
                    categoryTextView.text = "Unknown"
                    categoryTextView.setBackgroundColor(android.graphics.Color.GRAY)
                }

                // Show photo indicator if there's a photo
                if (!expense.photoPath.isNullOrEmpty()) {
                    photoIndicator.visibility = View.VISIBLE
                    Log.d("ViewExpensesActivity", "Photo available for expense: ${expense.id}, path: ${expense.photoPath}")
                } else {
                    photoIndicator.visibility = View.GONE
                    Log.d("ViewExpensesActivity", "No photo for expense: ${expense.id}")
                }

                itemView.setOnClickListener {
                    onExpenseClick(expense)
                }
            }
        }
    }
}