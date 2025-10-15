package com.example.page.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    // ECCE worker login (mobile + password on your backend)
    @POST("auth/login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    // Admin/Supervisor login (email + password). If your backend
    // doesn’t have this yet, point it to the correct route later.
    @POST("auth/admin-login")
    fun adminLogin(@Body body: AdminLoginRequest): Call<LoginResponse>

    @POST("auth/google")
    fun googleLogin(@Body body: Map<String, String>): Call<LoginResponse>

    @POST("auth/check-email")
    fun checkEmail(@Body body: Map<String, String>): Call<Map<String, String>>

    // Admin → Centers
    @GET("centers")
    fun getCenters(): Call<List<Center>>

    @POST("centers/add")
    fun addCenter(@Body body: AddCenterRequest): Call<Void>
}
