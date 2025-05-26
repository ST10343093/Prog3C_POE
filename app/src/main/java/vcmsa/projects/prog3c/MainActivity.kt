package vcmsa.projects.prog3c

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import vcmsa.projects.prog3c.data.FirestoreRepository
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modern main activity serving as an elegant dashboard
 * Provides navigation to all major features with a beautiful, card-based UI
 */
class MainActivity : AppCompatActivity() {

    // Firebase Authentication instance for user management
    private lateinit var auth: FirebaseAuth

    // Firestore repository for data access
    private lateinit var firestoreRepository: FirestoreRepository

    // UI components
    private lateinit var tvWelcome: TextView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvActiveBudgets: TextView
    private lateinit var btnLogout: MaterialButton

    // Action cards
    private lateinit var cardAddExpense: MaterialCardView
    private lateinit var cardViewExpenses: MaterialCardView
    private lateinit var cardCategories: MaterialCardView
    private lateinit var cardSetBudget: MaterialCardView
    private lateinit var cardViewBudgets: MaterialCardView
    private lateinit var cardAnalytics: MaterialCardView

    /**
     * Initialize the activity, set up UI components and navigation
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Authentication and Firestore
        auth = FirebaseAuth.getInstance()
        firestoreRepository = FirestoreRepository.getInstance()

        // Initialize UI components
        initializeViews()

        // Check if user is authenticated
        val user = auth.currentUser
        if (user != null) {
            setupWelcomeMessage(user.email ?: "User")
            loadDashboardData()
        } else {
            // If no user is logged in, redirect to login page
            redirectToLogin()
            return
        }

        // Set up click listeners for all action cards
        setupActionCards()

        // Set up logout button
        btnLogout.setOnClickListener {
            logout()
        }
    }

    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvActiveBudgets = findViewById(R.id.tvActiveBudgets)
        btnLogout = findViewById(R.id.btnLogout)

        cardAddExpense = findViewById(R.id.cardAddExpense)
        cardViewExpenses = findViewById(R.id.cardViewExpenses)
        cardCategories = findViewById(R.id.cardCategories)
        cardSetBudget = findViewById(R.id.cardSetBudget)
        cardViewBudgets = findViewById(R.id.cardViewBudgets)
        cardAnalytics = findViewById(R.id.cardAnalytics)
    }

    /**
     * Set up the welcome message with user's information
     */
    private fun setupWelcomeMessage(email: String) {
        // Extract name from email (part before @) and clean it up
        val emailPart = email.substringBefore("@")

        // Remove all numbers from the name
        val cleanName = emailPart.replace(Regex("\\d+"), "")

        // Capitalize first letter
        val userName = cleanName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        tvWelcome.text = userName
    }

    /**
     * Load dashboard summary data
     */
    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                // Get current month date range using Date objects
                val calendar = Calendar.getInstance()
                val endDate = calendar.time

                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val startDate = calendar.time

                // Load total expenses for current month
                val expenses = firestoreRepository.getExpensesByDateRange(startDate, endDate)
                expenses.collect { expenseList ->
                    val total = expenseList.sumOf { it.amount }
                    tvTotalExpenses.text = "R${String.format("%.2f", total)}"
                }

                // Load active budgets count - FIXED VERSION
                val budgets = firestoreRepository.getAllBudgets()
                budgets.collect { budgetList ->
                    val activeBudgets = budgetList.filter { budget ->
                        val now = Timestamp.now() // Use Timestamp instead of Date
                        budget.startDate <= now && budget.endDate >= now
                    }
                    tvActiveBudgets.text = activeBudgets.size.toString()
                }

            } catch (e: Exception) {
                // Handle errors gracefully
                tvTotalExpenses.text = "R0.00"
                tvActiveBudgets.text = "0"
            }
        }
    }

    /**
     * Set up click listeners for all action cards
     */
    private fun setupActionCards() {
        cardAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        cardViewExpenses.setOnClickListener {
            startActivity(Intent(this, ExpensesActivity::class.java))
        }

        cardCategories.setOnClickListener {
            startActivity(Intent(this, CategoriesActivity::class.java))
        }

        cardSetBudget.setOnClickListener {
            startActivity(Intent(this, BudgetActivity::class.java))
        }

        cardViewBudgets.setOnClickListener {
            startActivity(Intent(this, BudgetListActivity::class.java))
        }

        cardAnalytics.setOnClickListener {
            startActivity(Intent(this, CategoriesMonthlySpendingActivity::class.java))
        }
    }

    /**
     * Handle user logout
     */
    private fun logout() {
        auth.signOut()
        redirectToLogin()
    }

    /**
     * Redirect to login screen and clear activity stack
     */
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Refresh dashboard data when returning to this activity
     */
    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }
}