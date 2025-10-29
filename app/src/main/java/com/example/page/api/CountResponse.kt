package com.example.page.api

import com.google.gson.annotations.SerializedName

data class CountResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: CountData? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("message") val message: String? = null
)

data class CountData(
    @SerializedName("count") val count: Int? = null
)
