package com.example.page.api

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class TeacherModel(
    @SerializedName("uid") val uid: Int?,
    @SerializedName("userid") val userId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("center_code") val centerCode: String?,
    @SerializedName("center_name") val centerName: String?,
    @SerializedName("status") val status: Int?
) : Parcelable
