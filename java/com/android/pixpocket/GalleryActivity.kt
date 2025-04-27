package com.android.pixpocket

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.*

class GalleryActivity : Activity() {
    private lateinit var homeButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var storageReference: StorageReference
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val REQUEST_PERMISSION_CODE = 100
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        auth = FirebaseAuth.getInstance()
        storageReference = FirebaseStorage.getInstance().reference

        homeButton = findViewById(R.id.home)
        recyclerView = findViewById(R.id.gallery_recycler_view)

        homeButton.setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        recyclerView.layoutManager = GridLayoutManager(this, 3)

        if (checkGalleryPermission()) {
            loadGalleryImages()
        } else {
            requestGalleryPermission()
        }
    }

    private fun checkGalleryPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_PERMISSION_CODE)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadGalleryImages()
        } else {
            Toast.makeText(this, "Permission denied. Cannot access gallery.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadGalleryImages() {
        val images = mutableListOf<Uri>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                images.add(contentUri)
            }
        }

        imageAdapter = ImageAdapter(images) { uri ->
            showUploadConfirmationDialog(uri)
        }

        recyclerView.adapter = imageAdapter
    }

    private fun showUploadConfirmationDialog(imageUri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("Upload Image")
            .setMessage("Are you sure you want to upload this image?")
            .setPositiveButton("Upload") { _, _ ->
                uploadImageToFirebase(imageUri)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: "anonymous"
        val timestamp = System.currentTimeMillis()
        val imageRef = storageReference.child("uploads/$userId/${timestamp}_${UUID.randomUUID()}.jpg")

        // Create metadata with upload time
        val metadata = StorageMetadata.Builder()
            .setCustomMetadata("upload_time", timestamp.toString())
            .build()

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Uploading Image")
            .setMessage("Please wait...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        imageRef.putFile(imageUri, metadata)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { exception ->
                progressDialog.dismiss()
                Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}