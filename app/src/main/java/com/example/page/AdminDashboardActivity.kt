package com.example.page

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.admin.centers.AdminCentersActivity
import com.example.page.api.CentersResponse
import com.example.page.api.RetrofitClient
// ✅ **Import the ViewBinding class for this activity**
import com.example.page.databinding.ActivityAdminDashboardBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminDashboardActivity : AppCompatActivity() {

    // =========================== THE FIX IS HERE ===========================
    // Use a single 'binding' variable for all views.
    private lateinit var binding: ActivityAdminDashboardBinding
    // =====================================================================

    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // =========================== THE FIX IS HERE ===========================
        // 1. Inflate the layout using the binding class.
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        // 2. Set the content view to the root of the binding.
        setContentView(binding.root)
        // 3. `findViewById` is no longer needed.
        // =====================================================================

        // --- Authentication Check ---
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        token = prefs.getString("token", null)
        val email = prefs.getString("email", "Admin")

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

        // --- Card Listeners (using the 'binding' object) ---
        binding.cardCenters.setOnClickListener {
            startActivity(Intent(this, AdminCentersActivity::class.java))
        }

        binding.cardECCEUsers.setOnClickListener {
            Toast.makeText(this, "ECCE Users feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.cardProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.cardLogout.setOnClickListener {
            // Clear session and log out
            prefs.edit().clear().apply()
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

        RetrofitClient.getInstance(this)
            .getCenters()
            .enqueue(object : Callback<CentersResponse> {
                override fun onResponse(call: Call<CentersResponse>, response: Response<CentersResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val count = response.body()?.data?.size ?: 0
                        // Use the binding object to access the TextView
                        binding.tvCentersCount.text = count.toString()
                    } else {
                        Log.e("AdminDashboard", "Failed to get centers count: ${response.code()}")
                        binding.tvCentersCount.text = "0" // Default to 0 on error
                    }
                }

                override fun onFailure(call: Call<CentersResponse>, t: Throwable) {
                    Log.e("AdminDashboard", "Network error getting centers count", t)
                    binding.tvCentersCount.text = "!" // Indicate a network error
                }
            })
    }
}
