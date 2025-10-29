package com.example.page.api

import com.google.gson.annotations.SerializedName

data class CenterDetailsResponse(
    val success: Boolean,
    val message: String?,
    val data: CenterDetails?
)

data class CenterDetails(
    val center_name: String?,
    val center_code: String?,
    val state: String?,
    val district: String?,
    val mandal: String?,
    val locality: String?,
    val latitude: Double?,
    val longitude: Double?,
    val teachers: List<TeacherInfo>?,
    val teacher_count: Int?  // Added from backend response
)

data class TeacherInfo(
    val unique_id: String?,
    @SerializedName("name")  // This matches the alias from backend
    val name: String?,
    val full_name: String?,  // Fallback if backend sends full_name
    val mobile_number: String?,
    val email: String?
)