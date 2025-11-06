package com.example.page.api

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class TeacherModel(
    @SerializedName("uid") val uid: String?,
    @SerializedName("unique_id") val unique_id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("center_code") val centerCode: String?,
    @SerializedName("center_name") val centerName: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("district") val district: String?,
    @SerializedName("locality") val locality: String?,
    @SerializedName("mandal") val mandal: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("status") val status: Int?,
    @SerializedName("created_at") val created_at: String?,
    @SerializedName("updated_at") val updated_at: String?,
    @SerializedName("deleted_at") val deleted_at: String?,
    val reason: String?,
    @SerializedName("reason_timestamp") val reason_timestamp: String?
) : Parcelable
