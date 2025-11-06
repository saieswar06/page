package com.example.page

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.page.admin.centers.AdminCentersActivity
import com.example.page.databinding.ActivityAdminDashboardBinding
import com.google.android.material.navigation.NavigationView

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Drawer
        setSupportActionBar(binding.root.findViewById(R.id.toolbar))
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Set user info dynamically (if saved in SharedPreferences)
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val mobile = prefs.getString("mobile", "User")
        val userDetailsTextView = binding.navView.getHeaderView(0).findViewById<TextView>(R.id.user_details)
        userDetailsTextView.text = "Welcome, $mobile"

        // Profile icon click
        binding.btnProfile.setOnClickListener {
            Toast.makeText(this, "Profile page coming soon", Toast.LENGTH_SHORT).show()
        }

        // Card clicks
        binding.cardCenters.setOnClickListener {
            val intent = Intent(this, AdminCentersActivity::class.java)
            intent.putExtra("show_active", binding.chipActive.isChecked)
            startActivity(intent)
        }

        binding.cardTeachers.setOnClickListener {
            // You can create a similar activity for teachers
            Toast.makeText(this, "Teachers page coming soon", Toast.LENGTH_SHORT).show()
        }

        // Chip group listener
        binding.chipGroupStatus.setOnCheckedChangeListener { group, checkedId ->
            val status = if (checkedId == R.id.chip_active) "Active" else "Inactive"
            val totalStatus = if (checkedId == R.id.chip_active) "(active)" else "(inactive)"

            binding.tvCentersTitle.text = "Anganwadi Centers ($status)"
            binding.tvTotalCentersLabel.text = "Total Centers $totalStatus"

            binding.tvTeachersTitle.text = "ECCE Teachers ($status)"
            binding.tvTotalTeachersLabel.text = "Total Teachers $totalStatus"
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_activity_log -> {
                startActivity(Intent(this, ActivityLogActivity::class.java))
            }
            // Add other drawer item clicks here
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
