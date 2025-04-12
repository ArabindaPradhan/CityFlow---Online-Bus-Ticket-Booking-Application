package com.example.busconductorappv2

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class HistoryActivity : ComponentActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var recyclerView: RecyclerView
    private var busName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        firestore = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Get bus name from intent
        busName = intent.getStringExtra("BUS_NAME")?.replace("Bus: ", "")?.trim()

        if (busName.isNullOrEmpty()) {
            Log.e("HistoryActivity", "Bus name is missing!")
            Toast.makeText(this, "Bus Name not found", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("HistoryActivity", "Fetching history for bus: $busName")

        // Initialize adapter with an empty list
        historyAdapter = HistoryAdapter(mutableListOf())
        recyclerView.adapter = historyAdapter

        fetchHistory(busName!!)
    }

    private fun fetchHistory(busName: String) {
        firestore.collection("bookings")
            .whereEqualTo("busName", busName)
            .whereEqualTo("isScanned", true) // Ensure only scanned tickets are fetched
            .get()
            .addOnSuccessListener { documents ->
                val historyList = documents.mapNotNull { doc ->
                    doc.toObject(Ticket::class.java)
                }.toMutableList()

                Log.d("HistoryActivity", "Filtered History fetched: ${historyList.size} records")

                if (historyList.isEmpty()) {
                    Log.d("HistoryActivity", "No scanned tickets found.")
                    Toast.makeText(this, "No scanned tickets available.", Toast.LENGTH_SHORT).show()
                }

                // Update RecyclerView
                historyAdapter.updateData(historyList)
            }
            .addOnFailureListener { e ->
                Log.e("HistoryActivity", "Failed to load history: ${e.message}")
                Toast.makeText(this, "Failed to load history", Toast.LENGTH_SHORT).show()
            }

    }

}
