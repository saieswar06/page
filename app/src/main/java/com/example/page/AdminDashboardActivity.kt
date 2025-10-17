package com.example.page

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
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

        // ✅ Setup Drawer Toggle
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        // ✅ Set Navigation Listener
        binding.navView.setNavigationItemSelectedListener(this)

        // ✅ Refresh Button
        binding.btnRefresh.setOnClickListener {
            Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show()
            loadDashboardData()
        }

        // ✅ Card Click Actions
        binding.tvCentersTitle.setOnClickListener {
            Toast.makeText(this, "Navigating to Centers", Toast.LENGTH_SHORT).show()
            // Example: startActivity(Intent(this, AnganwadiCentersActivity::class.java))
        }

        binding.tvTeachersTitle.setOnClickListener {
            Toast.makeText(this, "Navigating to Teachers", Toast.LENGTH_SHORT).show()
        }

        // ✅ Load Dashboard Data Initially
        loadDashboardData()

        // ✅ Modern back button handling
        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                finish() // closes the activity safely
            }
        }
    }

    private fun loadDashboardData() {
        // TODO: Replace with API call later
        binding.tvCentersCount.text = "12"
        binding.tvTeachersCount.text = "58"
    }

    // ✅ Handle Navigation Drawer Menu Clicks
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> showToast("Home selected")
            R.id.nav_dashboard -> showToast("Dashboard selected")
            R.id.nav_resource -> showToast("Opening Resources")
            R.id.nav_helpdesk -> showToast("Helpdesk selected")
            R.id.nav_faq -> showToast("Frequently Asked Questions")
            R.id.nav_calculator -> showToast("Calculator coming soon")
            else -> showToast("Unknown option")
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // ✅ Handle Drawer Toggle
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }
}
