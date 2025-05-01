package vcmsa.projects.prog3c

import android.app.DatePickerDialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import vcmsa.projects.prog3c.data.AppDatabase
import vcmsa.projects.prog3c.data.Expense
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExpensesActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var startDate: Date? = Date()
    private var endDate: Date? = Date()
    private lateinit var btnBackFromExpenses: Button
    private lateinit var btnClearFilter: Button
    private lateinit var btnApplyFilter: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ExpenseAdapter
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private var expenseList: List<Expense> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses)

        database = AppDatabase.getDatabase(this)

        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)

        btnBackFromExpenses = findViewById(R.id.btnBack)
        btnClearFilter = findViewById(R.id.buttonClear)
        btnApplyFilter = findViewById(R.id.buttonApplyFilter)

        recyclerView = findViewById(R.id.rvExpenses)
        adapter = ExpenseAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupDatePicker()
        updateDateText()

        loadExpenses()

        btnClearFilter.setOnClickListener {
            loadExpenses()
        }
        btnApplyFilter.setOnClickListener {
            filterExpenses()
        }
        btnBackFromExpenses.setOnClickListener {
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

    private fun filterExpenses() {
        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
            return
        }

        val filteredList = expenseList.filter { expense ->
            expense.date in startDate!!..endDate!!
        }

        adapter.submitList(filteredList)
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            combine(
                database.expenseDao().getAllExpenses(),
                database.categoryDao().getAllCategories()
            ) { expenses, categories ->
                val categoryMap = categories.associateBy({ it.id }, { it.name })
                Triple(expenses, categoryMap, categories)
            }.collect { (expenses, categoryMap, _) ->
                adapter.categoryMap = categoryMap
                expenseList = expenses
                adapter.submitList(expenses)
            }
        }
    }

    class ExpenseAdapter : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

        private var expenses = listOf<Expense>()

        inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imagePhoto: ImageView = itemView.findViewById(R.id.imagePhoto)
            val textDescription: TextView = itemView.findViewById(R.id.textDescription)
            val textAmount: TextView = itemView.findViewById(R.id.textAmount)
            val textDate: TextView = itemView.findViewById(R.id.textDate)
            val textCategory: TextView = itemView.findViewById(R.id.textCategory)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_expense, parent, false)
            return ExpenseViewHolder(view)
        }

        override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
            val expense = expenses[position]
            holder.textDescription.text = expense.description
            holder.textAmount.text = "R${"%.2f".format(expense.amount)}"
            holder.textDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(expense.date)

            val categoryName = categoryMap[expense.categoryId] ?: "Unknown"
            holder.textCategory.text = categoryName

            val photoPath = expense.photoPath
            if (!photoPath.isNullOrEmpty()) {
                val file = File(photoPath)
                if (file.exists()) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        holder.imagePhoto.setImageBitmap(bitmap)
                        holder.imagePhoto.visibility = View.VISIBLE
                    } else {
                        holder.imagePhoto.visibility = View.GONE
                    }
                } else {
                    holder.imagePhoto.visibility = View.GONE
                }
            } else {
                holder.imagePhoto.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = expenses.size

        fun submitList(newList: List<Expense>) {
            expenses = newList
            notifyDataSetChanged()
        }

        var categoryMap: Map<Long, String> = emptyMap()
            set(value) {
                field = value
                notifyDataSetChanged()
            }
    }
}
