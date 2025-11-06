package com.example.page

import android.util.Log
import android.widget.Toast
import androidx.multidex.MultiDexApplication
import com.example.page.api.RetrofitClient

/**
 * Global Application class to capture uncaught exceptions and initialize global things.
 * Register this in AndroidManifest.xml as android:name=".App"
 */
class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Retrofit on a background thread
        RetrofitClient.initialize(this)

        // Global uncaught exception handler so crashes aren't silently swallowed.
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("UncaughtException", "Thread: ${thread.name}", throwable)
            } catch (_: Exception) { /* best effort logging */ }

            // Try to show a toast (safe fallback — won't crash)
            try {
                Toast.makeText(this, "App crashed: ${throwable.message}", Toast.LENGTH_LONG).show()
            } catch (_: Exception) { /* ignore */ }

            // Let system handle kill after logging
        }
    }
}
