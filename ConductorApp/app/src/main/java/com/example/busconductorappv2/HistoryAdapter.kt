package com.example.busconductorappv2

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(private var historyList: MutableList<Ticket>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    fun updateData(newList: List<Ticket>) {
        historyList.clear()
        historyList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val ticket = historyList[position]

        val formattedTime = convertMillisToTime(ticket.bookingTime)

        holder.txtRoute.text = "${ticket.fromTo}"
        holder.txtBookingTime.text = "Booking Time: $formattedTime"
        holder.txtPassengers.text = "Passengers: ${ticket.passengerCount}"
        holder.txtTotalPrice.text = "Total Price: ₹${ticket.totalPrice}"

        // Set text color (Example: Red for emphasis)
        holder.txtRoute.setTextColor(Color.RED)
        holder.txtBookingTime.setTextColor(Color.BLACK)
        holder.txtPassengers.setTextColor(Color.BLACK)
        holder.txtTotalPrice.setTextColor(Color.BLACK)
    }


    override fun getItemCount(): Int = historyList.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtRoute: TextView = itemView.findViewById(R.id.txtRoute)
        val txtPassengers: TextView = itemView.findViewById(R.id.txtPassengers)
        val txtTotalPrice: TextView = itemView.findViewById(R.id.txtTotalPrice)
        val txtBookingTime: TextView = itemView.findViewById(R.id.txtBookingTime)
    }

    private fun convertMillisToTime(timeInMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timeInMillis))
    }

}
