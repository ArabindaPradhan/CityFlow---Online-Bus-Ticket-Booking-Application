package com.example.test

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject

class QRGeneratorActivity : AppCompatActivity() {

    private lateinit var imageViewQR: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_generator)

        imageViewQR = findViewById(R.id.imageViewQR)

        // ✅ Retrieve booking details from Intent (passed from BookingAdapter)
        val bookingId = intent.getStringExtra("bookingId")
        val busName = intent.getStringExtra("busName") ?: ""
        val fromTo = intent.getStringExtra("fromTo") ?: ""
        val passengerCount = intent.getIntExtra("passengerCount", 0)
        val totalPrice = intent.getIntExtra("totalPrice", 0)
        val userId = intent.getStringExtra("userId") ?: ""

        if (bookingId.isNullOrEmpty()) {
            Log.e("QRGenerator", "Invalid or missing bookingId!")
            return
        }

        Log.d("QRGenerator", "Generating QR for bookingId: $bookingId")

        // ✅ Generate QR Code for clicked booking
        generateQRCodeForBooking(bookingId, busName, fromTo, passengerCount, totalPrice)
    }

    private fun generateQRCodeForBooking(
        bookingId: String,
        busName: String,
        fromTo: String,
        passengerCount: Int,
        totalPrice: Int
    ) {
        // ✅ Create QR Code Data with `bookingId`
        val qrData = JSONObject().apply {
            put("docId", bookingId) // ✅ Firestore document ID
            put("busName", busName)
            put("fromTo", fromTo)
            put("passengerCount", passengerCount)
            put("totalPrice", totalPrice)
        }.toString()

        // ✅ Generate and display the QR code
        generateQRCode(qrData)?.let { qrBitmap ->
            imageViewQR.setImageBitmap(qrBitmap)
        }
    }

    private fun generateQRCode(data: String): Bitmap? {
        val writer = QRCodeWriter()
        return try {
            val bitMatrix: BitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 600, 600)
            val width = bitMatrix.width
            val height = bitMatrix.height
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
            }
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }
}
