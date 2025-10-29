package com.example.page.api

import com.google.gson.annotations.SerializedName

data class AddTeacherRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String,
    @SerializedName("defaultPassword") val defaultPassword: String,
    @SerializedName("center_code") val centerCode: String
)