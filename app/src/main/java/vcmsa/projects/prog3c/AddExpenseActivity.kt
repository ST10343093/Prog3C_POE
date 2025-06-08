package vcmsa.projects.prog3c

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import vcmsa.projects.prog3c.data.FirestoreRepository
import vcmsa.projects.prog3c.data.FirestoreCategory
import vcmsa.projects.prog3c.data.FirestoreExpense
import vcmsa.projects.prog3c.utils.NavigationHelper
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity for adding new expenses or editing existing ones
 * Allows users to enter expense details, select categories, and attach receipt photos
 * Now includes smart navigation integration
 */
class AddExpenseActivity : AppCompatActivity() {

    // Firestore repository reference
    private lateinit var firestoreRepository: FirestoreRepository

    // UI elements
    private lateinit var etAmount: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etDate: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnTakePhoto: Button
    private lateinit var btnChoosePhoto: Button
    private lateinit var ivReceiptPhoto: ImageView
    private lateinit var btnSaveExpense: Button
    private lateinit var btnBackFromExpense: Button

    // Data variables
    private var selectedDate: Date = Date()            // Currently selected date
    private var selectedCategoryId: String = ""        // Currently selected category ID
    private var categories: List<FirestoreCategory> = emptyList() // List of available categories
    private var currentPhotoPath: String? = null       // File path to stored photo
    private var photoUri: Uri? = null                  // URI reference to photo

    // Edit mode variables
    private var isEditMode = false                     // Whether we're editing existing expense
    private var currentExpenseId: String = ""          // ID of expense being edited

    companion object {
        // Constants for activity result handling
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_GALLERY_IMAGE = 2
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val TAG = "AddExpenseActivity"    // Tag for logging
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        // Initialize Firestore repository
        firestoreRepository = FirestoreRepository.getInstance()

        // Initialize views
        etAmount = findViewById(R.id.etExpenseAmount)
        etDescription = findViewById(R.id.etExpenseDescription)
        etDate = findViewById(R.id.etExpenseDate)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnChoosePhoto = findViewById(R.id.btnChoosePhoto)
        ivReceiptPhoto = findViewById(R.id.ivReceiptPhoto)
        btnSaveExpense = findViewById(R.id.btnSaveExpense)
        btnBackFromExpense = findViewById(R.id.btnBackFromExpense)

        // Check if we're in edit mode
        isEditMode = intent.getBooleanExtra("IS_EDIT", false)
        if (isEditMode) {
            currentExpenseId = intent.getStringExtra("EXPENSE_ID") ?: ""
            if (currentExpenseId.isNotEmpty()) {
                // Change title and button text
                findViewById<TextView>(R.id.tvAddExpenseTitle).text = "Edit Expense"
                btnSaveExpense.text = "Update Expense"

                // Load expense data
                loadExpenseForEdit(currentExpenseId)
            } else {
                isEditMode = false
            }
        }

        // Set up date picker
        setupDatePicker()

        // Set default date to today
        updateDateText()

        // Load categories for spinner
        loadCategories()

        // Set up take photo button
        btnTakePhoto.setOnClickListener {
            checkCameraPermission()
        }

        // Set up choose photo from gallery button
        btnChoosePhoto.setOnClickListener {
            openGallery()
        }

        // Set up back button with smart navigation
        btnBackFromExpense.setOnClickListener {
            NavigationHelper.navigateBack(this)
        }

        // Set up save button
        btnSaveExpense.setOnClickListener {
            saveExpense()
        }
    }

    /**
     * Sets up the date picker dialog for selecting expense date
     * Makes the date field clickable to open a date picker
     */
    private fun setupDatePicker() {
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate

            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    selectedDate = calendar.time
                    updateDateText()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    /**
     * Updates the text in the date field to display the currently selected date
     */
    private fun updateDateText() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        etDate.setText(dateFormat.format(selectedDate))
    }

    /**
     * Loads categories from Firestore and sets up the category spinner
     * Provides smart navigation to categories if none exist
     */
    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                categories = firestoreRepository.getAllCategories().first()

                if (categories.isEmpty()) {
                    Toast.makeText(
                        this@AddExpenseActivity,
                        "No categories found. Let's create some first!",
                        Toast.LENGTH_LONG
                    ).show()
                    // Smart navigation to categories with context
                    NavigationHelper.navigateToCategories(this@AddExpenseActivity, fromExpenseFlow = true)
                    finish()
                    return@launch
                }

