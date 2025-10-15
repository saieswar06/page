package com.example.page

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.databinding.ActivityDashboardBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            // ✅ Load user data from SharedPreferences (saved during login)
            val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
            val name = prefs.getString("name", "Unknown")
            val email = prefs.getString("email", "N/A")

            // ✅ Display logged-in user info
            binding.userDetails.text = "Welcome, $name\n$email"

            // ✅ Card click listeners
            binding.cardBeneficiaries.setOnClickListener { showToast("Beneficiaries clicked") }
            binding.cardTracking.setOnClickListener { showToast("Daily Tracking clicked") }
            binding.cardHome.setOnClickListener { showToast("Home Visits clicked") }

            // ✅ Profile icon → open ProfileActivity
            binding.profileIcon.setOnClickListener {
                startActivity(Intent(this, ProfileActivity::class.java))
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            }

            // ✅ Bottom navigation
            val bottomNav: BottomNavigationView = binding.bottomNavigation
            bottomNav.selectedItemId = R.id.nav_home
            bottomNav.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> showToast("Home")
                    R.id.nav_beneficiaries -> showToast("Beneficiaries")
                    R.id.nav_tracking -> showToast("Tracking")
                    R.id.nav_visits -> showToast("Visits")
                    R.id.nav_more -> showToast("More Options")
                }
                true
            }

        } catch (e: Exception) {
            Log.e("DashboardActivity", "Error initializing dashboard", e)
            Toast.makeText(this, "Error loading dashboard", Toast.LENGTH_LONG).show()
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
