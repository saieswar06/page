package com.example.page

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.page.api.LoginRequest
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivitySupervisorLoginBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException

class SupervisorLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupervisorLoginBinding
    private var selectedRole = ROLE_ADMIN // Default role

    companion object {
        private const val ROLE_ADMIN = "admin"
        private const val ROLE_PEER_REVIEWER = "peer_reviewer"
        private const val ROLE_NITI_SURVEYOR = "niti_surveyor"
        private const val TAG = "SupervisorLogin"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupervisorLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRoleSelection()
        setupFormValidation()
        setupLoginButton()
    }

    private fun setupRoleSelection() {
        binding.toggleRole.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                selectedRole = when (checkedId) {
                    R.id.btn_admin -> ROLE_ADMIN
                    R.id.btn_peer_reviewer -> ROLE_PEER_REVIEWER
                    R.id.btn_niti_surveyor -> ROLE_NITI_SURVEYOR
                    else -> ROLE_ADMIN
                }
            } else if (group.checkedButtonId == View.NO_ID) {
                group.check(checkedId) // Prevent unchecking all buttons
            }
        }
    }

    private fun setupFormValidation() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()
                binding.btnLogin.isEnabled = email.isNotEmpty() && password.isNotEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.etEmail.addTextChangedListener(textWatcher)
        binding.etPassword.addTextChangedListener(textWatcher)
    }

    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Invalid email format"
                return@setOnClickListener
            }

            performLogin(email, password, selectedRole)
        }
    }

    private fun performLogin(email: String, password: String, role: String) {
        showLoading(true)

        val request = LoginRequest(email = email, password = password, loginType = role)
        Log.d(TAG, "📤 Request: ${Gson().toJson(request)}")

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance(this@SupervisorLoginActivity).login(request)
                showLoading(false)
                if (response.isSuccessful && response.body() != null) {
                    handleLoginSuccess(response.body()!!)
                } else {
                    handleLoginError(response.code(), response.errorBody()?.string())
                }
            } catch (t: Throwable) {
                showLoading(false)
                handleLoginFailure(t)
            }
        }
    }

    private fun handleLoginSuccess(response: com.example.page.api.LoginResponse) {
        Log.d(TAG, "✅ Login Success: ${Gson().toJson(response)}")
        val user = response.data?.user

        getSharedPreferences("UserSession", MODE_PRIVATE).edit().apply {
            putString("token", response.data?.token)
            putString("user_id", user?.uniqueId)
            putString("full_name", user?.fullName)
            putString("email", user?.email)
            putString("mobile", user?.mobileNumber)
            putInt("role_id", user?.roleId ?: 0)
            putString("role", selectedRole)
            apply()
        }

        RetrofitClient.clearInstance() // Important: clear previous Retrofit instance

        Toast.makeText(this, "Welcome ${user?.fullName ?: "User"}", Toast.LENGTH_SHORT).show()

        when (user?.roleId) {
            3 -> navigateTo(AdminDashboardActivity::class.java)
            4, 5 -> {
                Toast.makeText(this, "Dashboard coming soon", Toast.LENGTH_SHORT).show()
                navigateTo(AdminDashboardActivity::class.java) // Placeholder
            }
            else -> Toast.makeText(this, "Unknown role. Contact support.", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleLoginError(code: Int, errorBody: String?) {
        Log.e(TAG, "❌ Login Error: $code - $errorBody")

        val errorMessage = try {
            Gson().fromJson(errorBody, Map::class.java)["message"] as? String
        } catch (e: Exception) { null } ?: when (code) {
            401 -> "Invalid email or password."
            403 -> "Access denied for this role."
            404 -> "User not found."
            500 -> "Server error. Please try again later."
            else -> "An unknown error occurred."
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun handleLoginFailure(t: Throwable) {
        Log.e(TAG, "⚠️ Network Failure", t)

        val errorMessage = when (t) {
            is SocketTimeoutException -> "Connection timed out. Please check your network and ensure the server is running."
            is IOException -> "Network error. Please check your internet connection."
            else -> "An unexpected network error occurred: ${t.message}"
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
    }

    private fun <T> navigateTo(activity: Class<T>) {
        startActivity(Intent(this, activity).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
