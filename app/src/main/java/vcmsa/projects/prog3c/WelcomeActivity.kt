package vcmsa.projects.prog3c

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Entry point activity for the application
 * Displays welcome screen with options to sign in or register
 */
class WelcomeActivity : AppCompatActivity() {

    /**
     * Initializes the activity and sets up navigation buttons
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome_page)

        // Get references to UI buttons
        val loginButton = findViewById<Button>(R.id.btnSignIn)
        val signUpButton = findViewById<Button>(R.id.btnSignUp)

        // Configure sign in button to navigate to login screen
        loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Configure sign up button to navigate to registration screen
        signUpButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}