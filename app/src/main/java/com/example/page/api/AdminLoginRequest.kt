package com.example.page.api

import com.google.gson.annotations.SerializedName

data class AdminLoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)
