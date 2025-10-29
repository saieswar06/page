package com.example.page.api

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id")
    val id: Int?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("mobile")
    val mobile: String?,

    @SerializedName("role_id")
    val roleId: Int?
)