package com.example.page.api

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    @Volatile
    private var retrofit: Retrofit? = null

    private const val BASE_URL = "http://10.0.2.2:3000/"

    // Unified getInstance method
    fun getInstance(context: Context): ApiService {
        return getClient(context).create(ApiService::class.java)
    }

    // Remove getInstanceteacher and buildRetrofitteacher

    private fun getClient(context: Context): Retrofit {
        // Use double-checked locking for thread safety.
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(context).also { retrofit = it }
        }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        Log.d("RetrofitClient", "Building new Retrofit instance...")

        val logging = HttpLoggingInterceptor { msg -> Log.d("RetrofitLog", msg) }
        logging.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(createAuthInterceptor(context)) // Use a single, reliable auth interceptor
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun createAuthInterceptor(context: Context): Interceptor {
        return Interceptor { chain ->
            // Always get the latest token from the correct SharedPreferences file.
            val prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val token = prefs.getString("token", null)

            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")

            if (!token.isNullOrEmpty()) {
                requestBuilder.header("Authorization", "Bearer $token")
                Log.d("RetrofitClient", "✅ Authorization token added to request.")
            } else {
                Log.w("RetrofitClient", "⚠️ Authorization token is NULL.")
            }

            val request = requestBuilder.build()
            chain.proceed(request)
        }
    }

    /**
     * Clears the Retrofit instance. This MUST be called on logout.
     */
    fun clearInstance() {
        Log.d("RetrofitClient", "Clearing Retrofit instance.")
        retrofit = null
    }
}
