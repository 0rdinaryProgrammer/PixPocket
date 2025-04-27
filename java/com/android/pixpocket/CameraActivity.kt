package com.android.pixpocket

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.registerForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.android.pixpocket.databinding.ActivityCameraBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private val storageReference = FirebaseStorage.getInstance().reference
    private val auth: FirebaseAuth = Firebase.auth

    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var flashMode = ImageCapture.FLASH_MODE_OFF

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Permission request denied", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set white icons for flash and switch camera
        binding.flashButton.setColorFilter(Color.WHITE)
        binding.switchCameraButton.setColorFilter(Color.WHITE)
        binding.backButton.setColorFilter(Color.WHITE)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            activityResultLauncher.launch(Manifest.permission.CAMERA)
        }

        // Set up the listeners for buttons
        binding.captureButton.setOnClickListener {
            takePhoto()
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.switchCameraButton.setOnClickListener {
            switchCamera()
        }

        binding.flashButton.setOnClickListener {
            toggleFlash()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    private fun toggleFlash() {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> {
                binding.flashButton.setImageResource(R.drawable.ic_flash_on)
                ImageCapture.FLASH_MODE_ON
            }
            ImageCapture.FLASH_MODE_ON -> {
                binding.flashButton.setImageResource(R.drawable.ic_flash_auto)
                ImageCapture.FLASH_MODE_AUTO
            }
            else -> {
                binding.flashButton.setImageResource(R.drawable.ic_flash_off)
                ImageCapture.FLASH_MODE_OFF
            }
        }
        imageCapture?.flashMode = flashMode
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PixPocket")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    showError("Photo capture failed")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: run {
                        showError("Failed to save photo")
                        return
                    }

                    Log.d(TAG, "Photo capture succeeded: $savedUri")

                    // Show confirmation dialog
                    val builder = AlertDialog.Builder(this@CameraActivity)
                    builder.setMessage("Do you want to save this photo?")
                        .setCancelable(false)
                        .setPositiveButton("Save") { dialog, id ->
                            // Proceed with uploading image to Firebase
                            uploadImageToFirebase(savedUri)
                        }
                        .setNegativeButton("Discard") { dialog, id ->
                            // Optionally delete the saved file if discarded (delete from media store)
                            contentResolver.delete(savedUri, null, null)
                            Toast.makeText(this@CameraActivity, "Photo discarded", Toast.LENGTH_SHORT).show()
                        }
                    val alert = builder.create()
                    alert.show()
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flashMode)
                .build()

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                showError("Camera failed to start")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun uploadImageToFirebase(imageUri: Uri) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Please sign in to save photos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        runOnUiThread {
            binding.progressBar.visibility = View.VISIBLE
        }

        val timestamp = System.currentTimeMillis()
        val imagePath = "uploads/${user.uid}/${timestamp}_${UUID.randomUUID()}.jpg"
        val imageRef = storageReference.child(imagePath)

        // Create metadata with upload time
        val metadata = StorageMetadata.Builder()
            .setCustomMetadata("upload_time", timestamp.toString())
            .build()

        imageRef.putFile(imageUri, metadata)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    Log.d(TAG, "Image uploaded successfully: $downloadUri")
                    runOnUiThread {
                        Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show()
                        val resultIntent = Intent().apply {
                            data = downloadUri
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get download URL", e)
                    showError("Image saved but failed to get download URL")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Upload failed", exception)
                val errorMessage = when {
                    exception.message?.contains("permission", ignoreCase = true) == true ->
                        "Permission denied. Check your account settings."
                    exception.message?.contains("network", ignoreCase = true) == true ->
                        "Network error. Check your connection."
                    exception.message?.contains("quota", ignoreCase = true) == true ->
                        "Storage quota exceeded. Upgrade your plan."
                    else -> "Failed to save image. Please try again."
                }
                showError(errorMessage)
            }
            .addOnProgressListener { snapshot ->
                val progress = (100.0 * snapshot.bytesTransferred) / snapshot.totalByteCount
                runOnUiThread {
                    binding.progressBar.progress = progress.toInt()
                }
            }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val CAMERA_REQUEST_CODE = 1001
    }
}