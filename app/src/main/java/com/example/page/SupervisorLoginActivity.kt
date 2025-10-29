package com.example.page

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.api.LoginRequest
import com.example.page.api.LoginResponse
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivitySupervisorLoginBinding
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SupervisorLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupervisorLoginBinding
    private var selectedRole = ROLE_ADMIN // Default role

    companion object {
        private const val ROLE_ADMIN = "admin"
        private const val ROLE_PEER_REVIEWER = "peer_reviewer"
        private const val ROLE_NITI_SURVEYOR = "niti_surveyor"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupervisorLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initially disable the login button
        binding.btnLogin.isEnabled = false

        setupRoleSelection()
        setupFormValidation()

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmail.error = "Email is required"
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Invalid email format"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.etPassword.error = "Password is required"
                return@setOnClickListener
            }

            performLogin(email, password, selectedRole)
        }
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
            } else {
                // Prevent unchecking all buttons
                if (group.checkedButtonId == View.NO_ID) {
                    group.check(checkedId)
                }
            }
        }
    }

    private fun setupFormValidation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()
                binding.btnLogin.isEnabled = email.isNotEmpty() && password.isNotEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etEmail.addTextChangedListener(watcher)
        binding.etPassword.addTextChangedListener(watcher)
    }

    private fun performLogin(email: String, password: String, role: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        val req = LoginRequest(
            mobileNumber = null,
            email = email,
            password = password,
            loginType = role
        )

        Log.d("SupervisorLogin", "📤 Request: ${Gson().toJson(req)}")

        RetrofitClient.getInstance(this).login(req)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true

                    Log.d("SupervisorLogin", "📥 Response: ${response.code()}")

                    if (response.isSuccessful && response.body() != null) {
                        val res = response.body()!!
                        val token = res.data?.token
                        val user = res.data?.user

                        Log.d("SupervisorLogin", "✅ Success")
                        Log.d("SupervisorLogin", "User: ${Gson().toJson(user)}")

                        // ✅ Store session with backend fields
                        getSharedPreferences("UserSession", MODE_PRIVATE).edit().apply {
                            putString("token", token)
                            putString("user_id", user?.uniqueId)
                            putString("email", user?.email)
                            putString("mobile", user?.mobileNumber)
                            putInt("role_id", user?.roleId ?: 0)
                            putString("role", role)
                            apply()
                        }

                        RetrofitClient.clearInstance()

                        Toast.makeText(
                            this@SupervisorLoginActivity,
                            "Welcome ${user?.fullName ?: "User"}",
                            Toast.LENGTH_SHORT
                        ).show()

                        // ✅ Navigate based on role_id from backend
                        when (user?.roleId) {
                            3 -> { // Admin
                                startActivity(Intent(this@SupervisorLoginActivity, AdminDashboardActivity::class.java))
                                finish()
                            }
                            4, 5 -> { // Peer Reviewer & NITI Surveyor
                                Toast.makeText(
                                    this@SupervisorLoginActivity,
                                    "Dashboard coming soon",
                                    Toast.LENGTH_SHORT
                                ).show()
                                startActivity(Intent(this@SupervisorLoginActivity, AdminDashboardActivity::class.java))
                                finish()
                            }
                            else -> {
                                Toast.makeText(
                                    this@SupervisorLoginActivity,
                                    "Unknown role. Contact support.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        val errBody = response.errorBody()?.string()
                        Log.e("SupervisorLogin", "❌ Error: ${response.code()} - $errBody")

                        val errorMessage = try {
                            Gson().fromJson(errBody, Map::class.java)["message"] as? String
                        } catch (e: Exception) {
                            null
                        } ?: when (response.code()) {
                            400 -> "Invalid request format"
                            401 -> "Invalid email or password"
                            403 -> "Access denied. Use correct login portal."
                            404 -> "User not found"
                            500 -> "Server error. Please try again."
                            else -> "Login failed"
                        }

                        Toast.makeText(
                            this@SupervisorLoginActivity,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true

                    Log.e("SupervisorLogin", "⚠️ Network Error", t)

                    val errorMessage = when {
                        t.message?.contains("Unable to resolve host") == true ->
                            "Cannot connect to server. Check backend and BASE_URL."
                        t.message?.contains("timeout") == true ->
                            "Connection timeout. Server not responding."
                        else ->
                            "Network Error: ${t.message}"
                    }

                    Toast.makeText(
                        this@SupervisorLoginActivity,
                        errorMessage,
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}
