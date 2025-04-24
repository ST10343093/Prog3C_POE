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

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        val emailEditText = findViewById<EditText>(R.id.etRegisterEmail)
        val passwordEditText = findViewById<EditText>(R.id.etRegisterPassword)
        val confirmPasswordEditText = findViewById<EditText>(R.id.etConfirmPassword)
        val registerButton = findViewById<Button>(R.id.btnRegister)
        val loginPrompt = findViewById<TextView>(R.id.tvLoginPrompt)

        // Set up register button
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            // Validate inputs
            if (email.isEmpty()) {
                emailEditText.error = "Email is required"
                emailEditText.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordEditText.error = "Password is required"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                passwordEditText.error = "Password should be at least 6 characters"
                passwordEditText.requestFocus()
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                confirmPasswordEditText.error = "Confirm password is required"
                confirmPasswordEditText.requestFocus()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                confirmPasswordEditText.error = "Passwords do not match"
                confirmPasswordEditText.requestFocus()
                return@setOnClickListener
            }

            // Register user
            registerUser(email, password)
        }

        // Set up login prompt
        loginPrompt.setOnClickListener {
            // Navigate to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign up success
                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()

                    // Navigate to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    // Determine the specific error
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

                    // Display appropriate error message
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

                    // Log the error for debugging
                    Log.e("RegisterActivity", "Registration failed", exception)
                }
            }
    }
}