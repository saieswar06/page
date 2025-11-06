package com.example.page

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityAdminDashboardBinding
import com.example.page.admin.centers.AdminCentersActivity
import com.example.page.teacher.TeacherDashboardActivity
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private var isActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkSession()
        setupUI()
        loadDashboardCounts()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardCounts()
    }

    private fun setupUI() {
        binding.btnMenu.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }

        binding.navView.setNavigationItemSelectedListener { item ->
            handleNavigation(item)
        }

        binding.chipGroupStatus.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                isActive = checkedId == R.id.chip_active
                Log.d("Dashboard", "Toggle changed - isActive: $isActive")
                loadDashboardCounts()
                updateCardLabels()
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                finish()
            }
        }

        binding.cardCenters.setOnClickListener {
            val intent = Intent(this, AdminCentersActivity::class.java)
            intent.putExtra("show_active", isActive)
            startActivity(intent)
        }
        binding.cardTeachers.setOnClickListener {
            val intent = Intent(this, TeacherDashboardActivity::class.java)
            intent.putExtra("show_active", isActive)
            startActivity(intent)
        }
        binding.btnRefresh.setOnClickListener {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show()
            loadDashboardCounts()
        }

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, AdminProfileActivity::class.java))
        }

        // Initialize labels
        updateCardLabels()
    }

    private fun updateCardLabels() {
        if (isActive) {
            binding.tvCentersTitle.text = "Anganwadi Centers (Active)"
            binding.tvTotalCentersLabel.text = "Total Centers (active)"
            binding.tvTeachersTitle.text = "ECCE Teachers (Active)"
            binding.tvTotalTeachersLabel.text = "Total Teachers (active)"
        } else {
            binding.tvCentersTitle.text = "Anganwadi Centers (Inactive)"
            binding.tvTotalCentersLabel.text = "Total Centers (inactive)"
            binding.tvTeachersTitle.text = "ECCE Teachers (Inactive)"
            binding.tvTotalTeachersLabel.text = "Total Teachers (inactive)"
        }
    }

    private fun checkSession() {
        val token = getSharedPreferences("UserSession", MODE_PRIVATE).getString("token", null)
        if (token.isNullOrEmpty()) {
            showToast("Session expired. Please log in again.")
            startActivity(Intent(this, SupervisorLoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun loadDashboardCounts() {
        binding.tvCentersCount.text = "..."
        binding.tvTeachersCount.text = "..."

        val api = RetrofitClient.getInstance(this)
        val status = if (isActive) 1 else 2

        lifecycleScope.launch {
            try {
                // Fetch center count
                val centersResponse = api.getCenters(status = status)
                if (centersResponse.isSuccessful && centersResponse.body()?.success == true) {
                    val count = centersResponse.body()?.data?.size ?: 0
                    binding.tvCentersCount.text = count.toString()
                } else {
                    binding.tvCentersCount.text = "0"
                }

                // Fetch teacher count
                val teachersResponse = api.getTeachers(status = status)
                if (teachersResponse.isSuccessful && teachersResponse.body()?.success == true) {
                    val count = teachersResponse.body()?.data?.size ?: 0
                    binding.tvTeachersCount.text = count.toString()
                } else {
                    binding.tvTeachersCount.text = "0"
                }
            } catch (t: Throwable) {
                binding.tvCentersCount.text = "0"
                binding.tvTeachersCount.text = "0"
                showToast("Failed to load counts: ${t.message}")
            }
        }
    }


    private fun handleNavigation(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home, R.id.nav_dashboard -> showToast("Already at Dashboard")
            R.id.nav_activity_log -> {
                startActivity(Intent(this, ActivityLogActivity::class.java))
            }
            R.id.nav_resource -> showToast("Resources coming soon")
            R.id.nav_helpdesk -> showToast("Helpdesk coming soon")
            R.id.nav_faq -> showToast("FAQ coming soon")
            R.id.nav_calculator -> showToast("Calculator coming soon")
            R.id.nav_logout -> logoutUser()
            else -> return false
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logoutUser() {
        getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply()
        RetrofitClient.clearInstance()
        showToast("Logged out successfully")
        startActivity(Intent(this, SupervisorLoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
