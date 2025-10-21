package com.example.page.api

data class AddCenterRequest(
    val center_name: String,
    val address: String,
    val email: String? = null,
    val mobile: String? = null,
    val password: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val state: String? = null,
    val district: String? = null,
    val locality: String? = null,
    val mandal: String? = null,
    val stateCode: Int = 23,
    val districtCode: Int = 1,
    val blockCode: Int = 1,
    val sectorCode: Int = 1
)
