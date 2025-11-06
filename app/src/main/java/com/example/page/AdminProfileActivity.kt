package com.example.page

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityAdminProfileBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch

class AdminProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminProfileBinding
    private lateinit var session: SharedPreferences

    companion object {
        private const val TAG = "AdminProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = getSharedPreferences("UserSession", MODE_PRIVATE)

        loadAdminProfile()

        binding.tvResetPassword.setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java)
            intent.putExtra("email", binding.etEmail.text.toString())
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun loadAdminProfile() {
        binding.progressBar.visibility = View.VISIBLE

        // Load profile data from SharedPreferences
        val fullName = session.getString("full_name", "N/A")
        val email = session.getString("email", "N/A")
        val mobile = session.getString("mobile", "N/A")

        if (fullName != "N/A") {
            binding.tvAdminName.text = fullName
            binding.etEmail.setText(email)
            binding.etPhone.setText(mobile)
        } else {
            Toast.makeText(this, "Profile data not found. Please log in again.", Toast.LENGTH_SHORT).show()
            logout()
        }

        binding.progressBar.visibility = View.GONE
    }

    private fun logout() {
        // Clear session/token and navigate to login screen
        session.edit().clear().apply()
        RetrofitClient.clearInstance()
        val intent = Intent(this, SupervisorLoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
}
