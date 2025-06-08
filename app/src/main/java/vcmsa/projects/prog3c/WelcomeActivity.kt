package vcmsa.projects.prog3c

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import vcmsa.projects.prog3c.utils.NavigationHelper

/**
 * Entry point activity for the application
 * Displays welcome screen with options to sign in or register
 * Now includes smart navigation and authentication checking
 */
class WelcomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    /**
     * Initializes the activity and sets up navigation buttons with smart navigation
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome_page)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is already authenticated
        checkAuthenticationState()

        // Get references to UI buttons
        val loginButton = findViewById<Button>(R.id.btnSignIn)
        val signUpButton = findViewById<Button>(R.id.btnSignUp)

        // Configure sign in button to navigate to login screen with smart navigation
        loginButton.setOnClickListener {
            NavigationHelper.navigateToActivity(this, LoginActivity::class.java)
        }

        // Configure sign up button to navigate to registration screen with smart navigation
        signUpButton.setOnClickListener {
            NavigationHelper.navigateToActivity(this, RegisterActivity::class.java)
        }
    }

    /**
     * Check if user is already authenticated and redirect accordingly
     */
    private fun checkAuthenticationState() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is already logged in, navigate to main activity
            NavigationHelper.navigateToAuthenticatedFlow(this)
        }
    }

    /**
     * Handle back button press - exit app from welcome screen
     */
    override fun onBackPressed() {
        finishAffinity() // Close the entire app
    }

    /**
     * Check authentication state when returning to this activity
     */
    override fun onResume() {
        super.onResume()
        checkAuthenticationState()
    }
}