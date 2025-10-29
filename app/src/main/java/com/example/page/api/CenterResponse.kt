package com.example.page.api

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class CenterResponse(
    val id: Int?,
    val center_code: String?,
    val center_name: String?,
    val address: String?,
    val locality: String?,
    val district: String?,
    val state: String?,
    val mandal: String?,
    val village: String?,
    val pincode: String?,
    val contact_number: String?,
    val email: String?,
    val latitude: String?,
    val longitude: String?,
    @SerializedName("teacher_count") val teacher_count: Int = 0,
    val created_at: String?,
    val updated_at: String?,
    val is_active: Int?,
    val status: Int? = null,
    val reason: String? = null,
    val deletion_reason: String? = null,
    val total_children: Int? = 0
) : Parcelable