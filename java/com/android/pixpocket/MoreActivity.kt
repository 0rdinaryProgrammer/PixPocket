package com.android.pixpocket

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView

class MoreActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more) // Ensure this matches your XML file

        // Find ImageViews by ID
        val contactsImageView = findViewById<ImageView>(R.id.imageView)
        val favoritesImageView = findViewById<ImageView>(R.id.imageView2)

        // Set click listener for Contacts Image
        contactsImageView.setOnClickListener {
            val intent = Intent(this, ContactActivity::class.java)
            startActivity(intent)
        }

        // Set click listener for Favorites Image
        favoritesImageView.setOnClickListener {
            val intent = Intent(this, FavActivity::class.java)
            startActivity(intent)
        }
    }
}
