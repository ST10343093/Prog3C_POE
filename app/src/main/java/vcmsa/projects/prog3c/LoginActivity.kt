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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

/**
 * Activity for user authentication
 * Handles user login and provides navigation to registration screen
 */
class LoginActivity : AppCompatActivity() {

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    /**
     * Initializes the activity, sets up UI components and event listeners
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        val emailEditText = findViewById<EditText>(R.id.etLoginEmail)
        val passwordEditText = findViewById<EditText>(R.id.etLoginPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerPrompt = findViewById<TextView>(R.id.tvRegisterPrompt)

        // Set up login button with validation logic
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Validate email input
            if (email.isEmpty()) {
                emailEditText.error = "Email is required"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            // Validate password input
            if (password.isEmpty()) {
                passwordEditText.error = "Password is required"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            // Attempt login with validated inputs
            loginUser(email, password)
        }

        // Set up register prompt to navigate to registration screen
        registerPrompt.setOnClickListener {
            // Navigate to RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Authenticates user with Firebase using email and password
     * Handles login success and various error conditions
     *
     * @param email User's email address
     * @param password User's password
     */
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Authentication successful, show success message
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                    // Navigate to MainActivity and clear back stack
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // Determine the specific authentication error
                    val exception = task.exception
                    val errorMessage = when {
                        exception is FirebaseAuthInvalidUserException ->
                            "No account exists with this email. Please register first."
                        exception is FirebaseAuthInvalidCredentialsException ->
                            "Incorrect email or password. Please try again."
                        else -> "Login failed: ${exception?.message}"
                    }

                    // Display appropriate error message to user
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

                    // Log the error for debugging purposes
                    Log.e("LoginActivity", "Login failed", exception)
                }
            }
    }
}