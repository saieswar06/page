package com.example.page.api

import com.google.gson.annotations.SerializedName

/**
 * Represents the JSON body for a login request.
 * The field names here MUST match what the server expects.
 * Using @SerializedName ensures the correct JSON is sent even if variable names differ.
 */
data class LoginRequest(
    // Use @SerializedName to send "mobile_number" in the JSON, matching the database.
    @SerializedName("mobile")
    val mobileNumber: String? = null, // Changed from 'mobile' to 'mobileNumber'

    // Add an email field for admin/supervisor logins.
    @SerializedName("email")
    val email: String? = null,

    @SerializedName("password")
    val password: String,

    // This field is likely not used by the server, but we can keep it for now.
    @SerializedName("loginType")
    val loginType: String? = null
)
