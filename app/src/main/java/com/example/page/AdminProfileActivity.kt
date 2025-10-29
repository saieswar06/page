package com.example.page

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityAdminProfileBinding

class AdminProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make the background of the activity see-through
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Retrieve user info from SharedPreferences (if available)
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val email = prefs.getString("email", "admin@example.com")
        val name = prefs.getString("name", email) // Assuming you store a name

        // Populate the views
        binding.tvAdminName.text = name
        binding.tvAdminEmail.text = email

        binding.btnLogout.setOnClickListener { logoutUser() }
    }

    private fun logoutUser() {
        // Clear SharedPreferences
        getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply()

        // Clear Retrofit instance
        RetrofitClient.clearInstance()

        // Show toast and redirect to login
        showToast("Logged out successfully")
        val intent = Intent(this, SupervisorLoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}