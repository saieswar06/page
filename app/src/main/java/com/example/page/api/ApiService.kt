package com.example.page.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("auth/login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    @POST("auth/login")
    fun adminLogin(@Body body: AdminLoginRequest): Call<LoginResponse>

    @POST("auth/google")
    fun googleLogin(@Body body: Map<String, String>): Call<LoginResponse>

    @POST("auth/check-email")
    fun checkEmail(@Body body: Map<String, String>): Call<Map<String, String>>

    @GET("api/admin/centers")
    fun getCenters(): Call<List<Center>>

    @POST("api/admin/centers/add")
    fun addCenter(@Body body: AddCenterRequest): Call<Void>
}