                // Create adapter for spinner
                val adapter = ArrayAdapter(
                    this@AddExpenseActivity,
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

                // If in edit mode and we have the category data now, reselect the appropriate category
                if (isEditMode && currentExpenseId.isNotEmpty()) {
                    val expense = firestoreRepository.getExpenseById(currentExpenseId)
                    if (expense != null) {
                        val categoryPosition = categories.indexOfFirst { it.id == expense.categoryId }
                        if (categoryPosition >= 0) {
                            spinnerCategory.setSelection(categoryPosition)
                        }
                    }
                }
            } catch (e: Exception) {
                // Don't show error messages for cancellation (normal when leaving screen)
                if (e is CancellationException) {
                    Log.d(TAG, "Category loading cancelled (normal when leaving screen)")
                    return@launch
                }

                Toast.makeText(this@AddExpenseActivity, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
                NavigationHelper.navigateBack(this@AddExpenseActivity)
            }
        }
    }

    /**
     * Loads expense data when in edit mode
     * Populates the form with the existing expense details
     * @param expenseId ID of the expense to edit
     */
    private fun loadExpenseForEdit(expenseId: String) {
        lifecycleScope.launch {
            try {
                val expense = firestoreRepository.getExpenseById(expenseId)

                if (expense == null) {
                    Toast.makeText(this@AddExpenseActivity, "Expense not found", Toast.LENGTH_SHORT).show()
                    NavigationHelper.navigateBack(this@AddExpenseActivity)
                    return@launch
                }

                // Fill the form with expense data
                etAmount.setText(expense.amount.toString())
                etDescription.setText(expense.description)
                selectedDate = expense.getDate()
                updateDateText()
                currentPhotoPath = expense.photoPath

                // Show photo if it exists
                if (!expense.photoPath.isNullOrEmpty()) {
                    try {
                        Log.d(TAG, "Loading photo from path: ${expense.photoPath}")
                        ivReceiptPhoto.visibility = View.VISIBLE

                        if (expense.photoPath!!.startsWith("content://")) {
                            // It's a content URI
                            photoUri = Uri.parse(expense.photoPath)
                            ivReceiptPhoto.setImageURI(photoUri)
                            Log.d(TAG, "Loaded image from content URI")
                        } else {
                            // It's a file path
                            val photoFile = File(expense.photoPath!!)
                            if (photoFile.exists()) {
                                Log.d(TAG, "Photo file exists, loading bitmap")
                                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                ivReceiptPhoto.setImageBitmap(bitmap)
                            } else {
                                Log.e(TAG, "Photo file does not exist: ${expense.photoPath}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading image", e)
                        Toast.makeText(this@AddExpenseActivity, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                // Category will be selected when the categories are loaded in loadCategories()
            } catch (e: Exception) {
                // Don't show error messages for cancellation (normal when leaving screen)
                if (e is CancellationException) {
                    Log.d(TAG, "Expense loading cancelled (normal when leaving screen)")
                    return@launch
                }

                Toast.makeText(this@AddExpenseActivity, "Error loading expense: ${e.message}", Toast.LENGTH_SHORT).show()
                NavigationHelper.navigateBack(this@AddExpenseActivity)
            }
        }
    }

    /**
     * Copies an image from a content URI to app's internal storage
     * This prevents permission issues when accessing the image later
     * @param sourceUri The content URI of the image to copy
     * @return The file path of the copied image, or null if failed
     */
    private fun copyImageToAppStorage(sourceUri: Uri): String? {
        try {
            val fileName = "expense_${System.currentTimeMillis()}.jpg"
            val storageDir = File(filesDir, "expense_images")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            val destFile = File(storageDir, fileName)

            contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Log.d(TAG, "Image copied to: ${destFile.absolutePath}")
            return destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error copying image to app storage", e)
            return null
        }
    }

    /**
     * Opens the device gallery to select an image
     * Uses ACTION_OPEN_DOCUMENT to get persistable URI permissions
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE)
    }

    /**
     * Checks and requests camera permission if needed
     * Launches camera if permission is granted
     */
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            dispatchTakePictureIntent()
        }
    }

    /**
     * Handles permission request results
     * Launches camera if permission is granted
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required to take photos",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Launches the camera app to take a photo
     * Creates a file to store the photo and passes it to the camera app
     */
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.e(TAG, "Error creating image file", ex)
                    Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "vcmsa.projects.prog3c.fileprovider",
                        it
                    )
                    photoUri = photoURI
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    /**
     * Creates a new image file with unique name to store the photo
     * @return File The created empty file
     * @throws IOException If file creation fails
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
            Log.d(TAG, "Created image file: $currentPhotoPath")
        }
    }

    /**
     * Handles results from camera and gallery intents
     * Loads and displays the selected or captured image
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    // Show the image
                    ivReceiptPhoto.visibility = View.VISIBLE

                    // Load the taken picture and show it
                    try {
                        if (currentPhotoPath != null) {
                            Log.d(TAG, "Loading camera image from: $currentPhotoPath")
                            val bitmap = BitmapFactory.decodeFile(currentPhotoPath!!)
                            ivReceiptPhoto.setImageBitmap(bitmap)
                        } else if (photoUri != null) {
                            Log.d(TAG, "Loading camera image from URI: $photoUri")
                            ivReceiptPhoto.setImageURI(photoUri)
                            currentPhotoPath = photoUri.toString()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading camera image", e)
                        Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_GALLERY_IMAGE -> {
                    // Handle gallery image selection - IMPROVED VERSION
                    data?.data?.let { uri ->
                        try {
                            Log.d(TAG, "Selected gallery image: $uri")

                            // Copy the image to app's internal storage instead of relying on URI
                            val copiedImagePath = copyImageToAppStorage(uri)

                            if (copiedImagePath != null) {
                                currentPhotoPath = copiedImagePath
                                Log.d(TAG, "Image copied to app storage: $currentPhotoPath")

                                // Show the image
                                ivReceiptPhoto.visibility = View.VISIBLE
                                val bitmap = BitmapFactory.decodeFile(copiedImagePath)
                                ivReceiptPhoto.setImageBitmap(bitmap)

                                Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing gallery image", e)
                            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    /**
     * Validates input fields and saves the expense to Firestore
     * Updates an existing expense if in edit mode
     * Creates a new expense if in add mode
     * Provides smart navigation after successful save
     */
    private fun saveExpense() {
        val amountText = etAmount.text.toString().trim()
        val description = etDescription.text.toString().trim()

        // Validate inputs
        if (amountText.isEmpty()) {
            etAmount.error = "Amount is required"
            etAmount.requestFocus()
            return
        }

        val amount = try {
            amountText.toDouble()
        } catch (e: NumberFormatException) {
            etAmount.error = "Invalid amount format"
            etAmount.requestFocus()
            return
        }

        if (amount <= 0) {
            etAmount.error = "Amount must be greater than zero"
            etAmount.requestFocus()
            return
        }

        if (description.isEmpty()) {
            etDescription.error = "Description is required"
            etDescription.requestFocus()
            return
        }

        if (selectedCategoryId.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        // Log photo path before saving
        Log.d(TAG, "Saving expense with photo path: $currentPhotoPath")

        // Create expense object
        val expense = if (isEditMode) {
            FirestoreExpense(
                id = currentExpenseId,
                amount = amount,
                description = description,
                date = selectedDate,
                categoryId = selectedCategoryId,
                photoPath = currentPhotoPath,
                userId = "" // Will be set by repository
            )
        } else {
            FirestoreExpense(
                id = "",
                amount = amount,
                description = description,
                date = selectedDate,
                categoryId = selectedCategoryId,
                photoPath = currentPhotoPath,
                userId = "" // Will be set by repository
            )
        }

        // Save expense to Firestore
        lifecycleScope.launch {
            try {
                if (isEditMode) {
                    firestoreRepository.updateExpense(expense)
                } else {
                    firestoreRepository.insertExpense(expense)
                }

                val message = if (isEditMode) "Expense updated" else "Expense saved"
                Toast.makeText(this@AddExpenseActivity, message, Toast.LENGTH_SHORT).show()

                // Smart navigation back after successful save
                NavigationHelper.navigateBack(this@AddExpenseActivity)
            } catch (e: Exception) {
                // Don't show error messages for cancellation (normal when leaving screen)
                if (e is CancellationException) {
                    Log.d(TAG, "Save expense cancelled (normal when leaving screen)")
                    return@launch
                }

                Toast.makeText(this@AddExpenseActivity, "Error saving expense: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error saving expense", e)
            }
        }
    }

    /**
     * Handle back button press with smart navigation
     */
    override fun onBackPressed() {
        NavigationHelper.navigateBack(this)
    }
}