package com.example.page.api

import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ✅ Unified login (worker + admin)
    @POST("auth/login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    // ✅ Google login
    @POST("auth/google-login")
    fun googleLogin(@Body body: Map<String, String>): Call<LoginResponse>

    // ✅ Centers API (Admin only)
    // Backend: routes/centerRoutes.js -> app.use("/centers", ...)
    @GET("centers")
    fun getCenters(): Call<List<CenterResponse>>

    @POST("centers/add")
    fun addCenter(@Body body: AddCenterRequest): Call<ApiResponse>

    @PUT("centers/update/{id}")
    fun updateCenter(@Path("id") id: Int, @Body body: AddCenterRequest): Call<ApiResponse>

    @DELETE("centers/delete/{id}")
    fun deleteCenter(@Path("id") id: Int): Call<ApiResponse>
}
