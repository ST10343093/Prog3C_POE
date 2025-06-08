package vcmsa.projects.prog3c

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import vcmsa.projects.prog3c.utils.NavigationHelper

/**
 * Activity for new user registration
 * Handles creation of new accounts with email and password
 * Now includes smart navigation integration
 */
class RegisterActivity : AppCompatActivity() {

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    /**
     * Initialize the activity, set up UI components and validation logic with smart navigation
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize UI elements
        val emailEditText = findViewById<EditText>(R.id.etRegisterEmail)
        val passwordEditText = findViewById<EditText>(R.id.etRegisterPassword)
        val confirmPasswordEditText = findViewById<EditText>(R.id.etConfirmPassword)
        val registerButton = findViewById<Button>(R.id.btnRegister)
        val loginPrompt = findViewById<TextView>(R.id.tvLoginPrompt)

        // Set up register button with comprehensive input validation
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            // Validate email field
            if (email.isEmpty()) {
                emailEditText.error = "Email is required"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            // Validate password field
            if (password.isEmpty()) {
                passwordEditText.error = "Password is required"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            // Check password length requirement
            if (password.length < 6) {
                passwordEditText.error = "Password should be at least 6 characters"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            // Validate confirm password field
            if (confirmPassword.isEmpty()) {
                confirmPasswordEditText.error = "Confirm password is required"
                confirmPasswordEditText.requestFocus()
                return@setOnClickListener
            }

            // Check if passwords match
            if (password != confirmPassword) {
                confirmPasswordEditText.error = "Passwords do not match"
                confirmPasswordEditText.requestFocus()
                return@setOnClickListener
            }

            // Attempt to register user with validated inputs
            registerUser(email, password)
        }

        // Set up login prompt for existing users with smart navigation
        loginPrompt.setOnClickListener {
            NavigationHelper.navigateToActivity(this, LoginActivity::class.java)
            finish()
        }
    }

    /**
     * Creates a new user account with Firebase Authentication
     * Handles success and various error conditions with specific messages and smart navigation
     *
     * @param email New user's email address
     * @param password New user's password
     */
    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Account creation successful
                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()

                    // Navigate to MainActivity using smart navigation
                    NavigationHelper.navigateToAuthenticatedFlow(this)
                } else {
                    // Handle specific registration errors with helpful messages
                    val exception = task.exception
                    val errorMessage = when {
                        exception is FirebaseAuthUserCollisionException ->
                            "This email is already registered. Please use a different email or try logging in."
                        exception is FirebaseAuthWeakPasswordException ->
                            "Password is too weak. Please use a stronger password."
                        exception is FirebaseAuthInvalidCredentialsException ->
                            "Invalid email format. Please check your email address."
                        else -> "Registration failed: ${exception?.message}"
                    }

                    // Display appropriate error message to user
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

                    // Log error details for debugging
                    Log.e("RegisterActivity", "Registration failed", exception)
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