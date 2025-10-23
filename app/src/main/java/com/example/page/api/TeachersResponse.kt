package com.example.page.api



import com.google.gson.annotations.SerializedName

data class TeachersResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<TeacherModel>?
)
