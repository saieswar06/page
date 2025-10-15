package com.example.page

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.api.AdminLoginRequest
import com.example.page.api.LoginResponse
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivitySupervisorLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SupervisorLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySupervisorLoginBinding
    private var selectedRole: String = "Admin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupervisorLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRoleButtons()
        setupCaptcha()

        binding.tvReload.setOnClickListener { setupCaptcha() }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnLogin.isEnabled =
                    binding.etEmail.text!!.isNotEmpty() &&
                            binding.etPassword.text!!.isNotEmpty() &&
                            binding.etCaptchaInput.text!!.isNotEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.etEmail.addTextChangedListener(watcher)
        binding.etPassword.addTextChangedListener(watcher)
        binding.etCaptchaInput.addTextChangedListener(watcher)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val captchaInput = binding.etCaptchaInput.text.toString().trim()
            val generatedCaptcha = binding.tvCaptcha.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || captchaInput.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (captchaInput != generatedCaptcha) {
                Toast.makeText(this, "Invalid security code", Toast.LENGTH_SHORT).show()
                setupCaptcha()
                return@setOnClickListener
            }
            performLogin(email, password)
        }
    }

    private fun performLogin(email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        val req = AdminLoginRequest(email = email, password = password, role = selectedRole)

        RetrofitClient.instance.adminLogin(req).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!

                    getSharedPreferences("AdminSession", MODE_PRIVATE)
                        .edit()
                        .putString("token", res.token)
                        .putString("role", selectedRole)
                        .putString("email", res.user.email)
                        .apply()

                    Toast.makeText(
                        this@SupervisorLoginActivity,
                        "Welcome, ${res.user.email ?: selectedRole}",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this@SupervisorLoginActivity, AdminDashboardActivity::class.java)
                        .putExtra("role", selectedRole))
                    finish()
                } else {
                    Toast.makeText(
                        this@SupervisorLoginActivity,
                        "Invalid credentials or not authorized",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                Toast.makeText(
                    this@SupervisorLoginActivity,
                    "Network error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setupRoleButtons() {
        binding.btnAdmin.isSelected = true
        binding.btnAdmin.setOnClickListener {
            selectedRole = "Admin"; highlightSelectedButton("Admin")
        }
        binding.btnPeerReviewer.setOnClickListener {
            selectedRole = "Peer Reviewer"; highlightSelectedButton("Peer Reviewer")
        }
        binding.btnNitiSurveyor.setOnClickListener {
            selectedRole = "Niti Surveyor"; highlightSelectedButton("Niti Surveyor")
        }
    }

    private fun highlightSelectedButton(role: String) {
        val selectedColor = getColor(R.color.purple_700)
        val defaultColor = getColor(R.color.white)
        val selectedTextColor = getColor(R.color.white)
        val defaultTextColor = getColor(R.color.black)

        when (role) {
            "Admin" -> {
                binding.btnAdmin.setBackgroundColor(selectedColor)
                binding.btnAdmin.setTextColor(selectedTextColor)
                binding.btnPeerReviewer.setBackgroundColor(defaultColor)
                binding.btnPeerReviewer.setTextColor(defaultTextColor)
                binding.btnNitiSurveyor.setBackgroundColor(defaultColor)
                binding.btnNitiSurveyor.setTextColor(defaultTextColor)
            }
            "Peer Reviewer" -> {
                binding.btnPeerReviewer.setBackgroundColor(selectedColor)
                binding.btnPeerReviewer.setTextColor(selectedTextColor)
                binding.btnAdmin.setBackgroundColor(defaultColor)
                binding.btnAdmin.setTextColor(defaultTextColor)
                binding.btnNitiSurveyor.setBackgroundColor(defaultColor)
                binding.btnNitiSurveyor.setTextColor(defaultTextColor)
            }
            "Niti Surveyor" -> {
                binding.btnNitiSurveyor.setBackgroundColor(selectedColor)
                binding.btnNitiSurveyor.setTextColor(selectedTextColor)
                binding.btnAdmin.setBackgroundColor(defaultColor)
                binding.btnAdmin.setTextColor(defaultTextColor)
                binding.btnPeerReviewer.setBackgroundColor(defaultColor)
                binding.btnPeerReviewer.setTextColor(defaultTextColor)
            }
        }
    }

    private fun setupCaptcha() {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val captcha = (1..6).map { chars.random() }.joinToString("")
        binding.tvCaptcha.text = captcha
    }
}
