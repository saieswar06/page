package com.example.page.api

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private var retrofit: Retrofit? = null

    fun getInstance(context: Context): ApiService {
        if (retrofit == null) {

            val prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val token = prefs.getString("token", null)

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(Interceptor { chain ->
                    val newRequest = chain.request().newBuilder().apply {
                        if (token != null) addHeader("Authorization", "Bearer $token")
                    }.build()
                    chain.proceed(newRequest)
                })
                .build()

            retrofit = Retrofit.Builder()
                // ⚠️ Emulator → 10.0.2.2 | Real Device → use your PC IP
                .baseUrl("http://10.0.2.2:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        }

        return retrofit!!.create(ApiService::class.java)
    }
}
