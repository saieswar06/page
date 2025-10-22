package com.example.page

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.page.admin.centers.AdminCentersActivity
import com.example.page.api.CentersResponse
import com.example.page.api.RetrofitClient
// Deleted the unused import: import com.example.page.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminDashboardActivity : AppCompatActivity() {

    private var token: String? = null
    private lateinit var tvCentersCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        tvCentersCount = findViewById(R.id.tv_centers_count)

        // --- Authentication Check ---
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        token = prefs.getString("token", null)
        val email = prefs.getString("email", "Admin") // Use email, not username

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, SupervisorLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        // --- Welcome Message ---
        Toast.makeText(this, "Welcome, $email!", Toast.LENGTH_SHORT).show()

        // --- Load Initial Data ---
        loadCentersCount()

        // --- Card Listeners ---
        findViewById<CardView>(R.id.cardCenters).setOnClickListener {
            startActivity(Intent(this, AdminCentersActivity::class.java))
        }

        findViewById<CardView>(R.id.cardECCEUsers).setOnClickListener {
            Toast.makeText(this, "ECCE Users feature coming soon", Toast.LENGTH_SHORT).show()
        }

        findViewById<CardView>(R.id.cardProfile).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent);
        }

        findViewById<CardView>(R.id.cardLogout).setOnClickListener {
            // Clear session and log out
            val editor = prefs.edit()
            editor.clear()
            editor.apply()

            // Also clear the Retrofit instance to remove the old token
            RetrofitClient.clearInstance()

            val intent = Intent(this, SupervisorLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the count whenever the activity is resumed
        loadCentersCount()
    }

    private fun loadCentersCount() {
        if (token.isNullOrEmpty()) {
            Log.e("AdminDashboard", "Cannot load centers count, token is missing.")
            return
        }

        // FIX: Call getCenters() with NO arguments. The token is handled automatically.
        RetrofitClient.getInstance(this)
            .getCenters()
            .enqueue(object : Callback<CentersResponse> {
                override fun onResponse(call: Call<CentersResponse>, response: Response<CentersResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val count = response.body()?.data?.size ?: 0
                        tvCentersCount.text = count.toString()
                    } else {
                        Log.e("AdminDashboard", "Failed to get centers count: ${response.code()}")
                        tvCentersCount.text = "0" // Default to 0 on error
                    }
                }

                override fun onFailure(call: Call<CentersResponse>, t: Throwable) {
                    Log.e("AdminDashboard", "Network error getting centers count", t)
                    tvCentersCount.text = "!" // Indicate a network error
                }
            })
    }
}
