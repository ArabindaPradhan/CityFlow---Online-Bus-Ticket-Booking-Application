package com.example.busconductorappv2

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var edtBusId: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var sharedPreferences: SharedPreferences
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // ✅ Initialize Views
        edtBusId = findViewById(R.id.edtBusId)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)

        // ✅ Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("BusLoginPrefs", MODE_PRIVATE)

        // ✅ Check if the bus is already logged in
        val savedBusId = sharedPreferences.getString("BUS_ID", null)
        if (savedBusId != null) {
            navigateToQRScanner(savedBusId)
            return
        }

        // ✅ Handle Login Button Click
        btnLogin.setOnClickListener {
            val busId = edtBusId.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (busId.isEmpty()) {
                Toast.makeText(this, "Please enter Bus ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authenticateBus(busId, password)
        }
    }

    override fun onStart() {
        super.onStart()
        val sharedPreferences = getSharedPreferences("BusLoginPrefs", MODE_PRIVATE)
        val savedBusId = sharedPreferences.getString("BUS_ID", null)

        if (savedBusId != null) {
            navigateToQRScanner(savedBusId)
        }
    }

    private fun authenticateBus(busId: String, password: String) {
        firestore.collection("buses").document(busId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val storedPassword = document.getString("password") ?: ""
                    val busName = document.getString("busName") ?: "Unknown Bus"

                    if (storedPassword == password) {
                        // ✅ Save Login State
                        saveLoginState(busId, busName)

                        Toast.makeText(this, "✅ Login Successful!", Toast.LENGTH_SHORT).show()
                        navigateToQRScanner(busName)
                    } else {
                        Toast.makeText(this, "❌ Incorrect Password!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "⚠ Bus ID not found!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Error connecting to Firestore!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveLoginState(busId: String, busName: String) {
        val editor = sharedPreferences.edit()
        editor.putString("BUS_ID", busId)
        editor.putString("BUS_NAME", busName)
        editor.apply()
    }

    private fun navigateToQRScanner(busName: String) {
        val intent = Intent(this, QRScannerActivity::class.java).apply {
            putExtra("BUS_NAME", busName)
        }
        startActivity(intent)
        finish()
    }
}
