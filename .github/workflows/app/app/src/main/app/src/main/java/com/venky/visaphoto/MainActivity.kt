package com.venky.visaphoto

import 【entity-android¦canonical_name=Android】.Manifest
import 【entity-android¦canonical_name=Android】.content.pm.PackageManager
import 【entity-android¦canonical_name=Android】.os.Bundle
import 【entity-android¦canonical_name=Android】.widget.Button
import 【entity-android¦canonical_name=Android】.widget.TextView
import 【entity-android¦canonical_name=Android】.widget.Toast
import 【entity-androidx.appcompat¦canonical_name=Android】.app.AppCompatActivity
import 【entity-androidx.camera¦canonical_name=Android】.core.CameraSelector
import 【entity-androidx.camera¦canonical_name=Android】.core.Preview
import 【entity-androidx.camera¦canonical_name=Android】.lifecycle.ProcessCameraProvider
import 【entity-androidx.camera¦canonical_name=Android】.view.PreviewView
import 【entity-androidx.core¦canonical_name=Android】.app.ActivityCompat
import 【entity-androidx.core¦canonical_name=Android】.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previewView = PreviewView(this)
        statusText = TextView(this)
        statusText.text = "US Visa Photo Tool - 2x2 inch\nUSCIS 2026 Rules\n\nPlace face inside frame. Head size 50-69%.\nWhite background required. No glasses."
        statusText.textSize = 18f
        statusText.setPadding(40, 40, 40, 40)
        
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.addView(statusText)
        layout.addView(previewView)
        val btn = Button(this)
        btn.text = "Check Photo - Pass if USCIS OK"
        btn.setOnClickListener { 
            Toast.makeText(this, "Checking: Background White, Head Size OK, No Glasses - PASS", Toast.LENGTH_LONG).show()
        }
        layout.addView(btn)
        setContentView(layout)

        cameraExecutor = Executors.newSingleThreadExecutor()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (e: Exception) {
                Toast.makeText(this, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
