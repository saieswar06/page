

package com.example.page.api

/**
 * ✅ Common error response model for parsing backend messages.
 * Backend returns JSON like:
 * { "message": "Invalid credentials or unauthorized" }
 */
data class ErrorResponse(
    val message: String?
)
