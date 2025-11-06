package com.example.page.api

import com.google.gson.annotations.SerializedName

/**
 * Data class for the deactivate center request.
 */
data class DeactivateCenterRequest(
    @SerializedName("reason") val reason: String
)
