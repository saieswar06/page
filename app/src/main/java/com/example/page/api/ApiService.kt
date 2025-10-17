package com.example.page.api

import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @GET("api/admin/centers")
    fun getCenters(): Call<List<Center>>

    @Headers("Content-Type: application/json")
    @POST("api/admin/centers")
    fun addCenter(@Body body: AddCenterRequest): Call<Void>

    @Headers("Content-Type: application/json")
    @PUT("api/admin/centers/{id}")
    fun updateCenter(@Path("id") id: Int, @Body body: AddCenterRequest): Call<Void>

    @DELETE("api/admin/centers/{id}")
    fun deleteCenter(@Path("id") id: Int): Call<Void>

    @Headers("Content-Type: application/json")
    @POST("auth/login")
    fun login(@Body body: LoginRequest): Call<LoginResponse>

    @Headers("Content-Type: application/json")
    @POST("auth/google")
    fun googleLogin(@Body body: Map<String, String>): Call<LoginResponse>
}
