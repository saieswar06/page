package com.example.page.api

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class CenterResponse(
    val id: Int?,
    val center_code: String?,
    val center_name: String?,
    val locality: String?,
    val district: String?,
    val state: String?,
    val mandal: String?,
    val pincode: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("teacher_count") val teacher_count: Int = 0,
    val created_at: String?,
    val updated_at: String?,
    val deleted_at: String?,
    val status: Int? = null,
    val reason: String? = null
) : Parcelable
