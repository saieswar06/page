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

        binding.loginButton.setOnClickListener {
            val mobile = binding.MobileNumber.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (mobile.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter mobile number and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performWorkerLogin(mobile, password)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            Toast.makeText(this, "Google Sign-In coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performWorkerLogin(mobile: String, password: String) {
        setLoadingState(true)

        // Step 1: Ensure a completely clean state before attempting to log in.
        getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply()
        RetrofitClient.clearInstance()

        // Step 2: Create the LoginRequest with the correct loginType ("ecce").
        val request = LoginRequest(
            mobileNumber = mobile,
            password = password,
            loginType = "ecce" // This is the correct value based on your server's error message.
        )

        Log.d("WorkerLogin", "📤 Sending Login Request: ${Gson().toJson(request)}")

        // Step 3: Execute the network call.
        // FIX: Use the correct variable name 'request', not 'req'.
        RetrofitClient.getInstance(this)
            .login(request)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    // FIX: The call to setLoadingState(false) should be the first thing to happen.
                    setLoadingState(false)

                    if (response.isSuccessful && response.body() != null) {
                        // FIX: Use the correct variable name 'res', not 'loginResponse'.
                        val res = response.body()!!

                        // After a SUCCESSFUL login, clear the client instance again.
                        // This forces the next activity to build a new client with the new token.
                        RetrofitClient.clearInstance()

                        // Step 5: Save the new, valid session data.
                        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE).edit()
                        prefs.putString("token", res.data?.token)
                        prefs.putString("mobile", res.data?.user?.mobileNumber)
                        prefs.putString("role", "ecce") // FIX: Save the correct role string.
                        prefs.apply()

                        Log.d("WorkerLogin", "✅ Login successful. Token saved. Navigating to dashboard.")
                        Toast.makeText(this@MainActivity, "Login Successful!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@MainActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish() // Prevent user from coming back to the login screen.

                    } else {
                        // This block executes on errors like 400, 401, etc.
                        val errorBody = response.errorBody()?.string()
                        Log.e("WorkerLogin", "❌ Login failed. Code: ${response.code()}, Body: $errorBody")
                        Toast.makeText(this@MainActivity, "Invalid credentials or server error.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    // This block executes on network failures.
                    setLoadingState(false)
                    Log.e("WorkerLogin", "⚠️ Network request failed.", t)
                    Toast.makeText(this@MainActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.loginButton.isEnabled = !isLoading
        binding.loginButton.text = if (isLoading) "Logging in..." else "Login"
    }
}
