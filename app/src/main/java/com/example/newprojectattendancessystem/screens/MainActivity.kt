package com.example.newprojectattendancessystem.screens

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.newprojectattendancessystem.databinding.ActivityMainBinding
import com.example.newprojectattendancessystem.local.AppDatabase
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = AppDatabase.getInstance(applicationContext)

        initViews()
    }

    private fun initViews() {
        binding.login.setOnClickListener {
            val email = binding.nameInput.text.toString()
            val password = binding.passwordInput.text.toString()

            // Clear previous error messages
            clearErrors()

            // Validate credentials before proceeding
            if (isValidCredentials(email, password)) {
                checkUserCredentials(email, password)
            } else {
                Toast.makeText(this, "Please enter valid email and password", Toast.LENGTH_SHORT).show()
            }
        }

        binding.register.setOnClickListener {
            navigateToRegister()
        }
    }

    private fun navigateToRegister() {
        val intent = Intent(this@MainActivity, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun checkUserCredentials(email: String, password: String) {
        lifecycleScope.launch {
            val users = db.userDao().getAllUsers()
            var userFound = false

            for (user in users) {
                // Check if email and password match
                if (user.email == email && user.password == password) {
                    userFound = true
                    Toast.makeText(this@MainActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                    navigateToHome(email)
                    return@launch
                }
            }

            if (!userFound) {
                Toast.makeText(this@MainActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToHome(email: String) {
        val intent = Intent(this@MainActivity, DashboardActivity::class.java)
        intent.putExtra("EMAIL_CONSTANT", email)
        startActivity(intent)
    }

    // Validate email and password before database check
    private fun isValidCredentials(email: String, password: String): Boolean {
        var valid = true

        if (email.isEmpty() || !isValidEmail(email)) {
            binding.nameInputLayout.error = "Please enter a valid email address"
            valid = false
        }

        if (password.isEmpty() || password.length < 6) {
            binding.passwordInputLayout.error = "Password must be at least 6 characters"
            valid = false
        }

        return valid
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}"
        return email.matches(emailPattern.toRegex())
    }

    private fun clearErrors() {
        binding.nameInputLayout.error = null
        binding.passwordInputLayout.error = null
    }
}
