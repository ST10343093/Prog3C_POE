package vcmsa.projects.prog3c

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vcmsa.projects.prog3c.data.AppDatabase
import vcmsa.projects.prog3c.data.Category
import vcmsa.projects.prog3c.data.Expense
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Activity for displaying details of a single expense
 * Shows all expense information including amount, date, description, category, and receipt photo if available
 * Provides options to edit or delete the expense
 */
class ExpenseDetailActivity : AppCompatActivity() {

    // Database and UI component references
    private lateinit var database: AppDatabase
    private lateinit var tvExpenseAmount: TextView
    private lateinit var tvExpenseDate: TextView
    private lateinit var tvExpenseDescription: TextView
    private lateinit var tvExpenseCategory: TextView
    private lateinit var ivExpensePhoto: ImageView
    private lateinit var btnEditExpense: Button
    private lateinit var btnDeleteExpense: Button
    private lateinit var btnBack: Button
    private var currentExpense: Expense? = null

    // Logging tag
    private val TAG = "ExpenseDetail"

    /**
     * Initialize the activity, set up UI components and event handlers
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_detail)

        // Initialize database
        database = AppDatabase.getDatabase(this)

        // Initialize views
        tvExpenseAmount = findViewById(R.id.tvDetailAmount)
        tvExpenseDate = findViewById(R.id.tvDetailDate)
        tvExpenseDescription = findViewById(R.id.tvDetailDescription)
        tvExpenseCategory = findViewById(R.id.tvDetailCategory)
        ivExpensePhoto = findViewById(R.id.ivDetailPhoto)
        btnEditExpense = findViewById(R.id.btnEditExpense)
        btnDeleteExpense = findViewById(R.id.btnDeleteExpense)
        btnBack = findViewById(R.id.btnBackFromDetail)

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Set up edit button - navigate to AddExpenseActivity in edit mode
        btnEditExpense.setOnClickListener {
            val expenseId = intent.getLongExtra("EXPENSE_ID", -1)
            if (expenseId != -1L) {
                val intent = Intent(this, AddExpenseActivity::class.java)
                intent.putExtra("EXPENSE_ID", expenseId)
                intent.putExtra("IS_EDIT", true)
                startActivity(intent)
                finish()
            }
        }

        // Set up delete button - show confirmation dialog before deleting
        btnDeleteExpense.setOnClickListener {
            val expenseId = intent.getLongExtra("EXPENSE_ID", -1)
            if (expenseId != -1L) {
                showDeleteConfirmationDialog(expenseId)
            }
        }

        try {
            // Get expense ID from intent
            val expenseId = intent.getLongExtra("EXPENSE_ID", -1)
            Log.d(TAG, "Received expense ID: $expenseId")

            if (expenseId != -1L) {
                loadExpenseDetails(expenseId)
            } else {
                Log.e(TAG, "Invalid expense ID: $expenseId")
                Toast.makeText(this, "Invalid expense ID", Toast.LENGTH_SHORT).show()
                finish() // Close if no valid ID
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error loading expense: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Load expense details from the database using the provided ID
     * Fetches the associated category information as well
     *
     * @param expenseId The ID of the expense to load
     */
    private fun loadExpenseDetails(expenseId: Long) {
        Log.d(TAG, "Loading expense details for ID: $expenseId")
        lifecycleScope.launch {
            try {
                // Fetch expense from database
                val expense = withContext(Dispatchers.IO) {
                    database.expenseDao().getExpenseById(expenseId)
                }

                if (expense == null) {
                    Log.e(TAG, "Expense not found for ID: $expenseId")
                    Toast.makeText(this@ExpenseDetailActivity, "Expense not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                currentExpense = expense
                Log.d(TAG, "Loaded expense: $expense")

                // Fetch the category associated with this expense
                val category = withContext(Dispatchers.IO) {
                    database.categoryDao().getCategoryById(expense.categoryId)
                }

                // Display the loaded expense details
                displayExpenseDetails(expense, category)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading expense details", e)
                Toast.makeText(this@ExpenseDetailActivity, "Error loading expense: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    /**
     * Display expense details in the UI
     * Formats amount and date, displays description and category, and loads the photo if available
     *
     * @param expense The expense object to display
     * @param category The category object associated with the expense
     */
    private fun displayExpenseDetails(expense: Expense, category: Category?) {
        try {
            // Format and display amount - Using Rand (R) currency symbol
            tvExpenseAmount.text = "R" + String.format("%.2f", expense.amount)

            // Format and display date
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            tvExpenseDate.text = dateFormat.format(expense.date)

            // Display description
            tvExpenseDescription.text = expense.description

            // Display category
            if (category != null) {
                tvExpenseCategory.text = category.name
                tvExpenseCategory.setBackgroundColor(category.color)
            } else {
                tvExpenseCategory.text = "Unknown Category"
                tvExpenseCategory.setBackgroundColor(android.graphics.Color.GRAY)
            }

            // Handle photo display - supports both content URIs and file paths
            if (!expense.photoPath.isNullOrEmpty()) {
                try {
                    // Log for debugging
                    Log.d(TAG, "Photo path: ${expense.photoPath}")

                    if (expense.photoPath!!.startsWith("content://")) {
                        // Handle content URI (photos selected from gallery)
                        val uri = Uri.parse(expense.photoPath)

                        // Attempt to take persistent URI permission to access the photo
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            Log.d(TAG, "Took persistent permission for URI")
                        } catch (e: SecurityException) {
                            Log.e(TAG, "Failed to take permission: ${e.message}")
                            // Continue anyway, might still work
                        }

                        ivExpensePhoto.setImageURI(uri)
                        ivExpensePhoto.visibility = View.VISIBLE
                        Log.d(TAG, "Loaded image from content URI")
                    } else {
                        // Handle file path (photos taken with camera)
                        val photoFile = File(expense.photoPath!!)
                        if (photoFile.exists()) {
                            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                            ivExpensePhoto.setImageBitmap(bitmap)
                            ivExpensePhoto.visibility = View.VISIBLE
                            Log.d(TAG, "Loaded image from file path")
                        } else {
                            Log.e(TAG, "Photo file doesn't exist: ${expense.photoPath}")
                            ivExpensePhoto.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading image", e)
                    Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                    ivExpensePhoto.visibility = View.GONE
                }
            } else {
                Log.d(TAG, "No photo path available")
                ivExpensePhoto.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying expense details", e)
            Toast.makeText(this, "Error displaying expense details: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shows a confirmation dialog before deleting an expense
     *
     * @param expenseId The ID of the expense to delete
     */
    private fun showDeleteConfirmationDialog(expenseId: Long) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                deleteExpense(expenseId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Deletes the expense from the database
     * Also deletes associated photo file if it exists
     *
     * @param expenseId The ID of the expense to delete
     */
    private fun deleteExpense(expenseId: Long) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val expense = database.expenseDao().getExpenseById(expenseId)
                    if (expense != null) {
                        // Delete expense from database
                        database.expenseDao().deleteExpense(expense)

                        // Clean up associated photo file if it exists
                        if (!expense.photoPath.isNullOrEmpty() && !expense.photoPath!!.startsWith("content://")) {
                            try {
                                val photoFile = File(expense.photoPath!!)
                                if (photoFile.exists()) {
                                    photoFile.delete()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deleting photo file", e)
                            }
                        }
                    }
                }
                Toast.makeText(this@ExpenseDetailActivity, "Expense deleted", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting expense", e)
                Toast.makeText(this@ExpenseDetailActivity, "Error deleting expense: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}