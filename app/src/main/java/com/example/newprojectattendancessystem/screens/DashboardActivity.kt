package com.example.newprojectattendancessystem.screens

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.newprojectattendancessystem.databinding.ActivityDashboardBinding
import com.example.newprojectattendancessystem.local.AppDatabase
import com.example.newprojectattendancessystem.local.model.AttendanceRecord
import com.example.newprojectattendancessystem.local.model.User
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var db: AppDatabase
    private var photoUri: Uri? = null
    private lateinit var userEmail: String
    private var userId: Int = 0 // Variable to store the user ID from the database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getInstance(applicationContext)

        userEmail = intent.getStringExtra("EMAIL_CONSTANT") ?: ""
        Log.d("Dashboard", "Received email: $userEmail")

        initViews()
    }

    private fun initViews() {
        binding.userEmail.text = "Email: ${userEmail}"
        getAllData(userEmail)
        binding.attendanceImage.setOnClickListener {
            openCamera()
        }
        binding.logoutButton.setOnClickListener {
            navigateToMain()
        }
        binding.viewAttendanceButton.setOnClickListener {
            navigateToRecorded(userId)
        }

        binding.takeAttendanceButton.setOnClickListener {
            getUserImageFromDatabase(userEmail) { storedPhotoUri ->
                Log.d("TEST", "Stored Photo URI: $storedPhotoUri")
                Log.d("TEST", "Captured Photo URI: $photoUri")
                if (photoUri != null && storedPhotoUri != null) {
                    compareFaces(photoUri!!, storedPhotoUri)
                } else {
                    Toast.makeText(this, "Photo or stored image is missing.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Fetch user data from the database, including userId
    private fun getAllData(userEmail: String) {
        lifecycleScope.launch {
            val user = db.userDao().getUserByEmail(userEmail)
            if (user != null) {
                userId = user.id // Store the userId
                val name = user.name ?: "Unknown"
                val image = user.photoUri
                binding.userName.text = "Name: ${name}"
                binding.welcomeMessage.text = "Welcome, ${name}!"
                binding.attendanceImage2.setImageURI(Uri.parse(image))
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this@DashboardActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
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

    // Camera result launcher
    private val cameraResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d("Image", "Captured image URI: $photoUri")

                binding.attendanceImage.setImageURI(photoUri)
            } else {
                Toast.makeText(this, "Photo capture failed", Toast.LENGTH_SHORT).show()
            }
        }

    private fun getUserImageFromDatabase(email: String, callback: (Uri?) -> Unit) {
        lifecycleScope.launch {
            val user: User? = db.userDao().getUserByEmail(email)
            val storedImageUri = Uri.parse(user?.photoUri)
            Log.d("Database", "Stored Image URI from DB: $storedImageUri")
            callback(storedImageUri)
        }
    }

    // Resize image to ensure consistent scale
    private fun resizeImage(imageUri: Uri): InputImage? {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 640, 480, false)
            return InputImage.fromBitmap(scaledBitmap, 0)
        } catch (e: Exception) {
            Log.e("ImageResize", "Error resizing image", e)
            return null
        }
    }

    // Detect face from image and return list of faces
    private fun detectFaceFromImage(imageUri: Uri, callback: (List<Face>?) -> Unit) {
        val image = resizeImage(imageUri) ?: return
        val detectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        val faceDetector: FaceDetector = FaceDetection.getClient(detectorOptions)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                callback(faces)
            }
            .addOnFailureListener { e ->
                Log.e("FaceDetection", "Error in face detection", e)
                callback(null)
            }
    }

    private fun compareFaces(capturedUri: Uri, storedUri: Uri) {
        detectFaceFromImage(capturedUri) { capturedFaces ->
            detectFaceFromImage(storedUri) { storedFaces ->
                if (capturedFaces == null || storedFaces == null || capturedFaces.size != 1 || storedFaces.size != 1) {
                    Toast.makeText(this, "Face not detected in one or both images.", Toast.LENGTH_SHORT).show()
                    return@detectFaceFromImage
                }

                val capturedFace = capturedFaces[0]
                val storedFace = storedFaces[0]

                val capturedEyeLeft = capturedFace.getLandmark(FaceLandmark.LEFT_EYE)
                val capturedEyeRight = capturedFace.getLandmark(FaceLandmark.RIGHT_EYE)
                val capturedMouth = capturedFace.getLandmark(FaceLandmark.MOUTH_BOTTOM)
                val capturedNose = capturedFace.getLandmark(FaceLandmark.NOSE_BASE)

                val storedEyeLeft = storedFace.getLandmark(FaceLandmark.LEFT_EYE)
                val storedEyeRight = storedFace.getLandmark(FaceLandmark.RIGHT_EYE)
                val storedMouth = storedFace.getLandmark(FaceLandmark.MOUTH_BOTTOM)
                val storedNose = storedFace.getLandmark(FaceLandmark.NOSE_BASE)

                if (capturedEyeLeft != null && capturedEyeRight != null && capturedMouth != null && capturedNose != null &&
                    storedEyeLeft != null && storedEyeRight != null && storedMouth != null && storedNose != null) {

                    val eyeLeftDistance = calculateDistance(capturedEyeLeft.position, storedEyeLeft.position)
                    val eyeRightDistance = calculateDistance(capturedEyeRight.position, storedEyeRight.position)
                    val mouthDistance = calculateDistance(capturedMouth.position, storedMouth.position)
                    val noseDistance = calculateDistance(capturedNose.position, storedNose.position)

                    val threshold = 30f

                    if (eyeLeftDistance < threshold && eyeRightDistance < threshold && mouthDistance < threshold && noseDistance < threshold) {
                        Toast.makeText(this, "Faces match!", Toast.LENGTH_SHORT).show()
                        saveOnDataBase(userId)
                    } else {
                        Toast.makeText(this, "Faces do not match.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Landmarks not detected properly.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveOnDataBase(userId: Int) {
        val currentTime = getCurrentTime()
        val attendanceMessage = "Your attendance has been recorded"

        val attendanceRecord = AttendanceRecord(
            userId = userId,
            message = attendanceMessage,
            time = currentTime
        )

        lifecycleScope.launch {
            db.attendanceRecordDao().insert(attendanceRecord)
            Toast.makeText(this@DashboardActivity, "Attendance Recorded", Toast.LENGTH_SHORT).show()
            navigateToRecorded(userId)
        }
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun navigateToRecorded(userId:Int) {
        val intent = Intent(this@DashboardActivity, RecordedAttendanceActivity::class.java)
        intent.putExtra("USER_ID",userId)
        startActivity(intent)
    }

    private fun calculateDistance(point1: PointF, point2: PointF): Float {
        val dx = point1.x - point2.x
        val dy = point1.y - point2.y
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
