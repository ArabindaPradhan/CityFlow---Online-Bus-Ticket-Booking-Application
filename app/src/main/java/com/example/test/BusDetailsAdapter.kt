package com.example.test

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BusDetailsAdapter(context: Context, busList: List<BusDetails>) :
    ArrayAdapter<BusDetails>(context, R.layout.item_bus, busList.filter { it.busName != "Unknown" && it.busName != "No Bus Available" }) {

    private val filteredBusList = busList.filter { it.busName != "Unknown" && it.busName != "No Bus Available" }
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = filteredBusList.size

    override fun getItem(position: Int): BusDetails? = filteredBusList[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_bus_details, parent, false)
        val bus = filteredBusList[position]

        val textBusName = view.findViewById<TextView>(R.id.textBusName)
        val textBusRoute = view.findViewById<TextView>(R.id.textRouteName)
        val textEstimatedTime = view.findViewById<TextView>(R.id.textEstimatedTime)
        val textWaitingTime = view.findViewById<TextView>(R.id.textWaitingTime)
        val buttonSelectBus = view.findViewById<Button>(R.id.buttonSelectBus)

        textBusName.text = "Bus: ${bus.busName}"
        textBusRoute.text = "Route: ${bus.routeName}"
        textEstimatedTime.text = "Travel Time: ${bus.estimatedTime}"

        val waitingTimeText = formatWaitingTime(bus.waitingTime)
        textWaitingTime.text = "Next Bus In: $waitingTimeText"

        if (bus.waitingTime.matches(Regex("\\d{1,2}:\\d{2} (AM|PM)"))) {
            textWaitingTime.setTextColor(Color.RED)
        } else {
            textWaitingTime.setTextColor(Color.GREEN)
        }

        buttonSelectBus.setOnClickListener {
            showPassengerDialog(context, bus, this)
        }

        return view
    }

    private fun formatWaitingTime(waitingTime: String): String {
        val minutes = waitingTime.replace(" min", "").toIntOrNull()

        return if (minutes != null && minutes >= 60) {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            if (remainingMinutes == 0) {
                "$hours hr"
            } else {
                "$hours hr $remainingMinutes min"
            }
        } else {
            waitingTime
        }
    }

    private fun showPassengerDialog(context: Context, bus: BusDetails, adapter: BusDetailsAdapter) {
        val dialogView = inflater.inflate(R.layout.dialog_passenger_count, null)

        val editPassengerCount = dialogView.findViewById<EditText>(R.id.editPassengerCount)
        val textTotalPrice = dialogView.findViewById<TextView>(R.id.textTotalPrice)
        val buttonConfirmBooking = dialogView.findViewById<Button>(R.id.buttonConfirmBooking)

        val pricePerStoppage = 5
        val pricePerPassenger = pricePerStoppage * bus.stopCount

        editPassengerCount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val passengerCount = s.toString().toIntOrNull()
                if (passengerCount != null && passengerCount > 0) {
                    val totalPrice = passengerCount * pricePerPassenger
                    textTotalPrice.text = "Total Price: Rs. $totalPrice"
                } else {
                    textTotalPrice.text = "Total Price: Rs. 0"
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        buttonConfirmBooking.setOnClickListener {
            val passengerCount = editPassengerCount.text.toString().trim().toIntOrNull()
            if (passengerCount == null || passengerCount <= 0) {
                Toast.makeText(context, "Enter a valid number of passengers", Toast.LENGTH_SHORT).show()
            } else {
                val totalPrice = passengerCount * pricePerPassenger
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
                val db = FirebaseFirestore.getInstance()

                val bookingRef = db.collection("bookings").document()
                val bookingId = bookingRef.id

                val bookingData = hashMapOf(
                    "bookingId" to bookingId,
                    "userId" to userId,
                    "busName" to bus.busName,
                    "routeName" to bus.routeName,
                    "fromTo" to "${bus.from} -> ${bus.to}",
                    "bookingTime" to System.currentTimeMillis(),
                    "passengerCount" to passengerCount,
                    "totalPrice" to totalPrice,
                    "isScanned" to false
                )

                bookingRef.set(bookingData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Booking Confirmed!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        adapter.notifyDataSetChanged() // Refresh the adapter after booking
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Booking Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        dialog.show()
    }
}
