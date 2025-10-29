package com.example.page.api

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ------------------------
    // 🔐 AUTHENTICATION
    // ------------------------
    @POST("api/auth/login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    @POST("api/auth/google-login")
    fun googleLogin(@Body body: Map<String, String>): Call<LoginResponse>

    // ------------------------
    // 🏫 CENTERS (Admin)
    // ------------------------
    @GET("api/admin/centers")
    fun getCenters(@Query("page") page: Int? = null, @Query("status") status: Int? = null): Call<ApiResponse<List<CenterResponse>>>

    @GET("api/admin/centers/{id}")
    fun getCenterDetails(@Path("id") id: Int): Call<CenterDetailsResponse>

    @POST("api/admin/centers")
    suspend fun addCenter(@Body body: AddCenterRequest): Response<ApiResponse<Any>>

    @PUT("api/admin/centers/{id}")
    fun updateCenter(
        @Path("id") id: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Call<ApiResponse<Any>>

    @HTTP(method = "DELETE", path = "api/admin/centers/{id}", hasBody = true)
    fun deleteCenter(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Call<ApiResponse<Any>>

    @PATCH("api/admin/centers/{id}/deactivate")
    fun deactivateCenter(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Call<ApiResponse<Any>>

    @PATCH("api/admin/centers/{id}/restore")
    fun restoreCenter(@Path("id") id: Int, @Body body: Map<String, String>): Call<ApiResponse<Any>>

    // ------------------------
    // 👩‍🏫 TEACHERS MANAGEMENT
    // ------------------------
    @GET("api/admin/teachers")
    fun getTeachers(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null,
        @Query("sort") sort: String? = null,
        @Query("status") status: List<Int>? = null
    ): Call<ApiResponse<List<TeacherModel>>>

    @POST("api/admin/teachers")
    fun addTeacher(@Body teacher: AddTeacherRequest): Call<ApiResponse<Any>>

    @PUT("api/admin/teachers/{id}")
    fun updateTeacher(
        @Path("id") id: Int,
        @Body teacher:  Map<String, @JvmSuppressWildcards Any>
    ): Call<ApiResponse<Any>>

    @HTTP(method = "DELETE", path = "api/admin/teachers/{id}", hasBody = true)
    fun deleteTeacher(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Call<ApiResponse<Any>>

    @PATCH("api/admin/teachers/{id}/deactivate")
    fun deactivateTeacher(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Call<ApiResponse<Any>>

    @PATCH("api/admin/teachers/{id}/restore")
    fun restoreTeacher(@Path("id") id: Int, @Body body: Map<String, String>): Call<ApiResponse<Any>>

    // ------------------------
    // 📜 ACTIVITY LOG
    // -------------------------
    @GET("api/admin/activity-logs")
    fun getAllActivityLogs(): Call<ApiResponse<List<ActivityLog>>>

    @GET("api/admin/centers/{id}/activity-log")
    fun getCenterActivityLog(@Path("id") id: Int): Call<ApiResponse<List<ActivityLog>>>

    @GET("api/admin/teachers/{id}/activity-log")
    fun getTeacherActivityLog(@Path("id") id: Int): Call<ApiResponse<List<ActivityLog>>>
}