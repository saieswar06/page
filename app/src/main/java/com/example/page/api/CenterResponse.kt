package com.example.page.api
import com.google.gson.annotations.SerializedName

data class CenterResponse(
    @SerializedName("id") // The name in the JSON response from your server
    val id: Int,

    @SerializedName("center_name")
    val center_name: String?,

    @SerializedName("center_code")
    val center_code: String?, // Your code will now compile with this property name

    @SerializedName("state")
    val state: String?,

    @SerializedName("district")
    val district: String?,
    @SerializedName("lattitude")
    val latitude: String?,
    @SerializedName("logitude")
    val longitude: String?,
    @SerializedName("mandal")
    val mandal: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("locality")
    val locality: String?,
    val center_address: String

)
