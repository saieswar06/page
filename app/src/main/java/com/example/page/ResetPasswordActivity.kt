package com.example.page

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.page.api.ApiService
import com.example.page.api.RequestPasswordResetRequest
import com.example.page.api.RetrofitClient
import com.example.page.api.VerifyOtpRequest
import com.example.page.databinding.ActivityResetPasswordBinding
import kotlinx.coroutines.launch
import java.io.IOException

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiService = RetrofitClient.getInstance(this)

        val email = intent.getStringExtra("email")
        binding.etEmail.setText(email)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSendOtp.setOnClickListener {
            handleSendOtp()
        }

        binding.btnSubmit.setOnClickListener {
            handleResetPassword()
        }
    }

    private fun handleSendOtp() {
        val email = binding.etEmail.text.toString().trim()
        if (email.isEmpty()) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = apiService.requestPasswordReset(RequestPasswordResetRequest(email))
                if (response.isSuccessful) {
                    Toast.makeText(this@ResetPasswordActivity, "OTP sent to your email", Toast.LENGTH_SHORT).show()
                } else {
                    val errorMessage = response.body()?.message ?: "Failed to send OTP: ${response.message()}"
                    Toast.makeText(this@ResetPasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                val errorMessage = if (t is IOException) {
                    "Network error, please check your connection and try again."
                } else {
                    "An unexpected error occurred: ${t.message}"
                }
                Toast.makeText(this@ResetPasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun handleResetPassword() {
        val email = binding.etEmail.text.toString().trim()
        val otp = binding.etOtp.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (email.isEmpty() || otp.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        val request = VerifyOtpRequest(email = email, otp = otp, new_password = newPassword)

        lifecycleScope.launch {
            try {
                val response = apiService.verifyOTPAndResetPassword(request)
                if (response.isSuccessful) {
                    Toast.makeText(this@ResetPasswordActivity, "Password reset successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorMessage = response.body()?.message ?: "Failed to reset password: ${response.message()}"
                    Toast.makeText(this@ResetPasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                val errorMessage = if (t is IOException) {
                    "Network error, please check your connection and try again."
                } else {
                    "An unexpected error occurred: ${t.message}"
                }
                Toast.makeText(this@ResetPasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSendOtp.isEnabled = !isLoading
        binding.btnSubmit.isEnabled = !isLoading
    }
}
