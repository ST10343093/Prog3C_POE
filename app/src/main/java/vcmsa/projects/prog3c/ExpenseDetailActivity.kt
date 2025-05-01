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

class ExpenseDetailActivity : AppCompatActivity() {

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

    private val TAG = "ExpenseDetail"

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

        // Set up edit button
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

        // Set up delete button
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

    private fun loadExpenseDetails(expenseId: Long) {
        Log.d(TAG, "Loading expense details for ID: $expenseId")
        lifecycleScope.launch {
            try {
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

                val category = withContext(Dispatchers.IO) {
                    database.categoryDao().getCategoryById(expense.categoryId)
                }

                displayExpenseDetails(expense, category)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading expense details", e)
                Toast.makeText(this@ExpenseDetailActivity, "Error loading expense: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun displayExpenseDetails(expense: Expense, category: Category?) {
        try {
            // Format and display amount
            tvExpenseAmount.text = "$${String.format("%.2f", expense.amount)}"

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

            // Display photo if available
            if (!expense.photoPath.isNullOrEmpty()) {
                try {
                    // Log for debugging
                    Log.d(TAG, "Photo path: ${expense.photoPath}")

                    if (expense.photoPath!!.startsWith("content://")) {
                        // Handle content URI
                        val uri = Uri.parse(expense.photoPath)

                        // Take persistent URI permission
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
                        // Handle file path
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

    private fun deleteExpense(expenseId: Long) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val expense = database.expenseDao().getExpenseById(expenseId)
                    if (expense != null) {
                        database.expenseDao().deleteExpense(expense)

                        // Delete associated photo file if it exists
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