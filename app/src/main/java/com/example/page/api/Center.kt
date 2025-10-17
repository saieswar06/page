package com.example.page.api

data class Center(
    val id: Int,
    val center_code: String,
    val center_name: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)
