package com.example.newprojectattendancessystem.screens

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.newprojectattendancessystem.databinding.ActivityRegisterBinding
import com.example.newprojectattendancessystem.local.AppDatabase
import com.example.newprojectattendancessystem.local.model.User
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var db: AppDatabase
    private var photoUri: Uri? = null

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required to capture a photo",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(applicationContext)

        binding.registerSubmitButton.isEnabled = false

        initViews()
    }

    private fun initViews() {
        binding.registerSubmitButton.setOnClickListener {
            val name = binding.nameInput.text.toString()
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            val confirmPassword = binding.confirmPasswordInput.text.toString()

            if (isValid(name, email, password, confirmPassword)) {
                saveUserToDatabase(name, email, password)
            }
        }

        // Capture photo button click listener
        binding.capturePhotoButton.setOnClickListener {
            checkCameraPermission()
        }
    }

    private fun saveUserToDatabase(name: String, email: String, password: String) {
        val user = User(
            name = name,
            email = email,
            password = password,
            photoUri = photoUri?.toString()
        )

        lifecycleScope.launch {
            db.userDao().insert(user)
            Toast.makeText(
                this@RegisterActivity,
                "User registered successfully",
                Toast.LENGTH_SHORT
            ).show()
            navigateToLogin()
        }
    }

    private fun checkCameraPermission() {
        when {
            android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M -> {
                openCamera()
            }
            android.content.pm.PackageManager.PERMISSION_GRANTED == checkSelfPermission(android.Manifest.permission.CAMERA) -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "CapturedPhoto")
            put(MediaStore.Images.Media.DESCRIPTION, "Photo from camera")
        }

        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        Log.d("Image", "Image URI for camera: $imageUri")

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        }

        photoUri = imageUri

        cameraResultLauncher.launch(takePictureIntent)
    }

    private val cameraResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d("Image", "Captured image URI: $photoUri")

                binding.capturedImage.setImageURI(photoUri)

                photoUri?.let {
                    detectFaceInImage(it)
                }

            } else {
                Toast.makeText(this, "Photo capture failed", Toast.LENGTH_SHORT).show()
            }
        }

    private fun detectFaceInImage(imageUri: Uri) {
        val image = InputImage.fromFilePath(applicationContext, imageUri)

        val detectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()

        val faceDetector = FaceDetection.getClient(detectorOptions)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    Log.d("FaceDetection", "Faces detected: ${faces.size}")
                    saveImageToDatabase(imageUri)
                } else {
                    Toast.makeText(this, "No face detected", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("FaceDetection", "Error in face detection", e)
                Toast.makeText(this, "Error detecting face", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveImageToDatabase(imageUri: Uri) {
        val user = User(
            name = binding.nameInput.text.toString(),
            email = binding.emailInput.text.toString(),
            password = binding.passwordInput.text.toString(),
            photoUri = imageUri.toString()
        )

        lifecycleScope.launch {
            db.userDao().insert(user)
            Toast.makeText(
                this@RegisterActivity,
                "User registered successfully with face detection",
                Toast.LENGTH_SHORT
            ).show()
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
        startActivity(intent)
    }

    private fun isValid(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        clearErrors()

        var isValid = true

        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Name is required"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.emailInputLayout.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Please enter a valid email"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.passwordInputLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.passwordInputLayout.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.passwordConfrimInputLayout.error = "Confirm Password is required"
            isValid = false
        } else if (confirmPassword != password) {
            binding.passwordConfrimInputLayout.error = "Passwords do not match"
            isValid = false
        }

        return isValid
    }

    private fun clearErrors() {
        binding.nameInputLayout.error = null
        binding.emailInputLayout.error = null
        binding.passwordInputLayout.error = null
        binding.passwordConfrimInputLayout.error = null
    }
}
