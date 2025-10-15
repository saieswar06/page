package com.example.page

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.api.LoginRequest
import com.example.page.api.LoginResponse
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityMainBinding
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
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(mobile, password)
        }
    }

    private fun performLogin(mobile: String, password: String) {
        binding.loginButton.isEnabled = false
        binding.googleSignInButton.isEnabled = false
        Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()

        val request = LoginRequest(mobile = mobile, password = password)

        RetrofitClient.instance.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                binding.loginButton.isEnabled = true
                binding.googleSignInButton.isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!.user

                    // ✅ Safely assign fields or "N/A"
                    val name = user?.name ?: "N/A"
                    val mobileNum = user?.mobile ?: "N/A"
                    val aadhar = user?.aadhar ?: "N/A"
                    val dob = user?.dob ?: "N/A"
                    val gender = user?.gender ?: "N/A"
                    val experience = user?.experience ?: "N/A"
                    val education = user?.education ?: "N/A"
                    val ekycStatus = user?.ekycStatus ?: "N/A"
                    val ekycDate = user?.ekycDate ?: "N/A"

                    // ✅ Display summary without throwing errors
                    Toast.makeText(
                        this@MainActivity,
                        """
                        Welcome $name!
                        Mobile: $mobileNum
                        Aadhar: $aadhar
                        DOB: $dob
                        Gender: $gender
                        Experience: $experience
                        Education: $education
                        eKYC: $ekycStatus ($ekycDate)
                        """.trimIndent(),
                        Toast.LENGTH_LONG
                    ).show()

                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Invalid credentials or not authorized",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                binding.loginButton.isEnabled = true
                binding.googleSignInButton.isEnabled = true
                Toast.makeText(this@MainActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
