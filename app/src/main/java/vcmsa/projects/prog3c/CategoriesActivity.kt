package vcmsa.projects.prog3c

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vcmsa.projects.prog3c.data.AppDatabase
import vcmsa.projects.prog3c.data.Category
import yuku.ambilwarna.AmbilWarnaDialog

class CategoriesActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: CategoryAdapter
    private var selectedColor: Int = Color.BLUE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize database
        database = AppDatabase.getDatabase(this)

        // Set up RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.rvCategories)
        adapter = CategoryAdapter(emptyList(), ::editCategory, ::deleteCategory)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Set up color picker button
        val colorPickerButton = findViewById<Button>(R.id.btnColorPicker)
        colorPickerButton.setOnClickListener {
            openColorPicker()
        }
        colorPickerButton.setBackgroundColor(selectedColor)

        // Set up add category button
        val addButton = findViewById<Button>(R.id.btnAddCategory)
        val categoryNameInput = findViewById<TextInputEditText>(R.id.etCategoryName)

        addButton.setOnClickListener {
            val categoryName = categoryNameInput.text.toString().trim()

            if (categoryName.isEmpty()) {
                categoryNameInput.error = "Category name is required"
                return@setOnClickListener
            }

            val newCategory = Category(name = categoryName, color = selectedColor)

            // Add category to database
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    database.categoryDao().insertCategory(newCategory)
                }
                // Clear input field
                categoryNameInput.text?.clear()
                // Reset color
                selectedColor = Color.BLUE
                colorPickerButton.setBackgroundColor(selectedColor)

                Toast.makeText(this@CategoriesActivity, "Category added", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up back button
        val backButton = findViewById<Button>(R.id.btnBack)
        backButton.setOnClickListener {
            finish() // This will close the current activity and return to the previous one
        }

        // Load categories from database
        loadCategories()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            database.categoryDao().getAllCategories().collectLatest { categories ->
                adapter.updateCategories(categories)
            }
        }
    }

    private fun openColorPicker() {
        val colorPicker = AmbilWarnaDialog(this, selectedColor,
            object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onCancel(dialog: AmbilWarnaDialog) {
                    // Do nothing
                }

                override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                    selectedColor = color
                    findViewById<Button>(R.id.btnColorPicker).setBackgroundColor(color)
                }
            })
        colorPicker.show()
    }

    private fun editCategory(category: Category) {
        // Inflate the dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_category, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.etEditCategoryName)
        val colorButton = dialogView.findViewById<Button>(R.id.btnEditColorPicker)

        // Set initial values
        editText.setText(category.name)
        colorButton.setBackgroundColor(category.color)
        var newColor = category.color

        // Set up color picker for dialog
        colorButton.setOnClickListener {
            val colorPicker = AmbilWarnaDialog(this, newColor,
                object : AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onCancel(dialog: AmbilWarnaDialog) {
                        // Do nothing
                    }

                    override fun onOk(dialog: AmbilWarnaDialog, color: Int) {
                        newColor = color
                        colorButton.setBackgroundColor(color)
                    }
                })
            colorPicker.show()
        }

        // Create and show the dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Category")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updatedName = editText.text.toString().trim()

                if (updatedName.isNotEmpty()) {
                    val updatedCategory = category.copy(name = updatedName, color = newColor)

                    // Update category in database
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            database.categoryDao().updateCategory(updatedCategory)
                        }
                        Toast.makeText(this@CategoriesActivity, "Category updated", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun deleteCategory(category: Category) {
        // Show confirmation dialog
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete this category? All expenses in this category will also be deleted.")
            .setPositiveButton("Delete") { _, _ ->
                // Delete category from database
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        database.categoryDao().deleteCategory(category)
                    }
                    Toast.makeText(this@CategoriesActivity, "Category deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Adapter for the RecyclerView
    private class CategoryAdapter(
        private var categories: List<Category>,
        private val onEditClick: (Category) -> Unit,
        private val onDeleteClick: (Category) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        fun updateCategories(newCategories: List<Category>) {
            categories = newCategories
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category, parent, false)
            return CategoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val category = categories[position]
            holder.bind(category)
        }

        override fun getItemCount(): Int = categories.size

        inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameTextView: TextView = itemView.findViewById(R.id.tvCategoryName)
            private val colorView: View = itemView.findViewById(R.id.viewCategoryColor)
            private val editButton: ImageButton = itemView.findViewById(R.id.btnEditCategory)
            private val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteCategory)

            fun bind(category: Category) {
                nameTextView.text = category.name
                colorView.setBackgroundColor(category.color)

                editButton.setOnClickListener {
                    onEditClick(category)
                }

                deleteButton.setOnClickListener {
                    onDeleteClick(category)
                }
            }
        }
    }
}