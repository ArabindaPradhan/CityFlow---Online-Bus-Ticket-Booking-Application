package com.example.busconductorappv2

data class Ticket(
    val bookingId: String = "",
    val bookingTime: Long = 0,
    val busName: String = "",
    val fromTo: String = "",
    val isScanned: Boolean = false,  // Ensure Boolean type
    val passengerCount: Int = 0,
    val totalPrice: Int = 0,
    val userId: String = ""
)
