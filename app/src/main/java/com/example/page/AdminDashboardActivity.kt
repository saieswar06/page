package com.example.page

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.page.admin.centers.AdminCentersActivity
import com.example.page.databinding.ActivityAdminDashboardBinding
import com.google.android.material.navigation.NavigationView

class AdminDashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Setup Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ✅ Drawer toggle setup
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // ✅ Navigation menu listener
        binding.navView.setNavigationItemSelectedListener(this)

        // ✅ Refresh button
        binding.btnRefresh.setOnClickListener {
            Toast.makeText(this, "Refreshing dashboard...", Toast.LENGTH_SHORT).show()
            loadDashboardData()
        }

        // ✅ Anganwadi Centers card → Open Centers page
        binding.tvCentersTitle.setOnClickListener {
            startActivity(Intent(this, AdminCentersActivity::class.java))
        }

        // ✅ Teachers card
        binding.tvTeachersTitle.setOnClickListener {
            Toast.makeText(this, "Teachers page coming soon!", Toast.LENGTH_SHORT).show()
        }

        // ✅ Load dashboard data
        loadDashboardData()

        // ✅ Handle back press for drawer
        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                finish()
            }
        }
    }

    private fun loadDashboardData() {
        // TODO: replace with API calls
        binding.tvCentersCount.text = "2"
        binding.tvTeachersCount.text = "10"
    }

    // ✅ Navigation Drawer Item Selection
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> showToast("Home selected")
            R.id.nav_dashboard -> showToast("Dashboard selected")
            R.id.nav_resource -> showToast("Resources coming soon")
            R.id.nav_helpdesk -> showToast("Helpdesk selected")
            R.id.nav_faq -> showToast("FAQs selected")
            R.id.nav_calculator -> showToast("Calculator coming soon")
            else -> showToast("Unknown option")
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }
}
