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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : Activity() {
    private val TAG = "LoginActivity"
    private lateinit var usernameOrEmailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

        try {
            setContentView(R.layout.activity_login)
            Log.d(TAG, "Layout set successfully")

            // Initialize Firebase Auth
            auth = Firebase.auth

            // Get references to views
            usernameOrEmailEditText = findViewById(R.id.usernameEditText)
            passwordEditText = findViewById(R.id.passwordEditText)
            val btnLogin = findViewById<Button>(R.id.btn_login)
            val btnRegister = findViewById<Button>(R.id.btn_register)

            // Set click listeners
            btnLogin.setOnClickListener {
                try {
                    val usernameOrEmail = usernameOrEmailEditText.text.toString().trim()
                    val password = passwordEditText.text.toString().trim()

                    // Input validation
                    if (usernameOrEmail.isEmpty()) {
                        usernameOrEmailEditText.error = "Username or email is required"
                        return@setOnClickListener
                    }

                    if (password.isEmpty()) {
                        passwordEditText.error = "Password is required"
                        return@setOnClickListener
                    }

                    // Determine if input is email or username
                    if (Patterns.EMAIL_ADDRESS.matcher(usernameOrEmail).matches()) {
                        // It's an email - login directly
                        loginWithEmail(usernameOrEmail, password)
                    } else {
                        // It's a username - look up the email first
                        lookupEmailByUsername(usernameOrEmail, password)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            btnRegister.setOnClickListener {
                Log.d(TAG, "Register button clicked")
                startActivityForResult(Intent(this, RegisterActivity::class.java), REGISTER_REQUEST_CODE)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during onCreate", e)
            Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun loginWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "signInWithEmail:success")
                    startActivity(Intent(this, HomePageActivity::class.java))
                    finish()
                } else {
                    // Handle specific errors
                    val errorMessage = when (task.exception?.message) {
                        "The password is invalid or the user does not have a password." ->
                            "Incorrect password"
                        "There is no user record corresponding to this identifier. The user may have been deleted." ->
                            "Account not found"
                        else -> "Authentication failed: ${task.exception?.message}"
                    }
                    passwordEditText.error = errorMessage
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun lookupEmailByUsername(username: String, password: String) {
        db.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    usernameOrEmailEditText.error = "Username not found"
                    Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val email = documents.documents[0].getString("email")
                if (email != null) {
                    loginWithEmail(email, password)
                } else {
                    Toast.makeText(this, "Error: No email associated with this username", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error looking up username: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REGISTER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.let {
                val username = it.getStringExtra("username") ?: ""
                val password = it.getStringExtra("password") ?: ""
                usernameOrEmailEditText.setText(username)
                passwordEditText.setText(password)
            }
        }
    }

    companion object {
        private const val REGISTER_REQUEST_CODE = 1001
    }
}