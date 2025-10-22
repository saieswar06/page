package com.example.page.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Defines all the network API endpoints for the application.
 * The Authorization header is added automatically by the RetrofitClient Interceptor.
 */
interface ApiService {

    // ✅ AUTHENTICATION
    // A single, unified login endpoint for all user types.
    @POST("auth/login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    // Optional: Google Sign-In endpoint.
    @POST("auth/google-login")
    fun googleLogin(@Body body: Map<String, String>): Call<LoginResponse>


    // ✅ CENTERS API (For Admins)
    // Gets a list of all anganwadi centers.
    @GET("api/admin/centers")
    fun getCenters(): Call<CentersResponse>

    // Adds a new anganwadi center.
    @POST("api/admin/centers")
    fun addCenter(@Body body: AddCenterRequest): Call<ApiResponse>

    // Updates an existing anganwadi center.
    @PUT("api/admin/centers/{id}")
    fun updateCenter(
        @Path("id") id: Int,
        @Body body: AddCenterRequest
    ): Call<ApiResponse>

    // Deletes an anganwadi center by its ID.
    @DELETE("api/centers/{id}")
    fun deleteCenter(@Path("id") centerId: Int): Call<ApiResponse>


    // ✅ DASHBOARD API
    // Gets the total count of teachers for the dashboard.
    @GET("api/admin/teachers/count")
    fun getTeacherCount(): Call<CountResponse>

    @GET("api/admin/centers/{id}")
    fun getCenterDetails(@Path("id") centerId: Int): Call<CenterDetailsResponse> // We'll create CenterDetailsResponse next

}
