package com.example.page

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.page.admin.centers.AdminCentersActivity
import com.example.page.databinding.ActivityDashboardBinding
import com.google.android.material.navigation.NavigationView

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Drawer
        setSupportActionBar(binding.toolbar)
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        binding.menuIcon.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Set user info dynamically (if saved in SharedPreferences)
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val mobile = prefs.getString("mobile", "User")
        binding.userDetails.text = "Welcome, $mobile"

        // Profile icon click
        binding.profileIcon.setOnClickListener {
            Toast.makeText(this, "Profile page coming soon", Toast.LENGTH_SHORT).show()
        }

        // Card clicks
        binding.cardActiveCenters.setOnClickListener {
            val intent = Intent(this, AdminCentersActivity::class.java)
            intent.putExtra("show_active", true)
            startActivity(intent)
        }

        binding.cardInactiveCenters.setOnClickListener {
            val intent = Intent(this, AdminCentersActivity::class.java)
            intent.putExtra("show_active", false)
            startActivity(intent)
        }

        binding.cardAddCenter.setOnClickListener {
            startActivity(Intent(this, com.example.page.admin.centers.AddCenterActivity::class.java))
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
