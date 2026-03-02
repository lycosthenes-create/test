package com.example.foodtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.foodtracker.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private var latestCapturedFile: File? = null
    private val openAiClient = OpenAiClient()

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.captureButton.setOnClickListener { takePhoto() }
        binding.analyzeButton.setOnClickListener { analyzeLatestImage() }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val outputFile = File(cacheDir, "food-${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    latestCapturedFile = outputFile
                    binding.resultText.text = "Photo captured. Tap Analyze Food to send to ChatGPT."
                }

                override fun onError(exception: ImageCaptureException) {
                    binding.resultText.text = "Capture failed: ${exception.message}"
                }
            }
        )
    }

    private fun analyzeLatestImage() {
        val file = latestCapturedFile
        if (file == null || !file.exists()) {
            binding.resultText.text = "Please capture a photo first."
            return
        }

        val apiKey = binding.apiKeyInput.text?.toString().orEmpty().trim()
        if (apiKey.isBlank()) {
            binding.resultText.text = "Please enter your OpenAI API key."
            return
        }

        lifecycleScope.launch {
            binding.resultText.text = "Analyzing..."
            val result = withContext(Dispatchers.IO) {
                val imageBytes = file.readBytes()
                val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                openAiClient.analyzeFood(base64, apiKey)
            }

            result.onSuccess { response ->
                binding.resultText.text = response
            }.onFailure { error ->
                binding.resultText.text = "Analysis failed: ${error.message}"
            }
        }
    }
}
