package com.example.page

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat

import com.example.page.admin.centers.AdminCentersActivity
import com.example.page.api.CentersResponse
import com.example.page.api.CountResponse
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityAdminDashboardBinding
import com.example.page.teacher.TeacherDashboardActivity
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AdminDashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityAdminDashboardBinding
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Authentication Check ---
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        token = prefs.getString("token", null)
        val email = prefs.getString("email", "Admin")

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, SupervisorLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        binding.navView.setNavigationItemSelectedListener(this)

        // --- Welcome Message ---
        Toast.makeText(this, "Welcome, $email!", Toast.LENGTH_SHORT).show()

        // --- Load Initial Data ---
        loadCentersCount()
        loadTeachersCount()

        // --- Card Listeners ---
        binding.cardCenters.setOnClickListener {
            // ✅ FIX: Also pass the token to the Centers activity
            val intent = Intent(this, AdminCentersActivity::class.java)
            intent.putExtra("AUTH_TOKEN", token)
            startActivity(intent)
        }

        binding.cardECCEUsers.setOnClickListener {
            // ✅ FIX: You must pass the token to the next activity
            val intent = Intent(this, TeacherDashboardActivity::class.java)
            intent.putExtra("AUTH_TOKEN", token) // Add the token here
            startActivity(intent)
        }

        binding.cardProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            // If ProfileActivity needs authentication, pass the token here too
            intent.putExtra("AUTH_TOKEN", token)
            startActivity(intent)
        }

        binding.cardLogout.setOnClickListener {
            logoutUser()
        }
    }

    // ... (The rest of your AdminDashboardActivity code remains the same)
    // ...
    override fun onResume() {
        super.onResume()
        // Refresh the counts whenever the activity is resumed
        loadCentersCount()
        loadTeachersCount()
    }

    private fun loadCentersCount() {
        if (token.isNullOrEmpty()) {
            Log.e("AdminDashboard", "Cannot load centers count, token is missing.")
            return
        }

        RetrofitClient.getInstance(this)
            .getCenters()
            .enqueue(object : Callback<CentersResponse> {
                override fun onResponse(call: Call<CentersResponse>, response: Response<CentersResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val count = response.body()?.data?.size ?: 0
                        binding.tvCentersCount.text = count.toString()
                    } else {
                        Log.e("AdminDashboard", "Failed to get centers count: ${response.code()}")
                        binding.tvCentersCount.text = "0"
                    }
                }

                override fun onFailure(call: Call<CentersResponse>, t: Throwable) {
                    Log.e("AdminDashboard", "Network error getting centers count", t)
                    binding.tvCentersCount.text = "!"
                }
            })
    }
    private fun loadTeachersCount() {
        // Safe call for token to prevent crashes
        val currentToken = token
        if (currentToken.isNullOrEmpty()) {
            Log.e("AdminDashboard", "Cannot load teachers count, token is missing.")
            binding.tvTeachersCount.text = "--"
            return
        }

        RetrofitClient.getInstanceteacher(this).getTeacherCount(currentToken).enqueue(object : Callback<CountResponse> {
            override fun onResponse(call: Call<CountResponse>, response: Response<CountResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val count = response.body()?.count ?: 0
                    animateCount(0, count) { binding.tvTeachersCount.text = it.toString() }
                } else {
                    binding.tvTeachersCount.text = "--"
                }
            }

            override fun onFailure(call: Call<CountResponse>, t: Throwable) {
                binding.tvTeachersCount.text = "--"
                Log.e("AdminDashboard", "Network error getting teacher count", t)
            }
        })
    }
    private fun animateCount(from: Int, to: Int, updateText: (Int) -> Unit) {
        val animator = ValueAnimator.ofInt(from, to)
        animator.duration = 600
        animator.addUpdateListener { updateText(it.animatedValue as Int) }
        animator.start()
    }

    // This override is now valid because the interface is implemented
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> showToast("Home selected")
            R.id.nav_dashboard -> showToast("Dashboard selected")
            R.id.nav_logout -> logoutUser()
            else -> showToast("Coming soon")
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logoutUser() {
        // Use the correct SharedPreferences name to clear the session
        getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply()
        RetrofitClient.clearInstance() // Also clear the Retrofit instance
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, SupervisorLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
