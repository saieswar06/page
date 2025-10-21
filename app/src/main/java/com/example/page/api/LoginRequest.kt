package com.example.page.api

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("mobile") val mobile: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("password") val password: String,
    @SerializedName("loginType") val loginType: String // REQUIRED
)
