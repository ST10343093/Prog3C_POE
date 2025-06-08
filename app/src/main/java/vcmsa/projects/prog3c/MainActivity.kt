package vcmsa.projects.prog3c

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import vcmsa.projects.prog3c.data.FirestoreRepository
import vcmsa.projects.prog3c.utils.NavigationHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modern main activity serving as an elegant dashboard
 * Provides navigation to all major features with a beautiful, card-based UI
 * Now includes bottom navigation for easy access to main features
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
    private lateinit var bottomNavigationView: BottomNavigationView

    // Action cards - including new In My Pocket feature
    private lateinit var cardAddExpense: MaterialCardView
    private lateinit var cardViewExpenses: MaterialCardView
    private lateinit var cardCategories: MaterialCardView
    private lateinit var cardSetBudget: MaterialCardView
    private lateinit var cardViewBudgets: MaterialCardView
    private lateinit var cardAnalytics: MaterialCardView
    private lateinit var cardInMyPocket: MaterialCardView
    private lateinit var cardSpendingBreakdown: MaterialCardView

    /**
     * Initialize the activity, set up UI components and navigation
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // TEST LOG
        android.util.Log.d("MainActivity", "=== TEST - MainActivity onCreate called ===")

        // Initialize Firebase Authentication and Firestore
        auth = FirebaseAuth.getInstance()
        firestoreRepository = FirestoreRepository.getInstance()

        // Initialize UI components
        initializeViews()

        // Check if user is authenticated
        val user = auth.currentUser
        if (user != null) {
            android.util.Log.d("MainActivity", "=== TEST - User authenticated, setting up ===")
            setupWelcomeMessage(user.email ?: "User")
            loadDashboardData()
        } else {
            // If no user is logged in, redirect to login page
            android.util.Log.d("MainActivity", "=== TEST - User not authenticated, redirecting ===")
            redirectToLogin()
            return
        }

        // Set up click listeners for all action cards
        setupActionCards()

        // Set up bottom navigation
        setupBottomNavigation()

        // Set up logout button
        btnLogout.setOnClickListener {
            logout()
        }
    }

    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        android.util.Log.d("MainActivity", "=== TEST - Initializing views ===")
        tvWelcome = findViewById(R.id.tvWelcome)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvActiveBudgets = findViewById(R.id.tvActiveBudgets)
        btnLogout = findViewById(R.id.btnLogout)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        cardAddExpense = findViewById(R.id.cardAddExpense)
        cardViewExpenses = findViewById(R.id.cardViewExpenses)
        cardCategories = findViewById(R.id.cardCategories)
        cardSetBudget = findViewById(R.id.cardSetBudget)
        cardViewBudgets = findViewById(R.id.cardViewBudgets)
        cardAnalytics = findViewById(R.id.cardAnalytics)
        cardInMyPocket = findViewById(R.id.cardInMyPocket)
        cardSpendingBreakdown = findViewById(R.id.cardSpendingBreakdown)
    }

    /**
     * Set up bottom navigation with NavigationHelper
     */
    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home, do nothing or refresh
                    true
                }
                R.id.nav_expenses -> {
                    NavigationHelper.navigateToActivity(this, ExpensesActivity::class.java)
                    true
                }
                R.id.nav_add_expense -> {
                    NavigationHelper.navigateToAddExpense(this)
                    true
                }
                R.id.nav_budgets -> {
                    NavigationHelper.navigateToBudgets(this, NavigationHelper.BudgetAction.VIEW)
                    true
                }
                R.id.nav_analytics -> {
                    NavigationHelper.navigateToAnalytics(this, NavigationHelper.AnalyticsType.MONTHLY)
                    true
                }
                else -> false
            }
        }

        // Set home as selected by default
        bottomNavigationView.selectedItemId = R.id.nav_home
    }

    /**
     * Set up the welcome message with user's information
     * FIXED: Handles long email names intelligently to prevent UI wrapping
     */
    private fun setupWelcomeMessage(email: String) {
        android.util.Log.d("MainActivity", "=== TEST - Setting up welcome message for: $email ===")

        // Extract name from email (part before @)
        val emailPart = email.substringBefore("@")

        // Remove all numbers from the name
        val cleanName = emailPart.replace(Regex("\\d+"), "")

        // Smart name extraction logic
        val displayName = extractSmartDisplayName(cleanName)

        // Capitalize first letter
        val userName = displayName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        tvWelcome.text = userName
    }

    /**
     * Intelligently extract a display name from email prefix
     * Handles various email formats and prevents UI wrapping
     * IMPROVED: Recognizes common long names like Mohammed, Christopher, etc.
     */
    private fun extractSmartDisplayName(emailPrefix: String): String {
        // Maximum characters to display (prevents wrapping on most devices)
        val maxDisplayLength = 12

        // If the name is short enough, return as is
        if (emailPrefix.length <= maxDisplayLength) {
            return emailPrefix
        }

        // Try different smart extraction methods

        // Method 1: Check for common separators (dots, underscores, hyphens)
        val separators = listOf(".", "_", "-")
        for (separator in separators) {
            if (emailPrefix.contains(separator)) {
                val firstPart = emailPrefix.substringBefore(separator)
                if (firstPart.isNotEmpty() && firstPart.length <= maxDisplayLength) {
                    return firstPart
                }
            }
        }

        // Method 2: Check for camelCase (e.g., johnSmith -> john)
        val camelCaseMatch = Regex("^[a-z]+(?=[A-Z])").find(emailPrefix)
        if (camelCaseMatch != null) {
            val firstCamelPart = camelCaseMatch.value
            if (firstCamelPart.length <= maxDisplayLength) {
                return firstCamelPart
            }
        }

        // Method 3: NEW - Check for common long names first
        val commonLongNames = listOf(
            "mohammed", "muhammad", "christopher", "alexander", "elizabeth",
            "sebastian", "nathaniel", "gabrielle", "stephanie", "anthony",
            "francisco", "alessandro", "valentina", "anastasia", "maximilian",
            "konstantin", "alessandro", "francesca", "christian", "patricia"
        )

        val lowerEmail = emailPrefix.lowercase()
        for (commonName in commonLongNames) {
            if (lowerEmail.startsWith(commonName) && commonName.length <= maxDisplayLength) {
                return emailPrefix.substring(0, commonName.length)
            }
        }

        // Method 4: Look for common name patterns (original logic)
        val commonFirstNameLength = findLikelyFirstNameEnd(emailPrefix)
        if (commonFirstNameLength > 0 && commonFirstNameLength <= maxDisplayLength) {
            return emailPrefix.substring(0, commonFirstNameLength)
        }

        // Method 5: Fallback - truncate and add ellipsis
        return emailPrefix.substring(0, maxDisplayLength - 3) + "..."
    }

    /**
     * Helper function to find likely end of first name
     * Looks for common first name patterns
     */
    private fun findLikelyFirstNameEnd(name: String): Int {
        // Common first name lengths (most first names are 3-8 characters)
        val commonFirstNameLengths = listOf(4, 5, 6, 7, 8, 3)

        for (length in commonFirstNameLengths) {
            if (length < name.length) {
                // Check if this creates a reasonable break
                val potentialFirstName = name.substring(0, length)
                val nextChar = name[length]

                // Good break points: vowel followed by consonant, or consonant cluster
                if (isGoodNameBreakPoint(potentialFirstName, nextChar)) {
                    return length
                }
            }
        }

        return 0 // No good break point found
    }

    /**
     * Helper function to determine if this is a good place to break a name
     */
    private fun isGoodNameBreakPoint(namePart: String, nextChar: Char): Boolean {
        if (namePart.isEmpty()) return false

        val vowels = "aeiouAEIOU"
        val lastChar = namePart.last()

        // Good break: vowel followed by consonant (e.g., "moha|mmed")
        if (vowels.contains(lastChar) && !vowels.contains(nextChar)) {
            return true
        }

        // Good break: consonant followed by different consonant (e.g., "moh|amm")
        if (!vowels.contains(lastChar) && !vowels.contains(nextChar) && lastChar != nextChar) {
            return true
        }

        return false
    }

    /**
     * Load dashboard summary data - FIXED VERSION
     */
    private fun loadDashboardData() {
        android.util.Log.d("MainActivity", "=== TEST - loadDashboardData called ===")
        lifecycleScope.launch {
            android.util.Log.d("MainActivity", "=== TEST - inside lifecycleScope ===")
            try {
                android.util.Log.d("MainActivity", "=== TEST - starting try block ===")

                // Get current month date range using Date objects
                val calendar = Calendar.getInstance()
                val monthEndDate = calendar.time

                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val monthStartDate = calendar.time

                android.util.Log.d("MainActivity", "=== TEST - Date range: $monthStartDate to $monthEndDate ===")

                // Load total expenses for current month - SEPARATE LAUNCH
                android.util.Log.d("MainActivity", "=== TEST - Loading expenses ===")
                launch {
                    val expenses = firestoreRepository.getExpensesByDateRange(monthStartDate, monthEndDate)
                    expenses.collect { expenseList ->
                        android.util.Log.d("MainActivity", "=== TEST - Got ${expenseList.size} expenses ===")
                        val total = expenseList.sumOf { it.amount }
                        tvTotalExpenses.text = "R${String.format("%.2f", total)}"
                        android.util.Log.d("MainActivity", "=== TEST - Total expenses: R$total ===")
                    }
                }

                // FIXED: Load active budgets count - SEPARATE LAUNCH
                android.util.Log.d("MainActivity", "=== TEST - Loading budgets ===")
                launch {
                    val budgets = firestoreRepository.getAllBudgets()
                    budgets.collect { budgetList ->
                        android.util.Log.d("MainActivity", "=== TEST - Got ${budgetList.size} budgets ===")
                        val now = Date()

                        val activeBudgets = budgetList.filter { budget ->
                            val budgetStart = budget.getStartDate()
                            val budgetEnd = budget.getEndDate()

                            // FIXED: Use same date comparison logic as BudgetActivity
                            // Normalize current time to middle of day to avoid edge cases
                            val nowNormalized = Calendar.getInstance().apply {
                                time = now
                                set(Calendar.HOUR_OF_DAY, 12)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.time

                            // Check if current date falls within budget period
                            val isActive = nowNormalized >= budgetStart && nowNormalized <= budgetEnd

                            // Debug logging for each budget
                            android.util.Log.d("MainActivity", "=== BUDGET CHECK ===")
                            android.util.Log.d("MainActivity", "Budget ID: ${budget.id}")
                            android.util.Log.d("MainActivity", "Category: ${budget.categoryId}")
                            android.util.Log.d("MainActivity", "Budget Start: $budgetStart")
                            android.util.Log.d("MainActivity", "Budget End: $budgetEnd")
                            android.util.Log.d("MainActivity", "Current Time: $nowNormalized")
                            android.util.Log.d("MainActivity", "Is Active: $isActive")
                            android.util.Log.d("MainActivity", "==================")

                            isActive
                        }

                        tvActiveBudgets.text = activeBudgets.size.toString()

                        // Summary debug logging
                        android.util.Log.d("MainActivity", "=== SUMMARY ===")
                        android.util.Log.d("MainActivity", "Total budgets: ${budgetList.size}")
                        android.util.Log.d("MainActivity", "Active budgets: ${activeBudgets.size}")
                        android.util.Log.d("MainActivity", "Current date: $now")

                        // List active budget IDs
                        activeBudgets.forEach { budget ->
                            android.util.Log.d("MainActivity", "Active budget: ${budget.id}")
                        }
                        android.util.Log.d("MainActivity", "===============")
                    }
                }

            } catch (e: Exception) {
                // Handle errors gracefully
                android.util.Log.e("MainActivity", "=== ERROR in loadDashboardData ===", e)
                tvTotalExpenses.text = "R0.00"
                tvActiveBudgets.text = "0"
            }
        }
    }

    /**
     * Set up click listeners for all action cards
     */
    private fun setupActionCards() {
        android.util.Log.d("MainActivity", "=== TEST - Setting up action cards ===")
        cardAddExpense.setOnClickListener {
            NavigationHelper.navigateToAddExpense(this)
        }

        cardViewExpenses.setOnClickListener {
            NavigationHelper.navigateToActivity(this, ExpensesActivity::class.java)
        }

        cardCategories.setOnClickListener {
            NavigationHelper.navigateToCategories(this)
        }

        cardSetBudget.setOnClickListener {
            NavigationHelper.navigateToBudgets(this, NavigationHelper.BudgetAction.CREATE)
        }

        cardViewBudgets.setOnClickListener {
            NavigationHelper.navigateToBudgets(this, NavigationHelper.BudgetAction.VIEW)
        }

        cardAnalytics.setOnClickListener {
            NavigationHelper.navigateToAnalytics(this, NavigationHelper.AnalyticsType.MONTHLY)
        }

        // NEW: In My Pocket feature
        cardInMyPocket.setOnClickListener {
            NavigationHelper.navigateToSmartFeature(this, NavigationHelper.SmartFeature.IN_MY_POCKET)
        }

        // NEW: Spending Breakdown feature
        cardSpendingBreakdown.setOnClickListener {
            NavigationHelper.navigateToAnalytics(this, NavigationHelper.AnalyticsType.BREAKDOWN)
        }
    }

    /**
     * Handle user logout
     */
    private fun logout() {
        android.util.Log.d("MainActivity", "=== TEST - User logging out ===")
        auth.signOut()
        NavigationHelper.navigateToWelcome(this)
    }

    /**
     * Redirect to login screen and clear activity stack
     */
    private fun redirectToLogin() {
        android.util.Log.d("MainActivity", "=== TEST - Redirecting to login ===")
        NavigationHelper.navigateToWelcome(this)
    }

    /**
     * Refresh dashboard data when returning to this activity
     */
    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "=== TEST - onResume called ===")

        // Set home as selected when returning to MainActivity
        bottomNavigationView.selectedItemId = R.id.nav_home

        loadDashboardData()
    }
}