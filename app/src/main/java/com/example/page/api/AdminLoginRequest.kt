package com.example.page.api

// For Admin / Supervisor login screen
data class AdminLoginRequest(
    val email: String,
    val password: String,
    val loginType: String // e.g. "admin" | "peer_reviewer" | "niti_surveyor"
)
