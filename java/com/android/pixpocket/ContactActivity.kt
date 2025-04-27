package com.android.pixpocket

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView

class ContactActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        val listView: ListView = findViewById(R.id.Listview)

        val contactList = listOf(
            "John Doe", "Jane Smith", "Michael Brown", "Emily Johnson", "Chris Wilson",
            "Jessica Davis", "Daniel Martinez", "Laura Garcia", "James Anderson", "Sophia Lee"
        )

        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, contactList)
        listView.adapter = arrayAdapter


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
