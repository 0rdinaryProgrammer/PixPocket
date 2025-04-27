package com.android.pixpocket

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ListView

class FavActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        val listView: ListView = findViewById(R.id.Listview)
        val homeButton: Button = findViewById(R.id.home) // ✅ Corrected placement

        // Sample contact data (Names + Image IDs)
        val contacts = listOf(
            Contact("John Doe", R.drawable.ic_contact1),
            Contact("Jane Smith", R.drawable.ic_contact2),
            Contact("Michael Brown", R.drawable.ic_contact3 ),
            Contact("Emily Johnson", R.drawable.ic_contact4),
            Contact("Chris Wilson", R.drawable.ic_contact5)
        )

        val adapter = FavAdapter(this, contacts)
        listView.adapter = adapter

        // ✅ Redirect to HomePageActivity
        homeButton.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finish() // Optional: Closes FavActivity so the user can't go back using the back button
        }
    }
}
