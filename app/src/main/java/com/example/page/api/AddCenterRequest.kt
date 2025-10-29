package com.example.page.api

data class AddCenterRequest(
    val center_name: String,
    val address: String? = null,
    val email: String? = null,
    val mobile: String? = null,
    val password: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val state: String? = null,
    val district: String? = null,
    val locality: String? = null,
    val mandal: String? = null,
    val stateCode: Int? = null,
    val districtCode: Int? = null,
    val blockCode: Int? = null,
    val sectorCode: Int? = null
)
