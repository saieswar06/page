package com.example.page.api

data class AddCenterRequest(
    val center_name: String,
    val address: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val stateCode: Int? = 23,
    val districtCode: Int? = 1,
    val blockCode: Int? = 1,
    val sectorCode: Int? = 1
)
