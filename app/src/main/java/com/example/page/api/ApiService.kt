package com.example.page.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("auth/check-email")
    fun checkEmail(@Body body: Map<String, String>): Call<Map<String, String>>

    @POST("auth/login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    @POST("auth/google")
    fun googleLogin(@Body body: Map<String, String>): Call<LoginResponse>
}
