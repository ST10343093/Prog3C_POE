/*
 --------------------------------Project Details----------------------------------
 STUDENT NUMBERS: ST10343094        | ST10304100             | ST10248581    | ST10211919      | ST10295602
 STUDENT NAMES: Arshad Shoaib Bhula | Jordan Wayne Gardiner  | 3.Troy Krause | Azania Mdletshe | Phineas Junior Kalambay
 COURSE: BCAD Year 3
 MODULE: Programming 3C
 MODULE CODE: PROG7313
 ASSESSMENT: Portfolio of Evidence (POE) Part 2
 Github REPO LINK: https://github.com/ST10343093/Prog3C_POE
 --------------------------------Project Details----------------------------------
*/
/*
 --------------------------------Code Attribution----------------------------------
 Title: Basic syntax | Kotlin Documentation
 Author: Kotlin
 Date Published: 06 November 2024
 Date Accessed: 25 April 2025
 Code Version: v21.20
 Availability: https://kotlinlang.org/docs/basic-syntax.html
  --------------------------------Code Attribution----------------------------------
*/

package vcmsa.projects.prog3c

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import vcmsa.projects.prog3c.data.FirestoreRepository
import vcmsa.projects.prog3c.data.FirestoreCategory
import vcmsa.projects.prog3c.data.FirestoreBudget
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity for creating budget goals for expense categories
 * Allows users to set minimum and maximum spending targets for specific categories
 * within a defined time period
 */
class BudgetActivity : AppCompatActivity() {

    // Firestore repository reference
    private lateinit var firestoreRepository: FirestoreRepository

    // UI elements
    private lateinit var etMinimumGoal: TextInputEditText
    private lateinit var etMaximumGoal: TextInputEditText
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnSaveBudget: Button
    private lateinit var btnBackFromBudget: Button

    // Data variables
    private var startDate: Date = Date()                // Budget period start date
    private var endDate: Date = Date()                  // Budget period end date
    private var selectedCategoryId: String = ""         // Currently selected category ID
    private var categories: List<FirestoreCategory> = emptyList() // List of available categories

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_budget)

        // Initialize Firestore repository
        firestoreRepository = FirestoreRepository.getInstance()

        // Initialize views
        etMinimumGoal = findViewById(R.id.etMinimumGoal)
        etMaximumGoal = findViewById(R.id.etMaximumGoal)
        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnSaveBudget = findViewById(R.id.btnSaveBudget)
        btnBackFromBudget = findViewById(R.id.btnBackFromBudget)

        // FIXED: Set better default dates
        val calendar = Calendar.getInstance()

        // Start date: Today at beginning of day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        startDate = calendar.time

        // End date: End of current month
        calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1) // Last day of current month
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        endDate = calendar.time

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

    /**
     * Sets up the date picker dialogs for selecting budget period dates
     * Makes both date fields clickable to open a date picker
     */
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
                    // FIXED: Set start date to beginning of day
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    startDate = calendar.time

                    // FIXED: Auto-set end date to end of selected month if not set properly
                    if (endDate.before(startDate) || endDate == startDate) {
                        val endCalendar = Calendar.getInstance()
                        endCalendar.time = startDate
                        endCalendar.add(Calendar.MONTH, 1) // Default to 1 month later
                        endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                        endCalendar.set(Calendar.MINUTE, 59)
                        endCalendar.set(Calendar.SECOND, 59)
                        endCalendar.set(Calendar.MILLISECOND, 999)
                        endDate = endCalendar.time
                    }

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
                    // FIXED: Set end date to end of day
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                    calendar.set(Calendar.SECOND, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    endDate = calendar.time
                    updateDateText()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            // FIXED: Set minimum date to start date + 1 day
            datePickerDialog.datePicker.minDate = startDate.time + (24 * 60 * 60 * 1000) // Next day
            datePickerDialog.show()
        }
    }

    /**
     * Updates the text in both date fields to display the selected dates
     */
    private fun updateDateText() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        etStartDate.setText(dateFormat.format(startDate))
        etEndDate.setText(dateFormat.format(endDate))
    }

    /**
     * Loads categories from Firestore and sets up the category spinner
     * Exits the activity if no categories are available
     */
    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                categories = firestoreRepository.getAllCategories().first()

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
            } catch (e: Exception) {
                Toast.makeText(this@BudgetActivity, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * Validates input fields and saves the budget to Firestore
     * Creates a new budget with target spending limits for the selected category
     */
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

        if (minAmount <= 0) {
            etMinimumGoal.error = "Amount must be greater than zero"
            etMinimumGoal.requestFocus()
            return
        }

        if (maxAmount <= 0) {
            etMaximumGoal.error = "Amount must be greater than zero"
            etMaximumGoal.requestFocus()
            return
        }

        if (maxAmount <= minAmount) {
            etMaximumGoal.error = "Maximum amount must be greater than minimum amount"
            etMaximumGoal.requestFocus()
            return
        }

        if (selectedCategoryId.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        // FIXED: Better date validation
        if (endDate.before(startDate)) {
            Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show()
            return
        }

        // FIXED: Check if dates are too close (same day)
        val daysDifference = (endDate.time - startDate.time) / (24 * 60 * 60 * 1000)
        if (daysDifference < 1) {
            Toast.makeText(this, "Budget period must be at least 1 day long", Toast.LENGTH_SHORT).show()
            return
        }

        // Create budget object
        val budget = FirestoreBudget(
            id = "",
            minimumAmount = minAmount,
            maximumAmount = maxAmount,
            categoryId = selectedCategoryId,
            startDate = startDate,
            endDate = endDate,
            userId = "" // Will be set by repository
        )

        // Save budget to Firestore
        lifecycleScope.launch {
            try {
                firestoreRepository.insertBudget(budget)
                Toast.makeText(this@BudgetActivity, "Budget saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@BudgetActivity, "Error saving budget: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}