package com.example.test

data class BookingDetails(
    val userId: String = "",
    val routeName: String = "",
    val busName: String = "",
    val fromTo: String = "",
    val bookingTime: Long = 0L,
    val passengerCount: Int = 0,
    val totalPrice: Int = 0,
    val isScanned: Boolean = false,
    val bookingId: String = ""
)
