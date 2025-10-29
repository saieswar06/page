package com.example.page

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Set user info dynamically (if saved in SharedPreferences)
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val mobile = prefs.getString("mobile", "")
        binding.userDetails.text = mobile ?: "User"

        // 👤 Profile icon click
        binding.profileIcon.setOnClickListener {
            Toast.makeText(this, "Profile page coming soon", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, ProfileActivity::class.java))
        }

        // 🔔 Notifications
        binding.notificationIcon.setOnClickListener {
            Toast.makeText(this, "Notifications feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // 👥 Card 1: Beneficiaries
        binding.cardBeneficiaries.setOnClickListener {
            Toast.makeText(this, "Opening Beneficiaries...", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, BeneficiariesActivity::class.java))
        }

        // 📅 Card 2: Daily Tracking
        binding.cardTracking.setOnClickListener {
            Toast.makeText(this, "Opening Daily Tracking...", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, DailyTrackingActivity::class.java))
        }

        // 🏠 Card 3: Home Visits
        binding.cardHome.setOnClickListener {
            Toast.makeText(this, "Opening Home Visits...", Toast.LENGTH_SHORT).show()
            // startActivity(Intent(this, HomeVisitsActivity::class.java))
        }

        // ⚙️ Bottom Navigation actions
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_reports -> {
                    Toast.makeText(this, "Reports feature coming soon", Toast.LENGTH_SHORT).show()
                    // startActivity(Intent(this, ReportsActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Settings feature coming soon", Toast.LENGTH_SHORT).show()
                    // startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
