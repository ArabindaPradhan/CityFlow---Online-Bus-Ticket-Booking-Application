package com.example.test

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test.databinding.FragmentHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class History : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var bookingAdapter: BookingAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val handler = Handler(Looper.getMainLooper())  // Handler for refreshing

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize RecyclerView
        binding.recyclerViewHistory.layoutManager = LinearLayoutManager(requireContext())

        // Start auto-refreshing every 1 second
        startAutoRefresh()

        return root
    }

    private fun loadBookingHistory() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val bookings = mutableListOf<BookingDetails>()
                for (document in documents) {
                    val booking = document.toObject(BookingDetails::class.java).copy(
                        isScanned = document.getBoolean("isScanned") ?: false
                    )
                    bookings.add(booking)
                }

                if (bookings.isEmpty()) {
                    binding.textHistory.text = "No bookings found"
                    binding.recyclerViewHistory.visibility = View.GONE
                    binding.textHistory.visibility = View.VISIBLE
                } else {
                    binding.textHistory.visibility = View.GONE
                    binding.recyclerViewHistory.visibility = View.VISIBLE
                    bookingAdapter = BookingAdapter(bookings) { selectedBooking ->
                        if (!selectedBooking.isScanned) {
                            Toast.makeText(requireContext(), "Booking Clicked: ${selectedBooking.busName}", Toast.LENGTH_SHORT).show()
                            // Handle click (e.g., show booking details)
                        }
                    }
                    binding.recyclerViewHistory.adapter = bookingAdapter
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load booking history!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startAutoRefresh() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isAdded) { // ✅ Prevents crashes if fragment is not visible
                    loadBookingHistory()
//                    handler.postDelayed(this, 10000)
                }
            }
        }, 100)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        handler.removeCallbacksAndMessages(null)  // Stop auto-refreshing when fragment is destroyed
    }
}
