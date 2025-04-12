package com.example.test.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.test.Bus
import com.example.test.R
import com.example.test.adapter.BusAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.*

class HomeFragment : Fragment() {

    private val TAG = "CityFlowApp"

    private lateinit var recyclerViewBuses: RecyclerView
    private lateinit var busAdapter: BusAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var nearestStopText: TextView

    private val db = FirebaseFirestore.getInstance()
    private val routes = mutableListOf<Route>()
    private val buses = mutableListOf<Bus>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        nearestStopText = view.findViewById(R.id.nearest_stop)
        recyclerViewBuses = view.findViewById(R.id.recyclerView_buses)
        recyclerViewBuses.layoutManager = LinearLayoutManager(requireContext())
        busAdapter = BusAdapter(emptyList()) {}
        recyclerViewBuses.adapter = busAdapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        fetchRoutesAndBuses()

        view.findViewById<Button>(R.id.button2).setOnClickListener {
            fetchRoutesAndBuses()
        }

        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchRoutesAndBuses() {
        Log.d(TAG, "Fetching routes from Firestore...")
        db.collection("routes").get().addOnSuccessListener { routeDocs ->
            routes.clear()
            for (doc in routeDocs) {
                val route = Route(
                    routeName = doc.getString("routeName") ?: "",
                    stoppages = doc["stoppages"] as? List<String> ?: emptyList()
                )
                routes.add(route)
                Log.d(TAG, "Fetched route: $route")
            }
            fetchBuses()
        }.addOnFailureListener {
            Log.e(TAG, "Failed to load routes", it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchBuses() {
        Log.d(TAG, "Fetching buses from Firestore...")
        db.collection("buses").get().addOnSuccessListener { busDocs ->
            buses.clear()
            for (doc in busDocs) {
                val timingsData = doc["Timings"] as? Map<String, Map<String, String>> ?: emptyMap()
                val bus = Bus(
                    busName = doc.getString("busName") ?: "Unknown",
                    busRegdNumber = doc.getString("busRegdNumber") ?: "",
                    capacity = doc.getString("capacity") ?: "0",
                    route = doc.getString("route") ?: "",
                    waitingTime = "Calculating...",
                    timings = timingsData
                )
                buses.add(bus)
                Log.d(TAG, "Fetched bus: $bus")
            }
            fetchNearestStop()
        }.addOnFailureListener {
            Log.e(TAG, "Failed to load buses", it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchNearestStop() {
        Log.d(TAG, "Fetching stoppages from Firestore...")
        db.collection("Stoppages").document("stoppages").get().addOnSuccessListener { document ->
            val stoppageData = document.data ?: return@addOnSuccessListener
            getDeviceLocation { userLat, userLng ->
                val nearestStop = findNearestStoppage(userLat, userLng, stoppageData)
                nearestStopText.text = nearestStop
                Log.d(TAG, "Nearest stoppage detected: $nearestStop")
                showBusesForNearestStop(nearestStop)
            }
        }.addOnFailureListener {
            Log.e(TAG, "Failed to fetch stoppages", it)
        }
    }

    private fun getDeviceLocation(onLocationFetched: (Double, Double) -> Unit) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                Log.d(TAG, "User location fetched: Latitude=${it.latitude}, Longitude=${it.longitude}")
                onLocationFetched(it.latitude, it.longitude)
            } ?: Log.e(TAG, "Failed to fetch user location: Location is null")
        }
    }

    private fun findNearestStoppage(userLat: Double, userLng: Double, stoppages: Map<String, Any>): String {
        var nearestStop = "Unknown"
        var minDistance = Double.MAX_VALUE
        for ((name, geoPoint) in stoppages) {
            if (geoPoint is GeoPoint) {
                val distance = haversine(userLat, userLng, geoPoint.latitude, geoPoint.longitude)
                Log.d(TAG, "Distance to stoppage '$name': $distance km")
                if (distance < minDistance) {
                    minDistance = distance
                    nearestStop = name
                }
            }
        }
        return nearestStop
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showBusesForNearestStop(nearestStop: String) {
        val currentTime = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")

        val routeDetails = routes.find { it.stoppages.contains(nearestStop) } ?: return
        val stoppages = routeDetails.stoppages
        val displayedBuses = mutableListOf<Bus>()

        Log.d(TAG, "Showing buses for route: ${routeDetails.routeName}")

        for (bus in buses) {
            if (bus.route != routeDetails.routeName) continue

            for ((tripName, schedule) in bus.timings) {
                try {
                    val departureMinutes = convertTimeToMinutes(schedule["departure"] ?: continue)
                    val arrivalMinutes = convertTimeToMinutes(schedule["arrival"] ?: continue)

                    val stopIndex = stoppages.indexOf(nearestStop)
                    if (stopIndex == -1) continue

                    val totalStops = stoppages.size
                    if (totalStops <= 1) continue

                    val perStopTime = (arrivalMinutes - departureMinutes) / (totalStops - 1)
                    val nearestStopTime = if (tripName.contains("Reverse")) {
                        arrivalMinutes - (perStopTime * (totalStops - 1 - stopIndex))
                    } else {
                        departureMinutes + (perStopTime * stopIndex)
                    }
                    val currentMinutes = convertTimeToMinutes(currentTime.format(formatter))

                    val waitingTime = nearestStopTime - currentMinutes
                    if (waitingTime < 0) continue

                    val destinationStoppage = if (tripName.contains("Reverse")) {
                        stoppages.firstOrNull() ?: "Unknown Destination"
                    } else {
                        stoppages.lastOrNull() ?: "Unknown Destination"
                    }

                    val busEntry = bus.copy(
                        waitingTime = "$waitingTime min",
                        route = destinationStoppage
                    )

                    displayedBuses.add(busEntry)
                    Log.d(TAG, "Bus: ${bus.busName}, Waiting Time: $waitingTime min")
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing time for Bus: ${bus.busName}, Trip: $tripName", e)
                }
            }
        }


        // **Sort buses by waiting time (ascending order)**
        displayedBuses.sortBy { it.waitingTime.replace(" min", "").toIntOrNull() ?: Int.MAX_VALUE }

        // **Update RecyclerView**
        busAdapter = BusAdapter(displayedBuses) { selectedBus ->
            val bundle = Bundle().apply {
                putString("nearestStop", nearestStop) // ✅ Pass nearest stop
            }

            findNavController().navigate(R.id.action_homeFragment_to_bookTicketFragment, bundle)
        }
        recyclerViewBuses.adapter = busAdapter
    }

    // **Helper function to convert time (hh:mm AM/PM) to total minutes**
    @RequiresApi(Build.VERSION_CODES.O)
    private fun convertTimeToMinutes(time: String): Int {
        return try {
            val formatter = DateTimeFormatter.ofPattern("hh:mm a")
            val localTime = LocalTime.parse(time, formatter)
            localTime.hour * 60 + localTime.minute
        } catch (e: Exception) {
            Log.e(TAG, "Time parsing error for: $time", e)
            Int.MAX_VALUE  // Return large value in case of error to avoid sorting issues
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateWaitingTime(departureTime: LocalTime): String {
        val currentTime = LocalTime.now()
        val minutesDiff = java.time.Duration.between(currentTime, departureTime).toMinutes()
        return if (minutesDiff > 0) "$minutesDiff min" else "Bus left"
    }
}

data class Route(val routeName: String, val stoppages: List<String>)

data class Bus(
    val busName: String,
    val busRegdNumber: String,
    val capacity: String,
    val route: String,
    var waitingTime: String,
    val timings: Map<String, Map<String, String>>
)
