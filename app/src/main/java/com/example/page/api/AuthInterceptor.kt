package com.example.page.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

/**
 * ✅ Automatically attaches JWT token to every request.
 * Reads token from SharedPreferences ("AdminSession").
 */
class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val prefs = context.getSharedPreferences("AdminSession", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)

        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
