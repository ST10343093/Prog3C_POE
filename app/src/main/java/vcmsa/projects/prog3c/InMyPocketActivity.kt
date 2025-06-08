package vcmsa.projects.prog3c

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import vcmsa.projects.prog3c.data.FirestoreRepository
import java.util.*

/**
 * InMyPocket Activity - Calculates available spending money
 * This feature helps users understand how much discretionary spending they have
 * after accounting for budgets, savings goals, and essential expenses
 */
class InMyPocketActivity : AppCompatActivity() {

    private val TAG = "InMyPocketActivity"

    // Firestore repository for data access
    private lateinit var firestoreRepository: FirestoreRepository

    // UI components
    private lateinit var btnBack: MaterialButton
    private lateinit var etMonthlyIncome: TextInputEditText
    private lateinit var etSavingsGoal: TextInputEditText
    private lateinit var btnCalculate: MaterialButton

    // Result display components
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvBudgetAllocated: TextView
    private lateinit var tvSavingsGoal: TextView
    private lateinit var tvInMyPocket: TextView
    private lateinit var tvPocketStatus: TextView
    private lateinit var cardResults: MaterialCardView

    // Data variables
    private var monthlyIncome: Double = 0.0
    private var savingsGoalAmount: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_in_my_pocket)

        // Initialize Firestore repository
        firestoreRepository = FirestoreRepository.getInstance()

        // Initialize views
        initializeViews()

        // Set up event listeners
        setupEventListeners()

        // Hide results initially
        cardResults.visibility = android.view.View.GONE
    }

    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBackFromPocket)
        etMonthlyIncome = findViewById(R.id.etMonthlyIncome)
        etSavingsGoal = findViewById(R.id.etSavingsGoal)
        btnCalculate = findViewById(R.id.btnCalculate)

        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvBudgetAllocated = findViewById(R.id.tvBudgetAllocated)
        tvSavingsGoal = findViewById(R.id.tvSavingsGoal)
        tvInMyPocket = findViewById(R.id.tvInMyPocket)
        tvPocketStatus = findViewById(R.id.tvPocketStatus)
        cardResults = findViewById(R.id.cardResults)
    }

    /**
     * Set up event listeners for buttons
     */
    private fun setupEventListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnCalculate.setOnClickListener {
            calculateInMyPocket()
        }
    }

    /**
     * Main calculation function for "In My Pocket" feature
     */
    private fun calculateInMyPocket() {
        // Validate input
        val incomeText = etMonthlyIncome.text.toString().trim()
        val savingsText = etSavingsGoal.text.toString().trim()

        if (incomeText.isEmpty()) {
            etMonthlyIncome.error = "Monthly income is required"
            etMonthlyIncome.requestFocus()
            return
        }

        try {
            monthlyIncome = incomeText.toDouble()
            savingsGoalAmount = if (savingsText.isNotEmpty()) savingsText.toDouble() else 0.0

            if (monthlyIncome <= 0) {
                etMonthlyIncome.error = "Income must be greater than zero"
                etMonthlyIncome.requestFocus()
                return
            }

        } catch (e: NumberFormatException) {
            etMonthlyIncome.error = "Invalid number format"
            etMonthlyIncome.requestFocus()
            return
        }

        // Perform calculations
        lifecycleScope.launch {
            try {
                // Get current month date range
                val calendar = Calendar.getInstance()
                val monthEndDate = calendar.time

                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val monthStartDate = calendar.time

                Log.d(TAG, "Calculating for period: $monthStartDate to $monthEndDate")

                // 1. Get total expenses for current month
                val expenses = firestoreRepository.getExpensesByDateRange(monthStartDate, monthEndDate).first()
                val totalExpenses = expenses.sumOf { it.amount }

                // 2. Get total budget allocations for active budgets
                val budgets = firestoreRepository.getAllBudgets().first()
                val activeBudgets = budgets.filter { budget ->
                    val now = Date()
                    val budgetStart = budget.getStartDate()
                    val budgetEnd = budget.getEndDate()
                    now >= budgetStart && now <= budgetEnd
                }
                val totalBudgetAllocated = activeBudgets.sumOf { it.maximumAmount }

                // 3. Calculate "In My Pocket" amount
                val afterExpenses = monthlyIncome - totalExpenses
                val afterSavings = afterExpenses - savingsGoalAmount
                val afterBudgets = afterSavings - totalBudgetAllocated
                val inMyPocketAmount = maxOf(0.0, afterBudgets)

                // 4. Display results
                displayResults(monthlyIncome, totalExpenses, totalBudgetAllocated, savingsGoalAmount, inMyPocketAmount)

                Log.d(TAG, "Calculation complete - In My Pocket: R$inMyPocketAmount")

            } catch (e: Exception) {
                Log.e(TAG, "Error calculating In My Pocket", e)
                Toast.makeText(this@InMyPocketActivity, "Error calculating: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Display calculation results in the UI
     */
    private fun displayResults(
        income: Double,
        expenses: Double,
        budgetAllocated: Double,
        savings: Double,
        inMyPocket: Double
    ) {
        // Show results card
        cardResults.visibility = android.view.View.VISIBLE

        // Display all values
        tvTotalIncome.text = "R${String.format("%.2f", income)}"
        tvTotalExpenses.text = "R${String.format("%.2f", expenses)}"
        tvBudgetAllocated.text = "R${String.format("%.2f", budgetAllocated)}"
        tvSavingsGoal.text = "R${String.format("%.2f", savings)}"
        tvInMyPocket.text = "R${String.format("%.2f", inMyPocket)}"

        // Set status message and color based on amount
        when {
            inMyPocket > 1000 -> {
                tvPocketStatus.text = "Great! You have plenty of discretionary spending money."
                tvPocketStatus.setTextColor(resources.getColor(R.color.budget_green, null))
                tvInMyPocket.setTextColor(resources.getColor(R.color.budget_green, null))
            }
            inMyPocket > 500 -> {
                tvPocketStatus.text = "Good! You have moderate discretionary spending available."
                tvPocketStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
                tvInMyPocket.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            }
            inMyPocket > 0 -> {
                tvPocketStatus.text = "Limited discretionary spending available. Consider adjusting budgets."
                tvPocketStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
                tvInMyPocket.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            }
            else -> {
                tvPocketStatus.text = "Warning: Your expenses exceed your available income!"
                tvPocketStatus.setTextColor(resources.getColor(R.color.expense_red, null))
                tvInMyPocket.setTextColor(resources.getColor(R.color.expense_red, null))
            }
        }

        // Scroll to results
        findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView).post {
            findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView).smoothScrollTo(0, cardResults.top)
        }
    }
}