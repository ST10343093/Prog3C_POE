package vcmsa.projects.prog3c

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Set up the welcome message
        val welcomeText = findViewById<TextView>(R.id.tvWelcome)

        // Get the current user's email
        val user = auth.currentUser
        if (user != null) {
            welcomeText.text = "Welcome, ${user.email}"
        } else {
            // If no user is logged in, redirect to login page
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Set up navigation buttons
        val categoriesButton = findViewById<Button>(R.id.btnCategories)
        categoriesButton.setOnClickListener {
            val intent = Intent(this, CategoriesActivity::class.java)
            startActivity(intent)
        }

        // Add navigation to Add Expense screen
        val addExpenseButton = findViewById<Button>(R.id.btnAddExpense)
        addExpenseButton.setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivity(intent)
        }

        // Add navigation to View Expenses screen
        val viewExpensesButton = findViewById<Button>(R.id.btnViewExpenses)
        viewExpensesButton.setOnClickListener {
            val intent = Intent(this, ExpensesActivity::class.java)
            startActivity(intent)
        }

        // Add navigation to Budget screen
        val setBudgetButton = findViewById<Button>(R.id.btnSetBudget)
        setBudgetButton.setOnClickListener {
            val intent = Intent(this, BudgetActivity::class.java)
            startActivity(intent)
        }

        // Add navigation to Budget screen
        val viewSpendingCategoryButton = findViewById<Button>(R.id.btnViewSpendingByCategory)
        viewSpendingCategoryButton.setOnClickListener {
            val intent = Intent(this, CategoriesMonthlySpendingActivity::class.java)
            startActivity(intent)
        }

        // Set up logout button
        val logoutButton = findViewById<Button>(R.id.btnLogout)
        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}