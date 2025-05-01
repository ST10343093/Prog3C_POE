package vcmsa.projects.prog3c

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vcmsa.projects.prog3c.data.AppDatabase
import vcmsa.projects.prog3c.data.Budget
import vcmsa.projects.prog3c.data.Category
import vcmsa.projects.prog3c.data.Expense
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BudgetActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var etMinimumGoal: TextInputEditText
    private lateinit var etMaximumGoal: TextInputEditText
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnSaveBudget: Button
    private lateinit var btnBackFromBudget: Button

    private var startDate: Date = Date()
    private var endDate: Date = Date()
    private var selectedCategoryId: Long = -1
    private var categories: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_budget)

        // Initialize database
        database = AppDatabase.getDatabase(this)

        // Initialize views
        etMinimumGoal = findViewById(R.id.etMinimumGoal)
        etMaximumGoal = findViewById(R.id.etMaximumGoal)
        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnSaveBudget = findViewById(R.id.btnSaveBudget)
        btnBackFromBudget = findViewById(R.id.btnBackFromBudget)

        // Set up date picker
        setupDatePicker()

        // Set default date to today
        updateDateText()

        // Load categories for spinner
        loadCategories()

        // Set up back button
        btnBackFromBudget.setOnClickListener {
            finish() // This will close the current activity and return to the previous one
        }

        // Set up save button
        btnSaveBudget.setOnClickListener {
            saveBudget()
        }
    }

    private fun setupDatePicker() {
        etStartDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = startDate

            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
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
            calendar.time = endDate

            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
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
        etStartDate.setText(dateFormat.format(startDate))
        etEndDate.setText(dateFormat.format(endDate))
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
        }
    }

    private fun saveBudget() {
        val minAmountText = etMinimumGoal.text.toString().trim()
        val maxAmountText = etMaximumGoal.text.toString().trim()

        // Validate inputs
        if (minAmountText.isEmpty()) {
            etMinimumGoal.error = "Amount is required"
            etMinimumGoal.requestFocus()
            return
        }

        if (maxAmountText.isEmpty()) {
            etMaximumGoal.error = "Amount is required"
            etMaximumGoal.requestFocus()
            return
        }

        val minAmount = try {
            minAmountText.toDouble()
        } catch (e: NumberFormatException) {
            etMinimumGoal.error = "Invalid amount format"
            etMinimumGoal.requestFocus()
            return
        }

        val maxAmount = try {
            maxAmountText.toDouble()
        } catch (e: NumberFormatException) {
            etMaximumGoal.error = "Invalid amount format"
            etMaximumGoal.requestFocus()
            return
        }

        if (minAmount<= 0) {
            etMinimumGoal.error = "Amount must be greater than zero"
            etMinimumGoal.requestFocus()
            return
        }

        if (maxAmount<= 0) {
            etMinimumGoal.error = "Amount must be greater than zero"
            etMinimumGoal.requestFocus()
            return
        }

        if (selectedCategoryId == -1L) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        // Create budget object
        val budget =
            Budget(
         minimumAmount = minAmount,
        maximumAmount = maxAmount,
        categoryId = selectedCategoryId,
        startDate = startDate,
        endDate = endDate
            )


        // Save expense to database
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                    database.budgetDao().insertBudget(budget)
            }

            val message = "Budget saved"
            Toast.makeText(this@BudgetActivity, message, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}