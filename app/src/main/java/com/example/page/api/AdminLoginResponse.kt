package com.example.page.api

data class AdminLoginResponse(
    val token: String,
    val user: AdminUser
)

data class AdminUser(
    val id: Int,
    val email: String,
    val role: String
)