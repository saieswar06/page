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
        try {
            binding = ActivityDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            Log.e("DashboardActivity", "Layout binding failed", e)
            Toast.makeText(this, "UI initialization failed", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // ✅ Load user info safely
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val name = prefs.getString("name", "Unknown")
        val email = prefs.getString("email", "N/A")

        binding.userDetails?.text = "Welcome, $name\n$email"

        // ✅ Safe null checks for tablet layouts
        binding.profileIcon?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.cardBeneficiaries?.setOnClickListener {
            Toast.makeText(this, "Beneficiaries clicked", Toast.LENGTH_SHORT).show()
        }

        binding.cardTracking?.setOnClickListener {
            Toast.makeText(this, "Daily Tracking clicked", Toast.LENGTH_SHORT).show()
        }

        binding.cardHome?.setOnClickListener {
            Toast.makeText(this, "Home Visits clicked", Toast.LENGTH_SHORT).show()
        }

        val bottomNav: BottomNavigationView? = binding.bottomNavigation
        bottomNav?.selectedItemId = R.id.nav_home
        bottomNav?.setOnItemSelectedListener {
            Toast.makeText(this, "Feature coming soon!", Toast.LENGTH_SHORT).show()
            true
        }
    }
}
