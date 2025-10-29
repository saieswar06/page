package com.example.page.api

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("unique_id")
    val uniqueId: String?,

    @SerializedName("role_id")
    val roleId: Int?,

    @SerializedName("full_name")
    val fullName: String?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("mobile_number")
    val mobileNumber: String?,

    @SerializedName("center_code")
    val centerCode: String?,

    @SerializedName("status")
    val status: Int?,

    @SerializedName("last_login")
    val lastLogin: String?
)
