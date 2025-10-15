package com.example.page.api

data class AddCenterRequest(
    val centerName: String,
    val email: String,
    val mobile: String,
    val password: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)
