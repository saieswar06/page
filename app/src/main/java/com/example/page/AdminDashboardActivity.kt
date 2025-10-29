package com.example.page

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.page.api.ApiResponse
import com.example.page.api.CenterResponse
import com.example.page.api.RetrofitClient
import com.example.page.api.TeacherModel
import com.example.page.databinding.ActivityAdminDashboardBinding
import com.example.page.admin.centers.AdminCentersActivity
import com.example.page.teacher.TeacherDashboardActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private var isActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkSession()
        setupUI()
        loadDashboardCounts(isActive)
    }

    override fun onResume() {
        super.onResume()
        loadDashboardCounts(isActive)
    }

    private fun setupUI() {
        binding.btnMenu.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }

        binding.navView.setNavigationItemSelectedListener { item ->
            handleNavigation(item)
        }

        binding.toggleStatus.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isActive = checkedId == R.id.btn_active
                Log.d("Dashboard", "Toggle changed - isActive: $isActive, status: ${if (isActive) 1 else 0}")

                // Reset counts to loading state
                binding.tvCentersCount.text = "..."
                binding.tvTeachersCount.text = "..."

                loadDashboardCounts(isActive)
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
            binding.tvCentersCount.text = "..."
            binding.tvTeachersCount.text = "..."
            loadDashboardCounts(isActive)
        }

        binding.btnProfile.setOnClickListener { showProfileDialog() }

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

    private fun showProfileDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.activity_admin_profile)

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val email = prefs.getString("email", "admin@example.com")
        val name = prefs.getString("name", email)

        dialog.findViewById<TextView>(R.id.tv_admin_name).text = name
        dialog.findViewById<TextView>(R.id.tv_admin_email).text = email

        dialog.findViewById<Button>(R.id.btn_logout).setOnClickListener {
            logoutUser()
            dialog.dismiss()
        }

        dialog.show()
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

    private fun loadDashboardCounts(active: Boolean) {
        val api = RetrofitClient.getInstance(this)
        val status = if (active) 1 else 0

        Log.d("Dashboard", "Loading counts with status: $status (active: $active)")

        // Load Centers Count
        api.getCenters(status = status).enqueue(object : Callback<ApiResponse<List<CenterResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<CenterResponse>>>,
                response: Response<ApiResponse<List<CenterResponse>>>
            ) {
                Log.d("Dashboard", "Centers API Response - Code: ${response.code()}, Success: ${response.body()?.success}")

                if (response.isSuccessful && response.body()?.success == true) {
                    val count = response.body()?.data?.size ?: 0
                    binding.tvCentersCount.text = count.toString()
                    Log.d("Dashboard", "Centers count updated: $count (status: $status)")
                } else {
                    binding.tvCentersCount.text = "0"
                    val errorBody = response.errorBody()?.string()
                    Log.e("Dashboard", "Centers API error: ${response.code()} - $errorBody")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<CenterResponse>>>, t: Throwable) {
                binding.tvCentersCount.text = "0"
                Log.e("Dashboard", "Centers API failed: ${t.message}", t)
                showToast("Failed to load centers count")
            }
        })

        // Load Teachers Count
        api.getTeachers(status = status).enqueue(object : Callback<ApiResponse<List<TeacherModel>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<TeacherModel>>>,
                response: Response<ApiResponse<List<TeacherModel>>>
            ) {
                Log.d("Dashboard", "Teachers API Response - Code: ${response.code()}, Success: ${response.body()?.success}")

                if (response.isSuccessful && response.body()?.success == true) {
                    val count = response.body()?.data?.size ?: 0
                    binding.tvTeachersCount.text = count.toString()
                    Log.d("Dashboard", "Teachers count updated: $count (status: $status)")
                } else {
                    binding.tvTeachersCount.text = "0"
                    val errorBody = response.errorBody()?.string()
                    Log.e("Dashboard", "Teachers API error: ${response.code()} - $errorBody")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<TeacherModel>>>, t: Throwable) {
                binding.tvTeachersCount.text = "0"
                Log.e("Dashboard", "Teachers API failed: ${t.message}", t)
                showToast("Failed to load teachers count")
            }
        })
    }

    private fun handleNavigation(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home, R.id.nav_dashboard -> showToast("Already at Dashboard")
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