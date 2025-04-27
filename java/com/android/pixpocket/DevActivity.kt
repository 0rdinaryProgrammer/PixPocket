package com.android.pixpocket

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class DevActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dev)

        // Initialize the HOME button
        val homeButton: Button = findViewById(R.id.home)

        // Set click listener for HOME button
        homeButton.setOnClickListener {
            // Create intent to go back to HomePageActivity
            val intent = Intent(this, HomePageActivity::class.java)

            // Clear the activity stack so user can't go back to profile with back button

            // Start the HomePageActivity
            startActivity(intent)

            // Finish current activity to remove it from stack
            finish()
        }
    }
}