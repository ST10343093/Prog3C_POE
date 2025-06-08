package vcmsa.projects.prog3c

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import vcmsa.projects.prog3c.data.FirestoreRepository
import vcmsa.projects.prog3c.data.FirestoreCategory
import vcmsa.projects.prog3c.data.FirestoreExpense
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Activity for displaying details of a single expense
 * Shows all expense information including amount, date, description, category, and receipt photo if available
 * Provides options to edit or delete the expense
 */
class ExpenseDetailActivity : AppCompatActivity() {

    // Firestore repository and UI component references
    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var tvExpenseAmount: TextView
    private lateinit var tvExpenseDate: TextView
    private lateinit var tvExpenseDescription: TextView
    private lateinit var tvExpenseCategory: TextView
    private lateinit var ivExpensePhoto: ImageView
    private lateinit var photoCard: MaterialCardView
    private lateinit var btnEditExpense: Button
    private lateinit var btnDeleteExpense: Button
    private lateinit var btnBack: Button
    private var currentExpense: FirestoreExpense? = null

    // Logging tag
    private val TAG = "ExpenseDetail"

    /**
     * Initialize the activity, set up UI components and event handlers
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_detail)

        // Initialize Firestore repository
        firestoreRepository = FirestoreRepository.getInstance()

        // Initialize views
        tvExpenseAmount = findViewById(R.id.tvDetailAmount)
        tvExpenseDate = findViewById(R.id.tvDetailDate)
        tvExpenseDescription = findViewById(R.id.tvDetailDescription)
        tvExpenseCategory = findViewById(R.id.tvDetailCategory)
        ivExpensePhoto = findViewById(R.id.ivDetailPhoto)
        photoCard = findViewById(R.id.photoCard)
        btnEditExpense = findViewById(R.id.btnEditExpense)
        btnDeleteExpense = findViewById(R.id.btnDeleteExpense)
        btnBack = findViewById(R.id.btnBackFromDetail)

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Set up edit button - navigate to AddExpenseActivity in edit mode
        btnEditExpense.setOnClickListener {
            val expenseId = intent.getStringExtra("EXPENSE_ID")
            if (!expenseId.isNullOrEmpty()) {
                val intent = Intent(this, AddExpenseActivity::class.java)
                intent.putExtra("EXPENSE_ID", expenseId)
                intent.putExtra("IS_EDIT", true)
                startActivity(intent)
                finish()
            }
        }

        // Set up delete button - show confirmation dialog before deleting
        btnDeleteExpense.setOnClickListener {
            val expenseId = intent.getStringExtra("EXPENSE_ID")
            if (!expenseId.isNullOrEmpty()) {
                showDeleteConfirmationDialog(expenseId)
            }
        }

        try {
            // Get expense ID from intent - now expecting String instead of Long
            val expenseId = intent.getStringExtra("EXPENSE_ID")
            Log.d(TAG, "Received expense ID: $expenseId")

            if (!expenseId.isNullOrEmpty()) {
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
     * Load expense details from Firestore using the provided ID
     * Fetches the associated category information as well
     *
     * @param expenseId The ID of the expense to load
     */
    private fun loadExpenseDetails(expenseId: String) {
        Log.d(TAG, "Loading expense details for ID: $expenseId")
        lifecycleScope.launch {
            try {
                // Fetch expense from Firestore
                val expense = firestoreRepository.getExpenseById(expenseId)

                if (expense == null) {
                    Log.e(TAG, "Expense not found for ID: $expenseId")
                    Toast.makeText(this@ExpenseDetailActivity, "Expense not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                currentExpense = expense
                Log.d(TAG, "Loaded expense: $expense")

                // Fetch the category associated with this expense
                val category = firestoreRepository.getCategoryById(expense.categoryId)

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
     * Display expense details in the UI with improved error handling for images
     * Formats amount and date, displays description and category, and loads the photo if available
     *
     * @param expense The expense object to display
     * @param category The category object associated with the expense
     */
    private fun displayExpenseDetails(expense: FirestoreExpense, category: FirestoreCategory?) {
        try {
            // Format and display amount - Using Rand (R) currency symbol
            tvExpenseAmount.text = "R" + String.format("%.2f", expense.amount)

            // Format and display date
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            tvExpenseDate.text = dateFormat.format(expense.getDate())

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

            // Handle photo display with improved error handling - supports both content URIs and file paths
            if (!expense.photoPath.isNullOrEmpty()) {
                try {
                    // Log for debugging
                    Log.d(TAG, "Photo path: ${expense.photoPath}")

                    // Show the photo card
                    photoCard.visibility = View.VISIBLE

                    if (expense.photoPath!!.startsWith("content://")) {
                        // Handle content URI (photos selected from gallery) - with better error handling
                        try {
                            val uri = Uri.parse(expense.photoPath)

                            // Try to take persistent URI permission first
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

                            // Try to load the image using MediaStore first (more reliable)
                            try {
                                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                                ivExpensePhoto.setImageBitmap(bitmap)
                                Log.d(TAG, "Loaded image from content URI using MediaStore")
                            } catch (e: Exception) {
                                // Fallback to setImageURI
                                ivExpensePhoto.setImageURI(uri)
                                Log.d(TAG, "Loaded image from content URI using setImageURI")
                            }
                        } catch (securityException: SecurityException) {
                            Log.e(TAG, "Permission denied for content URI: ${expense.photoPath}")
                            showImageError("Image no longer accessible. Permission denied.")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading from content URI", e)
                            showImageError("Unable to load image from gallery.")
                        }
                    } else {
                        // Handle file path (photos taken with camera)
                        val photoFile = File(expense.photoPath!!)
                        if (photoFile.exists()) {
                            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                            if (bitmap != null) {
                                ivExpensePhoto.setImageBitmap(bitmap)
                                Log.d(TAG, "Loaded image from file path")
                            } else {
                                Log.e(TAG, "Failed to decode bitmap from file")
                                showImageError("Unable to load image file.")
                            }
                        } else {
                            Log.e(TAG, "Photo file doesn't exist: ${expense.photoPath}")
                            showImageError("Image file not found.")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading image", e)
                    showImageError("Error loading image: ${e.message}")
                }
            } else {
                Log.d(TAG, "No photo path available")
                // Hide the entire photo card when no photo
                photoCard.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying expense details", e)
            Toast.makeText(this, "Error displaying expense details: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shows a user-friendly error message when image loading fails
     * Hides the photo card and shows a toast message
     *
     * @param message The error message to display
     */
    private fun showImageError(message: String) {
        photoCard.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Image error: $message")

        // Optionally, you could show a placeholder or error message in the UI
        // For example, you could add a TextView that says "Image not available"
    }

    /**
     * Shows a confirmation dialog before deleting an expense
     *
     * @param expenseId The ID of the expense to delete
     */
    private fun showDeleteConfirmationDialog(expenseId: String) {
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
     * Deletes the expense from Firestore
     * Also deletes associated photo file if it exists
     *
     * @param expenseId The ID of the expense to delete
     */
    private fun deleteExpense(expenseId: String) {
        lifecycleScope.launch {
            try {
                val expense = firestoreRepository.getExpenseById(expenseId)
                if (expense != null) {
                    // Delete expense from Firestore
                    firestoreRepository.deleteExpense(expenseId)

                    // Clean up associated photo file if it exists and it's a local file
                    if (!expense.photoPath.isNullOrEmpty() && !expense.photoPath!!.startsWith("content://")) {
                        try {
                            val photoFile = File(expense.photoPath!!)
                            if (photoFile.exists()) {
                                photoFile.delete()
                                Log.d(TAG, "Deleted photo file: ${expense.photoPath}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deleting photo file", e)
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