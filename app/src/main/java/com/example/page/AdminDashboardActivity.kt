package com.example.page

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.page.admin.centers.AdminCentersActivity
import com.example.page.api.CentersResponse
import com.example.page.api.CountResponse
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityAdminDashboardBinding
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminDashboardActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Toolbar setup
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ✅ Drawer setup
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)

        // ✅ Token from shared preferences (same key as login)
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        token = prefs.getString("token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // ✅ Click: Refresh
        binding.btnRefresh.setOnClickListener {
            Toast.makeText(this, "Refreshing dashboard...", Toast.LENGTH_SHORT).show()
            loadDashboardData()
        }

        // ✅ Click: Centers card
        binding.cardCenters.setOnClickListener {
            startActivity(Intent(this, AdminCentersActivity::class.java))
        }

        // ✅ Load counts
        loadDashboardData()

        // ✅ Handle back press (close drawer first)
        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                finish()
            }
        }
    }

    // ---------------- LOAD DASHBOARD DATA ----------------
    private fun loadDashboardData() {
        loadCentersCount()
        loadTeachersCount()
    }

    private fun loadCentersCount() {
        RetrofitClient.getInstance(this)
            .getCenters("Bearer $token")
            .enqueue(object : Callback<CentersResponse> {
                override fun onResponse(
                    call: Call<CentersResponse>,
                    response: Response<CentersResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val centers = response.body()?.data ?: emptyList()
                        val count = centers.size
                        Log.d("Dashboard", "✅ Centers loaded: $count")

                        val old = binding.tvCentersCount.text.toString().toIntOrNull() ?: 0
                        animateCount(old, count) {
                            binding.tvCentersCount.text = it.toString()
                        }
                    } else {
                        Log.e("Dashboard", "❌ Failed to load centers. Code: ${response.code()}")
                        binding.tvCentersCount.text = "--"
                    }
                }

                override fun onFailure(call: Call<CentersResponse>, t: Throwable) {
                    Log.e("Dashboard", "⚠️ Centers load failed: ${t.message}", t)
                    binding.tvCentersCount.text = "--"
                }
            })
    }

    private fun loadTeachersCount() {
        RetrofitClient.getInstance(this)
            .getTeacherCount("Bearer $token")
            .enqueue(object : Callback<CountResponse> {
                override fun onResponse(
                    call: Call<CountResponse>,
                    response: Response<CountResponse>
                ) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val count = response.body()?.count ?: 0
                        Log.d("Dashboard", "✅ Teachers count: $count")

                        val old = binding.tvTeachersCount.text.toString().toIntOrNull() ?: 0
                        animateCount(old, count) {
                            binding.tvTeachersCount.text = it.toString()
                        }
                    } else {
                        Log.e("Dashboard", "❌ Failed to load teachers. Code: ${response.code()}")
                        binding.tvTeachersCount.text = "--"
                    }
                }

                override fun onFailure(call: Call<CountResponse>, t: Throwable) {
                    Log.e("Dashboard", "⚠️ Teachers load failed: ${t.message}", t)
                    binding.tvTeachersCount.text = "--"
                }
            })
    }

    // ---------------- ANIMATION ----------------
    private fun animateCount(from: Int, to: Int, updateText: (Int) -> Unit) {
        val animator = ValueAnimator.ofInt(from, to)
        animator.duration = 600
        animator.addUpdateListener { updateText(it.animatedValue as Int) }
        animator.start()
    }

    // ---------------- NAVIGATION MENU ----------------
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> showToast("Home selected")
            R.id.nav_dashboard -> showToast("Dashboard selected")
            R.id.nav_resource -> showToast("Resources coming soon")
            R.id.nav_helpdesk -> showToast("Helpdesk selected")
            R.id.nav_faq -> showToast("FAQs selected")
            R.id.nav_calculator -> showToast("Calculator coming soon")
            R.id.nav_logout -> logoutUser()
            else -> showToast("Unknown option")
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logoutUser() {
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE).edit()
        prefs.clear()
        prefs.apply()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }
}
