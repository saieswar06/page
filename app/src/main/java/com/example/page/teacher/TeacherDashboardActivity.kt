package com.example.page.teacher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.page.R
import com.example.page.api.RetrofitClient
import com.example.page.api.TeacherModel
import com.example.page.api.TeachersResponse
// Make sure you have the correct import for your SupervisorLoginActivity
import com.example.page.SupervisorLoginActivity
import com.example.page.databinding.ActivityTeacherDashboardBinding
import com.google.android.material.navigation.NavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TeacherDashboardActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityTeacherDashboardBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var teacherAdapter: TeacherAdapter
    private val teacherList = mutableListOf<TeacherModel>()

    // This will hold the token for the activity's lifecycle
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTeacherDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Get token from the Intent first
        token = intent.getStringExtra("AUTH_TOKEN")

        // ✅ Check if the token is valid IMMEDIATELY
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()

            // Redirect to the login screen
            val intent = Intent(this, SupervisorLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Important: finish this activity
            return   // Stop further execution of onCreate
        }

        // --- If token is valid, proceed with setting up the UI ---

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

        // ✅ Back button closes drawer first
        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                finish()
            }
        }

        // ✅ RecyclerView setup
        binding.rvTeachers.layoutManager = LinearLayoutManager(this)
        teacherAdapter = TeacherAdapter(teacherList)
        binding.rvTeachers.adapter = teacherAdapter

        // ✅ Button actions
        binding.btnMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.btnRefresh.setOnClickListener {
            Toast.makeText(this, "Refreshing teacher list...", Toast.LENGTH_SHORT).show()
            loadTeachers()
        }

        binding.btnAddTeacher.setOnClickListener {
            // Pass the token to the AddTeacherActivity as well
            val intent = Intent(this, AddTeacherActivity::class.java)
            intent.putExtra("AUTH_TOKEN", token)
            startActivity(intent)
        }

        // ✅ Initial data load
        loadTeachers()
    }

    /**
     * Fetch teacher list from backend
     */
    private fun loadTeachers() {
        // ⭐️ FIX: Use the class-level 'token' variable. Do NOT read from SharedPreferences here.
        if (token.isNullOrEmpty()) { // This check is now for safety
            Toast.makeText(this, "Session expired, please log in again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        // ⭐️ FIX: Use the class-level 'token' in the API call.
        RetrofitClient.getInstance(this)
            .getTeachers("Bearer $token")
            .enqueue(object : Callback<TeachersResponse> {
                override fun onResponse(
                    call: Call<TeachersResponse>,
                    response: Response<TeachersResponse>
                ) {
                    binding.progressBar.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.success == true) {
                        val teachers = response.body()?.data ?: emptyList()
                        teacherList.clear()
                        teacherList.addAll(teachers)
                        teacherAdapter.notifyDataSetChanged()

                        if (teacherList.isEmpty()) {
                            Toast.makeText(
                                this@TeacherDashboardActivity,
                                "No teachers found.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        Log.d("TeachersAPI", "✅ Loaded ${teacherList.size} teachers")

                    } else {
                        val msg = response.body()?.message ?: "Failed to fetch teachers"
                        Toast.makeText(
                            this@TeacherDashboardActivity,
                            "❌ $msg (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("TeachersAPI", "Error ${response.code()}: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<TeachersResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@TeacherDashboardActivity,
                        "🚨 Network error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("TeachersAPI", "Failure: ${t.message}")
                }
            })
    }

    /**
     * Navigation drawer item actions
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> showToast("Home selected")
            R.id.nav_dashboard -> showToast("Dashboard selected")
            R.id.nav_resource -> showToast("Resources coming soon")
            R.id.nav_helpdesk -> showToast("Helpdesk selected")
            R.id.nav_logout -> logoutUser()
            else -> showToast("Unknown option")
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logoutUser() {
        // This is fine, as logging out should clear any saved credentials.
        getSharedPreferences("MyPrefs", MODE_PRIVATE).edit().clear().apply()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Redirect to login and clear the task stack
        val intent = Intent(this, SupervisorLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }
}
