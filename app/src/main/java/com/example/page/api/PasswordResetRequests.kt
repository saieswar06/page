package com.example.page.api

import com.google.gson.annotations.SerializedName

data class ChangePasswordRequest(
    @SerializedName("current_password") val current_password: String,
    @SerializedName("new_password") val new_password: String
)

data class RequestPasswordResetRequest(
    @SerializedName("email") val email: String
)

data class VerifyOtpRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("new_password") val new_password: String
)

data class UpdateProfileRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String
)

data class ProfilePictureUrl(
    @SerializedName("profile_picture_url") val profilePictureUrl: String
)
