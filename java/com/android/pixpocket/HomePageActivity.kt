package com.android.pixpocket

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class HomePageActivity : Activity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuButton: ImageView
    private lateinit var turnCamButton: ImageView
    private lateinit var removePhotoButton: ImageView
    private lateinit var cloudText: TextView
    private lateinit var cloudImageView: ImageView
    private lateinit var navigationView: NavigationView
    private lateinit var JoinButton: Button
    private lateinit var GroupChatLogo: ImageView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val CAMERA_REQUEST_CODE = 1001
    private val GALLERY_REQUEST_CODE = 1002
    private lateinit var sharedPreferences: SharedPreferences

    // Firebase variables
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        // Initialize Firebase
        firebaseStorage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout)
        menuButton = findViewById(R.id.menu_button)
        turnCamButton = findViewById(R.id.end_logo_2)
        removePhotoButton = findViewById(R.id.removePhoto)
        cloudText = findViewById(R.id.cloud_text)
        cloudImageView = findViewById(R.id.cloud_image_view)
        navigationView = findViewById(R.id.nav_view)
        JoinButton = findViewById(R.id.add_friends_button)
        GroupChatLogo = findViewById(R.id.end_logo)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        sharedPreferences = getSharedPreferences("PixPocketPrefs", MODE_PRIVATE)

        // Load the most recent image from Firebase
        loadMostRecentImage()

        // Set up swipe to refresh
        swipeRefreshLayout.setOnRefreshListener {
            loadMostRecentImage()
        }

        GroupChatLogo.setOnClickListener {
            val intent = Intent(this, GroupChatActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Set up menu button
        menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Open camera
        turnCamButton.setOnClickListener {
            startActivityForResult(Intent(this, CameraActivity::class.java), CAMERA_REQUEST_CODE)
        }

        // Remove photo with confirmation
        removePhotoButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Remove Photo")
                .setMessage("Are you sure you want to remove this photo?")
                .setPositiveButton("Yes") { _, _ ->
                    clearPhoto()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        // Navigation drawer actions
        navigationView.setNavigationItemSelectedListener { menuItem ->
            val intent = when (menuItem.itemId) {
                R.id.Profile -> Intent(this, ProfileActivity::class.java)
                R.id.Dev -> Intent(this, DevActivity::class.java)
                R.id.Settings -> Intent(this, SettingsActivity::class.java)
                R.id.LogOut -> {
                    AlertDialog.Builder(this)
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out?")
                        .setPositiveButton("Yes") { _, _ ->
                            auth.signOut()
                            startActivity(Intent(this, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                        }
                        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                        .show()
                    return@setNavigationItemSelectedListener true
                }
                R.id.Gallery -> {
                    val galleryIntent = Intent(this, GalleryActivity::class.java)
                    startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
                    null
                }
                R.id.More -> Intent(this, MoreActivity::class.java)
                else -> null
            }
            intent?.let { startActivity(it) }
            true
        }
    }

    private fun loadMostRecentImage() {
        val user = auth.currentUser
        if (user == null) {
            swipeRefreshLayout.isRefreshing = false
            return
        }

        val storageRef = firebaseStorage.reference.child("uploads/${user.uid}")

        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isEmpty()) {
                    showNoImages()
                    return@addOnSuccessListener
                }

                // Get metadata for all images to find the most recent one
                val imageRefsWithTime = mutableListOf<Pair<StorageReference, Long>>()
                val countDownLatch = java.util.concurrent.CountDownLatch(listResult.items.size)

                listResult.items.forEach { imageRef ->
                    imageRef.metadata.addOnSuccessListener { metadata ->
                        val updatedTime = metadata.updatedTimeMillis
                        imageRefsWithTime.add(Pair(imageRef, updatedTime))
                        countDownLatch.countDown()
                    }.addOnFailureListener {
                        countDownLatch.countDown()
                    }
                }

                // Wait for all metadata requests to complete
                Thread {
                    countDownLatch.await()
                    runOnUiThread {
                        if (imageRefsWithTime.isNotEmpty()) {
                            // Sort by timestamp (newest first)
                            val mostRecent = imageRefsWithTime.maxByOrNull { it.second }
                            mostRecent?.first?.downloadUrl?.addOnSuccessListener { uri ->
                                displayPhoto(uri)
                                swipeRefreshLayout.isRefreshing = false
                            }?.addOnFailureListener {
                                showNoImages()
                            }
                        } else {
                            showNoImages()
                        }
                    }
                }.start()
            }
            .addOnFailureListener {
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this, "Failed to load images", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showNoImages() {
        cloudText.visibility = View.VISIBLE
        cloudImageView.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
    }

    private fun displayPhoto(uri: Uri) {
        cloudText.visibility = View.GONE
        cloudImageView.visibility = View.VISIBLE

        Glide.with(this)
            .load(uri)
            .into(cloudImageView)

        sharedPreferences.edit().putString("photoUri", uri.toString()).apply()
    }

    private fun clearPhoto() {
        cloudImageView.setImageURI(null)
        cloudImageView.visibility = View.GONE
        cloudText.visibility = View.VISIBLE
        sharedPreferences.edit().remove("photoUri").apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    data?.data?.let { uri ->
                        displayPhoto(uri)
                        uploadImageToFirebase(uri)
                    }
                }
            }
            GALLERY_REQUEST_CODE -> {
                loadMostRecentImage()
            }
        }
    }

    private fun uploadImageToFirebase(fileUri: Uri) {
        val user = auth.currentUser ?: return
        val storageRef = firebaseStorage.reference
        val timestamp = System.currentTimeMillis()
        val filename = "${timestamp}_${UUID.randomUUID()}"
        val imagesRef = storageRef.child("uploads/${user.uid}/$filename")

        // Include upload time in metadata
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setCustomMetadata("upload_time", timestamp.toString())
            .build()

        imagesRef.putFile(fileUri, metadata)
            .addOnSuccessListener {
                // Force refresh with a slight delay to ensure Firebase updates
                Handler(Looper.getMainLooper()).postDelayed({
                    loadMostRecentImage()
                }, 1000)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        loadMostRecentImage()
    }
}