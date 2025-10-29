package com.example.page.api

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: LoginData?,

    @SerializedName("message")
    val message: String
)

data class LoginData(
    @SerializedName("user")
    val user: UserDto?,

    @SerializedName("token")
    val token: String?
)
