package com.example.test

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class BookTicket : Fragment() {

    private lateinit var stoppagesFrom: AutoCompleteTextView
    private lateinit var stoppagesTo: AutoCompleteTextView
    private lateinit var checkButton: Button
    private val db = FirebaseFirestore.getInstance()

    private lateinit var routes: MutableList<Route>
    private lateinit var buses: MutableList<Bus>
    private var stoppagesList: List<String> = listOf("Select Stop")

    private var nearestStop: String? = null

    companion object {
        fun newInstance(nearestStop: String): BookTicket {
            val fragment = BookTicket()
            val args = Bundle()
            args.putString("nearestStop", nearestStop)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nearestStop = arguments?.getString("nearestStop")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_book_ticket, container, false)

        stoppagesFrom = view.findViewById(R.id.autocomplete_stoppages_from)
        stoppagesTo = view.findViewById(R.id.autocomplete_stoppages_to)
        checkButton = view.findViewById(R.id.button)

        fetchRoutes()
        fetchBuses()

        checkButton.setOnClickListener {
            val from = stoppagesFrom.text.toString()
            val to = stoppagesTo.text.toString()

            if (from == "Select Stop" || to == "Select Stop" || from.isEmpty() || to.isEmpty()) {
                Toast.makeText(requireContext(), "Please select valid stoppages!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (from == to) {
                Toast.makeText(requireContext(), "Source and Destination cannot be the same!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val routesList = findRoutesForStoppages(from, to)
            Log.d("BusBooking", "Routes found: ${routesList.size}")

            if (routesList.isEmpty()) {
                Toast.makeText(requireContext(), "No direct buses found!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val busDetailsList = calculateBusDetails(routesList, from, to)
            if (busDetailsList.isEmpty()) {
                Toast.makeText(requireContext(), "No available buses!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            showBusDetailsDialog(busDetailsList)
        }

        return view
    }

    private fun fetchRoutes() {
        db.collection("routes").get().addOnSuccessListener { documents ->
            routes = mutableListOf()
            val allStoppages = mutableSetOf<String>()

            for (doc in documents) {
                val route = Route(
                    routeName = doc.getString("routeName") ?: "",
                    stoppages = doc["stoppages"] as? List<String> ?: emptyList()
                )
                Log.d("BusBooking", "Fetched Route: ${route.routeName}, Stoppages: ${route.stoppages}")
                routes.add(route)
                allStoppages.addAll(route.stoppages)
            }

            stoppagesList = allStoppages.toList()
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, stoppagesList)
            stoppagesFrom.setAdapter(adapter)
            stoppagesTo.setAdapter(adapter)

            nearestStop?.let {
                if (stoppagesList.contains(it)) {
                    stoppagesFrom.setText(it, false)
                }
            }
        }.addOnFailureListener {
            Log.e("BusBooking", "Failed to load routes", it)
        }
    }

    private fun fetchBuses() {
        db.collection("buses").get().addOnSuccessListener { documents ->
            buses = mutableListOf()
            for (doc in documents) {
                val busName = doc.getString("busName") ?: ""
                val route = doc.getString("route") ?: ""

                if (busName.isNotBlank() && route.isNotBlank()) {
                    val timings = doc.get("Timings") as? Map<String, Map<String, String>>
                    Log.d("BusBooking", "Fetched Bus: $busName, Route: $route, Timings: $timings")

                    val bus = Bus(
                        busName = busName,
                        busRegdNumber = doc.getString("busRegdNumber") ?: "",
                        capacity = doc.getString("capacity") ?: "0",
                        route = route,
                        waitingTime = "Calculating...",
                        timings = timings ?: emptyMap()
                    )
                    buses.add(bus)
                }
            }
        }.addOnFailureListener {
            Log.e("BusBooking", "Failed to load buses", it)
        }
    }

private fun findRoutesForStoppages(from: String, to: String): List<Route> {
        return routes.filter { it.stoppages.contains(from) && it.stoppages.contains(to) }
    }

    private fun calculateBusDetails(routes: List<Route>, from: String, to: String): List<BusDetails> {
        val busDetailsMap = mutableMapOf<String, BusDetails>()
        val currentTime = Calendar.getInstance()
        val currentMinutes = currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE)

        for (route in routes) {
            val busesForRoute = buses.filter { it.route == route.routeName }

            for (bus in busesForRoute) {
                Log.d("BusBooking", "Checking bus ${bus.busName} on route ${route.routeName}")
                Log.d("BusBooking", "Bus ${bus.busName} has timings: ${bus.timings}")

                val fromIndex = route.stoppages.indexOf(from)
                val toIndex = route.stoppages.indexOf(to)

                if (fromIndex == -1 || toIndex == -1) continue // Skip if stops are not in route

                val isForwardDirection = fromIndex < toIndex
                var firstDepartureTime: String? = null  // Store first departure time (06:00 AM)
                var firstEstimatedTime: Int? = null     // Store estimated time for first departure

                for ((tripKey, timing) in bus.timings) {
                    if (isForwardDirection && tripKey.contains("Reverse")) continue
                    if (!isForwardDirection && !tripKey.contains("Reverse")) continue

                    val departureTime = timing["departure"] ?: continue
                    val arrivalTime = timing["arrival"] ?: continue

                    val departureMinutes = convertTimeToMinutes(departureTime)
                    val arrivalMinutes = convertTimeToMinutes(arrivalTime)

                    Log.d("BusBooking", "Departure time: $departureTime -> Minutes: $departureMinutes, Current minutes: $currentMinutes")

                    val totalStops = route.stoppages.size
                    if (totalStops <= 1) continue

                    val perStopTime = (arrivalMinutes - departureMinutes) / (totalStops - 1)

                    val fromStopTime = if (isForwardDirection) {
                        departureMinutes + (perStopTime * fromIndex)
                    } else {
                        arrivalMinutes - (perStopTime * (route.stoppages.size - 1 - fromIndex))
                    }


                    Log.d("BusBooking", "Bus reaches '$from' at: $fromStopTime minutes")

                    val waitingTime = fromStopTime - currentMinutes

                    if (waitingTime < 0) {
                        if (firstDepartureTime == null || departureMinutes < convertTimeToMinutes(firstDepartureTime)) {
                            firstDepartureTime = departureTime  // Store earliest departure time
                            firstEstimatedTime = perStopTime * kotlin.math.abs(toIndex - fromIndex) // Store estimated time
                        }
                        continue  // Skip past buses
                    }

                    val estimatedTime = perStopTime * kotlin.math.abs(toIndex - fromIndex)

                    val busDetail = BusDetails(
                        bus.busName, bus.route, "$estimatedTime min", "$waitingTime min",
                        kotlin.math.abs(toIndex - fromIndex), from, to
                    )

                    Log.d("BusBooking", "Bus added: $busDetail")

                    if (!busDetailsMap.containsKey(bus.busName) ||
                        waitingTime < busDetailsMap[bus.busName]!!.waitingTime.replace(" min", "").toInt()) {
                        busDetailsMap[bus.busName] = busDetail
                    }
                }

                // If no bus is available today, show first departure time (06:00 AM) with estimated time
                if (!busDetailsMap.containsKey(bus.busName) && firstDepartureTime != null && firstEstimatedTime != null) {
                    val nextDayBusDetail = BusDetails(
                        bus.busName, bus.route, "$firstEstimatedTime min", firstDepartureTime, // Show departure time instead of waiting time
                        kotlin.math.abs(toIndex - fromIndex), from, to
                    )
                    Log.d("BusBooking", "No bus available today. Showing next departure time: ${firstDepartureTime}")
                    busDetailsMap[bus.busName] = nextDayBusDetail
                }
            }
        }

        return busDetailsMap.values
            .sortedWith(compareBy {
                it.waitingTime.replace(" min", "").toIntOrNull() ?: Int.MAX_VALUE
            })
            .toList()

    }

    private fun convertTimeToMinutes(time: String): Int {
        val parts = time.split(" ", limit = 2)
        if (parts.size != 2) return 0

        val timeParts = parts[0].split(":").map { it.toInt() }
        var hours = timeParts[0]
        val minutes = timeParts[1]
        val period = parts[1]

        if (period.equals("PM", ignoreCase = true) && hours != 12) {
            hours += 12
        } else if (period.equals("AM", ignoreCase = true) && hours == 12) {
            hours = 0
        }

        return (hours * 60) + minutes
    }

    private fun showBusDetailsDialog(busDetailsList: List<BusDetails>) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_bus_list, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).setCancelable(true).create()

        val listView = dialogView.findViewById<ListView>(R.id.listViewBuses)
        val adapter = BusDetailsAdapter(requireContext(), busDetailsList)
        listView.adapter = adapter

        Log.d("BusBooking", "Showing dialog with ${busDetailsList.size} buses")
        dialog.show()
    }
}

data class Route(val routeName: String, val stoppages: List<String>)
data class Bus(
    val busName: String,
    val busRegdNumber: String,
    val capacity: String,
    var route: String,
    var waitingTime: String,
    val timings: Map<String, Map<String, String>>
)
data class BusDetails(
    val busName: String,
    val routeName: String, // Add this field
    val estimatedTime: String,
    val waitingTime: String,
    val stopCount: Int,
    val from: String,
    val to: String
)

