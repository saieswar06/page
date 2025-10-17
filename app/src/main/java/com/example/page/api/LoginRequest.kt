package com.example.page.api

// For ECCE worker login
data class LoginRequest(
    val mobile: String,
    val password: String,
    val loginType: String // e.g. "ecce" or "beneficiary"
)
