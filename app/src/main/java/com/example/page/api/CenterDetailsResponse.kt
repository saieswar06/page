package com.example.page.api
import com.google.gson.annotations.SerializedName

data class CenterDetailsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: CenterResponse? // The 'data' field contains a single Center object
)
