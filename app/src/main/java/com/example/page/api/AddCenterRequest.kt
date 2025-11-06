package com.example.page.api

data class AddCenterRequest(
    val center_name: String,
    val state: String,
    val district: String,
    val mandal: String,
    val locality: String,
    val pincode: String,
    val latitude: Double,
    val longitude: Double,
    val stateCode: String,
    val districtCode: String,
    val projectCode: String,
    val sectorCode: String
)
