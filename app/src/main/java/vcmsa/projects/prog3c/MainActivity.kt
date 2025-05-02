package vcmsa.projects.prog3c

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * Main activity of the application serving as the dashboard
 * Provides navigation to all major features of the budget tracking app
 */
class MainActivity : AppCompatActivity() {

    // Firebase Authentication instance for user management
    private lateinit var auth: FirebaseAuth

    /**
     * Initialize the activity, set up UI components and navigation
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Set up the welcome message with user's email
        val welcomeText = findViewById<TextView>(R.id.tvWelcome)

        // Get the current user's information
        val user = auth.currentUser
        if (user != null) {
            welcomeText.text = "Welcome, ${user.email}"
        } else {
            // If no user is logged in, redirect to login page
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Set up categories management button
        val categoriesButton = findViewById<Button>(R.id.btnCategories)
        categoriesButton.setOnClickListener {
            val intent = Intent(this, CategoriesActivity::class.java)
            startActivity(intent)
        }

        // Set up add expense button
        val addExpenseButton = findViewById<Button>(R.id.btnAddExpense)
        addExpenseButton.setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivity(intent)
        }

        // Set up view expenses button
        val viewExpensesButton = findViewById<Button>(R.id.btnViewExpenses)
        viewExpensesButton.setOnClickListener {
            val intent = Intent(this, ExpensesActivity::class.java)
            startActivity(intent)
        }

        // Set up budget management button
        val setBudgetButton = findViewById<Button>(R.id.btnSetBudget)
        setBudgetButton.setOnClickListener {
            val intent = Intent(this, BudgetActivity::class.java)
            startActivity(intent)
        }

        // Set up category spending analysis button
        val viewSpendingCategoryButton = findViewById<Button>(R.id.btnViewSpendingByCategory)
        viewSpendingCategoryButton.setOnClickListener {
            val intent = Intent(this, CategoriesMonthlySpendingActivity::class.java)
            startActivity(intent)
        }

        // Set up logout button
        val logoutButton = findViewById<Button>(R.id.btnLogout)
        logoutButton.setOnClickListener {
            // Sign user out and redirect to login screen
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}