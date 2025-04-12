package com.example.test.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.test.Bus
import com.example.test.R

class BusAdapter(
    private val busList: List<Bus>,
    private val onBusClick: (Bus) -> Unit // Callback for handling item clicks
) : RecyclerView.Adapter<BusAdapter.BusViewHolder>() {

    inner class BusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val busName: TextView = itemView.findViewById(R.id.text_bus_name)
        val arrivalTime: TextView = itemView.findViewById(R.id.text_arrival_time)
        val route: TextView = itemView.findViewById(R.id.text_bus_route)

        fun bind(bus: Bus) {
            busName.text = bus.busName

            // Remove " min" and convert to integer
            val waitingMinutes = bus.waitingTime.replace(" min", "").toIntOrNull() ?: 0

            // Convert waiting time to hours and minutes if > 60 minutes
            val formattedWaitingTime = if (waitingMinutes >= 60) {
                val hours = waitingMinutes / 60
                val minutes = waitingMinutes % 60
                if (minutes == 0) "$hours hr" else "$hours hr $minutes min"
            } else {
                "$waitingMinutes min"
            }

            arrivalTime.text = "Arriving in: $formattedWaitingTime"
            route.text = "To: ${bus.route}"

            itemView.setOnClickListener {
                onBusClick(bus) // Trigger click event
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bus, parent, false)
        return BusViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusViewHolder, position: Int) {
        holder.bind(busList[position])
    }

    override fun getItemCount(): Int = busList.size
}
