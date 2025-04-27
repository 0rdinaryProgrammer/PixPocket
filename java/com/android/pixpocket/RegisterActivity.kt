package com.android.pixpocket

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterActivity : Activity() {
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth
        auth = Firebase.auth

        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        emailEditText = findViewById(R.id.emailEditText)
        registerButton = findViewById(R.id.btn_register)

        registerButton.setOnClickListener {
            if (validateInputs()) {
                registerUser()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()

        // Username validation
        if (username.isEmpty()) {
            usernameEditText.error = "Username is required"
            return false
        }
        if (username.length < 4) {
            usernameEditText.error = "Username must be at least 4 characters"
            return false
        }

        // Email validation
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Please enter a valid email"
            return false
        }

        // Password validation
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            return false
        }
        if (password.length < 6) {
            passwordEditText.error = "Password must be at least 6 characters"
            return false
        }

        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            confirmPasswordEditText.error = "Please confirm your password"
            return false
        }
        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Passwords do not match"
            return false
        }

        return true
    }

    private fun registerUser() {
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign up success
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser

                    Toast.makeText(
                        this, "Registration successful!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Return to login with autofill data
                    val resultIntent = Intent()
                    resultIntent.putExtra("username", email) // Using email as username for Firebase
                    resultIntent.putExtra("password", password)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } else {
                    // If sign up fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        this, "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    companion object {
        private const val TAG = "RegisterActivity"
    }
}