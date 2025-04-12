package com.example.test

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookingAdapter(
    private val bookings: List<BookingDetails>,
    private val onItemClick: (BookingDetails) -> Unit // Pass click event listener
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    private val sortedBookings = bookings.sortedByDescending { it.bookingTime }

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textPassengerCount: TextView = itemView.findViewById(R.id.textPassenger)
        val textBusName: TextView = itemView.findViewById(R.id.textBusName)
        val textRouteName: TextView = itemView.findViewById(R.id.textRouteName)
        val textFromTo: TextView = itemView.findViewById(R.id.textFromTo)
        val textBookingTime: TextView = itemView.findViewById(R.id.textBookingTime)
        val textPrice: TextView = itemView.findViewById(R.id.textPrice)
        val status: TextView = itemView.findViewById(R.id.textStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = sortedBookings[position]

        holder.textPassengerCount.text = "Passengers: ${booking.passengerCount}"
        holder.textBusName.text = "Bus: ${booking.busName}"
        holder.textRouteName.text = "Route: ${booking.routeName}"
        holder.textFromTo.text = booking.fromTo
        holder.textBookingTime.text = "Booking Time: ${formatBookingTime(booking.bookingTime)}"
        holder.textPrice.text = "Total: Rs. ${booking.totalPrice}"

        val statusText = checkStatus(booking.bookingTime, booking.isScanned)
        holder.status.text = statusText

        when (statusText) {
            "Active" -> {
                holder.status.setTextColor(Color.GREEN)
                holder.itemView.alpha = 1.0f // Fully visible
            }
            "Used Ticket" -> {
                holder.status.setTextColor(Color.GRAY)
                holder.itemView.alpha = 0.5f // Faded
            }
            "Expired" -> {
                holder.status.setTextColor(Color.RED)
                holder.itemView.alpha = 0.5f // Faded
            }
        }

        // Enable click only if ticket is active
        holder.itemView.isEnabled = statusText == "Active"

        if (statusText == "Active") {
            holder.itemView.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, QRGeneratorActivity::class.java).apply {
                    putExtra("bookingId", booking.bookingId) // 🔹 Using bookingId
                    putExtra("busName", booking.busName)
                    putExtra("fromTo", booking.fromTo)
                    putExtra("routeName", booking.routeName)
                    putExtra("passengerCount", booking.passengerCount)
                    putExtra("totalPrice", booking.totalPrice)
                    putExtra("userId", booking.userId)
                }
                context.startActivity(intent)
            }
        } else {
            holder.itemView.setOnClickListener(null) // Prevent clicks on expired/used tickets
        }
    }

    private fun formatBookingTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun checkStatus(bookingTime: Long, isScanned: Boolean): String {
        val currentTime = System.currentTimeMillis()
        val twentyFourHoursInMillis = 24 * 60 * 60 * 1000
        val bookingTimeMillis = if (bookingTime < currentTime / 1000) bookingTime * 1000 else bookingTime

        return when {
            isScanned -> "Used Ticket"
            currentTime - bookingTimeMillis >= twentyFourHoursInMillis -> "Expired"
            else -> "Active"
        }
    }


    override fun getItemCount(): Int = sortedBookings.size
}
