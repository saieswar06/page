package com.example.page.api

import retrofit2.Call
import retrofit2.http.*
//import retrofit2.http.Header
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
    fun getCenters(): Call<CentersResponse>

    @POST("api/admin/centers")
    fun addCenter(@Body body: AddCenterRequest): Call<ApiResponse>

    @PUT("api/admin/centers/{id}")
    fun updateCenter(
        @Path("id") id: Int,
        @Body body: AddCenterRequest
    ): Call<ApiResponse>

    @DELETE("api/centers/{id}") // Make sure this endpoint is correct
    fun deleteCenter(@Path("id") centerId: Int): Call<ApiResponse>

    // In your ApiService.kt or equivalent file

    @GET("your/centers/endpoint")
    fun getCenters(@Header("Authorization") token: String): Call<CentersResponse>

    @GET("api/admin/teachers/count")
    fun getTeacherCount(): Call<CountResponse>

}
