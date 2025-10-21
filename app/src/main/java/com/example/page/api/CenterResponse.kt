package com.example.page.api

data class CenterResponse(
    val id: Int,
    val centerCode: String,
    val center_name: String,
    val address: String,
    val sectorCode: Int,
    val blockCode: Int,
    val districtCode: Int,
    val stateCode: Int
)
