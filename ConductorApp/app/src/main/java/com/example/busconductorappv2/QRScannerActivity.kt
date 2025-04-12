package com.example.busconductorappv2

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.busconductorappv2.databinding.ActivityQrscannerBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRScannerActivity : ComponentActivity() {

    private lateinit var binding: ActivityQrscannerBinding
    private lateinit var cameraExecutor: ExecutorService
    private val firestore = FirebaseFirestore.getInstance()
    private var lastScannedCode: String? = null
    private var lastScanTime: Long = 0
    private lateinit var txtBusName: TextView
    private lateinit var btnLogout: Button
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var btnHistory: Button


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrscannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Initialize Views
        txtBusName = findViewById(R.id.txtBusName)
        btnLogout = findViewById(R.id.btnLogout)

        btnHistory = findViewById(R.id.btnHistory)
        sharedPreferences = getSharedPreferences("BusLoginPrefs", MODE_PRIVATE)

        // ✅ Set Bus Name
        val busName = intent.getStringExtra("BUS_NAME") ?: "Unknown Bus"
        txtBusName.text = "Bus: $busName"

        // ✅ Handle Logout Button Click
        btnLogout.setOnClickListener {
            logout()
        }
        // ✅ Open History Activity
        btnHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            intent.putExtra("BUS_NAME", busName)
            startActivity(intent)
        }
        // ✅ Request Camera Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun logout() {
        // ✅ Clear SharedPreferences to remove saved login info
        sharedPreferences.edit().clear().apply()

        // ✅ Navigate to LoginActivity after logout
        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 500)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera binding failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                val scanner = BarcodeScanning.getClient()

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { scannedData ->
                                handleScannedData(scannedData)
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "QR scan failed", Toast.LENGTH_SHORT).show()
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        } catch (e: Exception) {
            imageProxy.close()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleScannedData(scannedData: String) {
        val currentTime = System.currentTimeMillis()
        Log.d("QRScanner", "Scanned Data: $scannedData")

        // ✅ Avoid duplicate scanning within 3 seconds
        if (scannedData == lastScannedCode && (currentTime - lastScanTime < 3000)) {
            return
        }

        lastScannedCode = scannedData
        lastScanTime = currentTime

        playBeepSound()
        vibrateDevice()

        try {
            // ✅ Parse QR code data
            val jsonData = JSONObject(scannedData.trim())
            val docId = jsonData.optString("docId", "").trim()
            val qrBusName = jsonData.optString("busName", "").trim()
            val fromTo = jsonData.optString("fromTo", "Unknown Route")
            val passengerCount = jsonData.optInt("passengerCount", 1)
            val totalPrice = jsonData.optInt("totalPrice", 0)

            // ✅ Get the logged-in bus name
            val loggedBusName = intent.getStringExtra("BUS_NAME") ?: "Unknown Bus"

            // ✅ Format QR bus name by adding "Bus" prefix
            val formattedQrBusName = "Bus$qrBusName" // "Bus18\nrouteName" or "Bus18AC\nrouteName"

            // ✅ Verify ticket bus name
            if (!formattedQrBusName.contains(loggedBusName)) {
                showPopupMessage("Invalid Ticket!", "This ticket is for $formattedQrBusName, not $loggedBusName")
                return
            }

            if (!loggedBusName.contains("AC") && qrBusName.contains("AC")) {
                showPopupMessage("Invalid Ticket!", "AC and non-AC buses are different.")
                return
            }

            // ✅ Proceed with Firestore verification
            val bookingRef = firestore.collection("bookings").document(docId)
            bookingRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val isScanned = document.getBoolean("isScanned") ?: false

                    if (isScanned) {
                        showPopupMessage("Ticket Already Scanned!", "This ticket has already been used.")
                    } else {
                        // ✅ Update Firestore to mark ticket as scanned
                        bookingRef.update("isScanned", true)
                            .addOnSuccessListener {
                                // ✅ Show ticket details in popup
                                showTicketDetails(
                                    busName = qrBusName,
                                    fromTo = fromTo,
                                    passengerCount = passengerCount,
                                    totalPrice = totalPrice
                                )
                            }
                            .addOnFailureListener {
                                showPopupMessage("Error!", "Failed to update ticket status.")
                            }
                    }
                } else {
                    showPopupMessage("Invalid Ticket!", "No record found for this ticket.")
                }
            }.addOnFailureListener {
                showPopupMessage("Error!", "Error fetching ticket details.")
            }

        } catch (e: Exception) {
            showPopupMessage("Error!", "Invalid QR Code Format")
        }
    }

    private fun showTicketDetails(busName: String, fromTo: String, passengerCount: Int, totalPrice: Int) {
        val dialogView = layoutInflater.inflate(R.layout.popup_ticket_details, null)
        val txtBusName = dialogView.findViewById<TextView>(R.id.txtBusName)
        val txtRoute = dialogView.findViewById<TextView>(R.id.txtRoute)
        val txtPassengers = dialogView.findViewById<TextView>(R.id.txtPassengers)
        val txtTotalPrice = dialogView.findViewById<TextView>(R.id.txtTotalPrice)
        val btnClosePopup = dialogView.findViewById<Button>(R.id.btnClosePopup)

        txtBusName.text = "Bus: $busName"
        txtRoute.text = "Route: $fromTo"
        txtPassengers.text = "Passengers: $passengerCount"
        txtTotalPrice.text = "Total Price: ₹$totalPrice"

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnClosePopup.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun showPopupMessage(title: String, message: String) {
        val dialogView = layoutInflater.inflate(R.layout.popup_ticket_info, null)
        val txtPopupMessage = dialogView.findViewById<TextView>(R.id.txtPopupMessage)
        val txtTicketDetails = dialogView.findViewById<TextView>(R.id.txtTicketDetails)
        val btnClosePopup = dialogView.findViewById<Button>(R.id.btnClosePopup)

        txtPopupMessage.text = title
        txtTicketDetails.text = message

        // Show ticket details only if it's valid
        if (!message.contains("Invalid") && !message.contains("Error")) {
            txtTicketDetails.visibility = View.VISIBLE
        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnClosePopup.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun playBeepSound() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, R.raw.beep)
        mediaPlayer?.start()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrateDevice() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        mediaPlayer?.release()
    }
}