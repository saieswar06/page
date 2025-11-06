package com.example.page.api

import retrofit2.http.*
import retrofit2.Response

/**
 * Defines the API endpoints for the application.
 */
interface ApiService {

    /**
     * Logs in a user.
     */
    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    /**
     * Changes the user's password.
     */
    @POST("api/auth/change-password")
    suspend fun changePassword(@Body changePasswordRequest: ChangePasswordRequest): Response<ApiResponse<Any>>

    /**
     * Requests a password reset for the user.
     */
    @POST("api/auth/request-password-reset")
    suspend fun requestPasswordReset(@Body requestPasswordResetRequest: RequestPasswordResetRequest): Response<ApiResponse<Any>>

    /**
     * Verifies the OTP and resets the password.
     */
    @POST("api/auth/verify-otp-reset-password")
    suspend fun verifyOTPAndResetPassword(@Body verifyOtpRequest: VerifyOtpRequest): Response<ApiResponse<Any>>

    /**
     * Gets the current user's profile information.
     */
    @GET("api/auth/profile")
    suspend fun getCurrentUser(): Response<ApiResponse<UserDto>>

    /**
     * Updates the user's profile.
     */
    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body updateProfileRequest: UpdateProfileRequest): Response<ApiResponse<UserDto>>

    /**
     * Uploads the user's profile picture.
     */
    @POST("api/auth/profile-picture")
    suspend fun uploadProfilePicture(@Body profilePictureUrl: ProfilePictureUrl): Response<ApiResponse<ProfilePictureUrl>>

    @GET("api/admin/activity-logs")
    suspend fun getAllActivityLogs(): Response<ApiResponse<List<ActivityLog>>>

    @GET("api/admin/activity-logs/center/{centerId}")
    suspend fun getCenterActivityLog(@Path("centerId") centerId: Int): Response<ApiResponse<List<ActivityLog>>>

    @GET("api/admin/activity-logs/teacher/{teacherId}")
    suspend fun getTeacherActivityLog(@Path("teacherId") teacherId: Int): Response<ApiResponse<List<ActivityLog>>>

    @GET("api/admin/centers")
    suspend fun getCenters(@Query("status") status: Int): Response<ApiResponse<List<CenterResponse>>>

    @GET("api/admin/centers/{id}")
    suspend fun getCenterDetails(@Path("id") centerId: String): Response<ApiResponse<CenterDetails>>

    @POST("api/admin/centers")
    suspend fun addCenter(@Body addCenterRequest: AddCenterRequest): Response<ApiResponse<Center>>

    @PUT("api/admin/centers/{centerId}")
    suspend fun updateCenter(@Path("centerId") centerId: String, @Body center: AddCenterRequest): Response<ApiResponse<Center>>

    @PUT("api/admin/centers/{centerId}/deactivate")
    suspend fun deactivateCenter(@Path("centerId") centerId: String, @Body reason: DeactivateCenterRequest): Response<ApiResponse<Any>>

    @PUT("api/admin/centers/{centerId}/restore")
    suspend fun restoreCenter(@Path("centerId") centerId: String): Response<ApiResponse<Any>>

    @DELETE("api/admin/centers/{centerId}")
    suspend fun deleteCenter(@Path("centerId") centerId: String): Response<ApiResponse<Any>>

    @GET("api/admin/teachers")
    suspend fun getTeachers(@Query("status") status: Int): Response<ApiResponse<List<TeacherModel>>>

    @POST("api/admin/teachers")
    suspend fun addTeacher(@Body addTeacherRequest: AddTeacherRequest): Response<ApiResponse<TeacherModel>>

    @PUT("api/admin/teachers/{teacherId}")
    suspend fun updateTeacher(@Path("teacherId") teacherId: String, @Body teacher: UpdateTeacherRequest): Response<ApiResponse<TeacherModel>>

    @PUT("api/admin/teachers/{teacherId}/deactivate")
    suspend fun deactivateTeacher(@Path("teacherId") teacherId: String, @Body reason: DeactivateCenterRequest): Response<ApiResponse<Any>>

    @PUT("api/admin/teachers/{teacherId}/restore")
    suspend fun restoreTeacher(@Path("teacherId") teacherId: String): Response<ApiResponse<Any>>

    @DELETE("api/admin/teachers/{teacherId}")
    suspend fun deleteTeacher(@Path("teacherId") teacherId: String): Response<ApiResponse<Any>>
}
