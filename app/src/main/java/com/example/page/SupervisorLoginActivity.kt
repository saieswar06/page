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
    private var selectedRole = "admin" // For UI only

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupervisorLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRoleButtons()
        setupFormValidation()

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(email, password)
        }
    }

    private fun setupRoleButtons() {
        updateButtonColors("admin")

        binding.btnAdmin.setOnClickListener {
            selectedRole = "admin"
            updateButtonColors("admin")
        }
        binding.btnPeerReviewer.setOnClickListener {
            selectedRole = "peer_reviewer"
            updateButtonColors("peer_reviewer")
        }
        binding.btnNitiSurveyor.setOnClickListener {
            selectedRole = "niti_surveyor"
            updateButtonColors("niti_surveyor")
        }
    }

    private fun updateButtonColors(selected: String) {
        val activeColor = getColor(R.color.purple_700)
        val inactiveColor = getColor(R.color.white)
        val activeText = getColor(R.color.white)
        val inactiveText = getColor(R.color.black)

        binding.btnAdmin.setBackgroundColor(if (selected == "admin") activeColor else inactiveColor)
        binding.btnAdmin.setTextColor(if (selected == "admin") activeText else inactiveText)

        binding.btnPeerReviewer.setBackgroundColor(if (selected == "peer_reviewer") activeColor else inactiveColor)
        binding.btnPeerReviewer.setTextColor(if (selected == "peer_reviewer") activeText else inactiveText)

        binding.btnNitiSurveyor.setBackgroundColor(if (selected == "niti_surveyor") activeColor else inactiveColor)
        binding.btnNitiSurveyor.setTextColor(if (selected == "niti_surveyor") activeText else inactiveText)
    }

    private fun setupFormValidation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnLogin.isEnabled =
                    binding.etEmail.text!!.isNotEmpty() && binding.etPassword.text!!.isNotEmpty()
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etEmail.addTextChangedListener(watcher)
        binding.etPassword.addTextChangedListener(watcher)
    }

    private fun performLogin(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        val req = LoginRequest(
            email = email,
            password = password,
            loginType = "admin"
        )

        Log.d("AdminLogin", "Sending: ${Gson().toJson(req)}")

        RetrofitClient.getInstance(this).login(req)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val res = response.body()!!
                        val token = res.token
                        val user = res.user

                        getSharedPreferences("UserSession", MODE_PRIVATE).edit()
                            .putString("token", token)
                            .putString("email", user?.email)
                            .putString("role", selectedRole)
                            .apply()

                        RetrofitClient.clearInstance()

                        Toast.makeText(
                            this@SupervisorLoginActivity,
                            "Welcome ${user?.email}",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(Intent(this@SupervisorLoginActivity, AdminDashboardActivity::class.java))
                        finish()
                    } else {
                        val errBody = response.errorBody()?.string()
                        Log.e("AdminLogin", "Failed: ${response.code()} $errBody")
                        Toast.makeText(this@SupervisorLoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this@SupervisorLoginActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
