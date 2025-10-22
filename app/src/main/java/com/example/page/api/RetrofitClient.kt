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

object RetrofitClient {

    @Volatile
    private var retrofit: Retrofit? = null

    // ✅ Local backend (for Android emulator)
    private const val BASE_URL = "http://10.0.2.2:3000/"

    fun getInstance(context: Context): ApiService {
        // We now rebuild the instance every time to ensure the latest token is used.
        // This is less efficient but more robust for this specific token handling setup.
        retrofit = buildRetrofit(context)
        return retrofit!!.create(ApiService::class.java)
    }

    private fun buildRetrofit(context: Context): Retrofit {
        // ✅ Use the correct preference file: UserSession
        val prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)

        val logging = HttpLoggingInterceptor { msg -> Log.d("RetrofitLog", msg) }
        logging.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(Interceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")

                // ✅ Add the authorization token to every request if it exists
                if (token != null) {
                    requestBuilder.header("Authorization", "Bearer $token")
                    Log.d("RetrofitClient", "Authorization token added.")
                } else {
                    Log.w("RetrofitClient", "Authorization token is NULL.")
                }

                val request = requestBuilder.build()
                try {
                    val response = chain.proceed(request)
                    if (response.code == 401) {
                        Log.e("RetrofitClient", "Authorization failed with a 401 error. The token is likely invalid or expired.")
                    }
                    response
                } catch (e: IOException) {
                    Log.e("RetrofitClient", "A network error occurred: ${e.message}", e)
                    throw e
                }
            })
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

    /**
     * Clears the Retrofit instance. This should be called on logout
     * to ensure the old token is not reused.
     */
    fun clearInstance() {
        retrofit = null
    }
}