package com.example.page.api

// For Admin / Supervisor login screen
data class AdminLoginRequest(
    val email: String,
    val password: String,
    val role: String // "Admin" | "Peer Reviewer" | "Niti Surveyor"
)
