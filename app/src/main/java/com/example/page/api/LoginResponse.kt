package com.example.page.api

data class LoginResponse(
    val token: String,
    val user: UserDto
)
