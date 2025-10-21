package com.example.page.api

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {

    @Volatile
    private var retrofit: Retrofit? = null

    // ✅ Matches your backend running locally
    private const val BASE_URL = "http://10.0.2.2:3000/"

    fun getInstance(context: Context): ApiService {
        if (retrofit == null) {
            synchronized(this) {
                if (retrofit == null) {
                    retrofit = buildRetrofit(context)
                }
            }
        }
        return retrofit!!.create(ApiService::class.java)
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)

        val logging = HttpLoggingInterceptor { msg -> Log.d("RetrofitLog", msg) }
        logging.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(Interceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")

                token?.let {
                    builder.header("Authorization", "Bearer $it")
                }

                try {
                    val resp = chain.proceed(builder.build())
                    if (resp.code == 401) Log.w("RetrofitClient", "Unauthorized")
                    resp
                } catch (e: IOException) {
                    Log.e("RetrofitClient", "Network error", e)
                    throw e
                }
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun clearInstance() {
        retrofit = null
    }
}
