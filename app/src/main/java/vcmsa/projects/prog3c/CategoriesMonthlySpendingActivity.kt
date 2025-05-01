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

class CategoriesMonthlySpendingActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var startDate: Date? = Date()
    private var endDate: Date? = Date()
    private lateinit var btnBackFromCategories: Button
    private lateinit var btnClearFilter: Button
    private lateinit var btnApplyFilter: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategorySummaryAdapter
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private var expenseList: List<Expense> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_categories_monthly)

        database = AppDatabase.getDatabase(this)

        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)

        btnBackFromCategories = findViewById(R.id.btnBack)
        btnClearFilter = findViewById(R.id.buttonClear)
        btnApplyFilter = findViewById(R.id.buttonApplyFilter)

        recyclerView = findViewById(R.id.rvExpenses)
        adapter = CategorySummaryAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupDatePicker()
        updateDateText()

        loadCategories()

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

    private fun setupDatePicker() {
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

    private fun updateDateText() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        startDate?.let { etStartDate.setText(dateFormat.format(it)) }
        endDate?.let { etEndDate.setText(dateFormat.format(it)) }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            combine(
                database.expenseDao().getAllExpenses(),
                database.categoryDao().getAllCategories()
            ) { expenses, categories ->
                val categoryMap = categories.associateBy({ it.id }, { it })
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
                adapter.submitList(summaries)
            }
        }
    }

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
                val filtered = expenses.filter { it.date in start..end }
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

    class CategorySummaryAdapter : RecyclerView.Adapter<CategorySummaryAdapter.SummaryViewHolder>() {

        private var items = listOf<CategorySummary>()

        data class CategorySummary(val name: String, val totalSpent: Double, val color: Int)

        inner class SummaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val categoryName: TextView = view.findViewById(R.id.textCategoryName)
            val totalAmount: TextView = view.findViewById(R.id.textAmount)
            val colorView: View = view.findViewById(R.id.viewCategoryColor)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_categroty_expense, parent, false)
            return SummaryViewHolder(view)
        }

        override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
            val item = items[position]
            holder.categoryName.text = item.name
            holder.totalAmount.text = "R%.2f".format(item.totalSpent)
            holder.colorView.setBackgroundColor(item.color)
        }

        override fun getItemCount(): Int = items.size

        fun submitList(newList: List<CategorySummary>) {
            items = newList
            notifyDataSetChanged()
        }
    }
}
