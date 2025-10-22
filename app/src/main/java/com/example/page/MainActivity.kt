package com.example.page

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.api.LoginRequest
import com.example.page.api.LoginResponse
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityMainBinding
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Handle Worker Login button click
        binding.loginButton.setOnClickListener {
            val mobile = binding.MobileNumber.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (mobile.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter mobile number and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performWorkerLogin(mobile, password)
        }

        // ✅ Optional: Google Sign-In button
        binding.btnGoogleSignIn.setOnClickListener {
            Toast.makeText(this, "Google Sign-In coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performWorkerLogin(mobile: String, password: String) {
        binding.loginButton.isEnabled = false
        binding.loginButton.text = "Logging in..."
        binding.btnGoogleSignIn.isEnabled = false

        val request = LoginRequest(
            mobile = mobile,
            password = password,
            loginType = "worker" // ✅ Important: worker login type
        )

        Log.d("WorkerLogin", "📤 Sending request: ${Gson().toJson(request)}")

        RetrofitClient.getInstance(this)
            .login(request)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    binding.loginButton.isEnabled = true
                    binding.loginButton.text = "Login"
                    binding.btnGoogleSignIn.isEnabled = true

                    if (response.isSuccessful && response.body() != null) {
                        val res = response.body()!!
                        val token = res.token
                        val user = res.user

                        // ✅ Save token in UserSession (the correct file)
                        getSharedPreferences("UserSession", MODE_PRIVATE).edit()
                            .putString("token", token)
                            .putString("mobile", user?.mobile)
                            .putString("role", "worker")
                            .apply()

                        RetrofitClient.clearInstance()

                        Toast.makeText(
                            this@MainActivity,
                            "Welcome ${user?.mobile ?: "Worker"}!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // ✅ Redirect to Worker Dashboard
                        val intent = Intent(this@MainActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        val errBody = response.errorBody()?.string()
                        Log.e("WorkerLogin", "❌ Error: ${response.code()} $errBody")
                        Toast.makeText(this@MainActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    binding.loginButton.isEnabled = true
                    binding.loginButton.text = "Login"
                    binding.btnGoogleSignIn.isEnabled = true

                    Log.e("WorkerLogin", "⚠️ Network error", t)
                    Toast.makeText(this@MainActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
