package com.example.page.api

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Response

object RetrofitClient {

    @Volatile
    private var retrofit: Retrofit? = null
    private var lastUsedToken: String? = null

    private const val BASE_URL = "http://10.0.2.2:3000/"

    fun getInstance(context: Context): ApiService {
        return getClient(context).create(ApiService::class.java)
    }
    fun getInstanceteacher(context: Context): ApiService {
        val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val currentToken = prefs.getString("token", null)

        // Rebuild Retrofit if:
        // 1️⃣ first call, or 2️⃣ token changed since last time
        if (retrofit == null || currentToken != lastUsedToken) {
            synchronized(this) {
                if (retrofit == null || currentToken != lastUsedToken) {
                    retrofit = buildRetrofitteacher(context)
                    lastUsedToken = currentToken
                    Log.d("RetrofitClient", "🔄 Retrofit rebuilt with updated token.")
                }
            }
        }

        return retrofit!!.create(ApiService::class.java)
    }

    private fun getClient(context: Context): Retrofit {
        // This thread-safe singleton implementation is correct.
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(context).also { retrofit = it }
        }
    }

    private fun buildRetrofit(context: Context): Retrofit {
        Log.d("RetrofitClient", "Building new Retrofit instance...")
        val prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)

        val logging = HttpLoggingInterceptor { msg -> Log.d("RetrofitLog", msg) }
        logging.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            // =========================== THE DEFINITIVE FIX ===========================
            // The Interceptor MUST read the token from SharedPreferences on every call.
            .addInterceptor(Interceptor { chain ->
                // Get the latest token from SharedPreferences for every request.
                val currentToken = prefs.getString("token", null)

                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")

                if (currentToken != null) {
                    // Use the fresh token from SharedPreferences.
                    requestBuilder.header("Authorization", "Bearer $currentToken")
                    Log.d("RetrofitClient", "✅ Authorization token added to request.")
                } else {
                    Log.w("RetrofitClient", "⚠️ Authorization token is NULL. (This is expected for login requests).")
                }

                val request = requestBuilder.build()
                chain.proceed(request)
                // The try-catch block for the response was removed for simplicity,
                // as error handling is done in the enqueue() callbacks.
            })
            // ===========================================================================
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
    private fun buildRetrofitteacher(context: Context): Retrofit {
        val logging = HttpLoggingInterceptor { message ->
            Log.d("RetrofitLog", message)
        }.apply {
            // Use BODY level only for debugging, switch to NONE in release builds
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .addInterceptor(logging)
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

    /**
     * Clears the Retrofit instance. This MUST be called on logout.
     */
    fun clearInstance() {
        Log.d("RetrofitClient", "Clearing Retrofit instance.")
        retrofit = null
        lastUsedToken=null
    }
    private class AuthInterceptor(private val context: Context) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            val token = prefs.getString("token", null)

            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")

            token?.let {
                val formattedToken = if (it.startsWith("Bearer ")) it else "Bearer $it"
                requestBuilder.header("Authorization", formattedToken)
            }

            val request = requestBuilder.build()

            return try {
                val response = chain.proceed(request)
                if (response.code == 401) {
                    Log.w("RetrofitClient", "⚠️ Unauthorized (401) → Token expired or invalid.")
                }
                response
            } catch (e: IOException) {
                Log.e("RetrofitClient", "🚨 Network error: ${e.message}", e)
                throw e
            }
        }
    }
}
