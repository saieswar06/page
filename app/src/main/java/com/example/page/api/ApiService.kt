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
    // Correct backend path: /api/admin/centers
    @GET("api/admin/centers")
    fun getCenters(@Header("Authorization") token: String): Call<CentersResponse>

    @POST("api/admin/centers")
    fun addCenter(
        @Header("Authorization") token: String,
        @Body body: AddCenterRequest
    ): Call<ApiResponse>

    @PUT("api/admin/centers/{id}")
    fun updateCenter(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body body: AddCenterRequest
    ): Call<ApiResponse>

    @DELETE("api/admin/centers/{id}")
    fun deleteCenter(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Call<ApiResponse>
    @GET("api/admin/centers/count")
    fun getCenterCount(
        @Header("Authorization") token: String
    ): Call<CountResponse>

    @GET("api/admin/teachers/count")
    fun getTeacherCount(
        @Header("Authorization") token: String
    ): Call<CountResponse>

}
