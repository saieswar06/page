package com.example.page

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Load user data from SharedPreferences (saved during login)
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)

        val name = prefs.getString("name", "Unknown")
        val mobile = prefs.getString("mobile", "N/A")
        val aadhar = prefs.getString("aadhar", "N/A")
        val dob = prefs.getString("dob", "N/A")
        val gender = prefs.getString("gender", "N/A")
        val email = prefs.getString("email", "N/A")
        val experience = prefs.getString("experience", "N/A")
        val education = prefs.getString("education", "N/A")
        val ekycStatus = prefs.getString("ekycStatus", "Pending")
        val ekycDate = prefs.getString("ekycDate", "N/A")

        // ✅ Populate UI
        binding.apply {
            tvName.text = name
            tvRole.text = "ECCE Worker"
            tvMobile.text = "Mobile: $mobile"
            tvEmail.text = "Email: $email"
            tvAadhar.text = "Aadhar: $aadhar"
            tvDOB.text = "DOB: $dob"
            tvGender.text = "Gender: $gender"
            tvExperience.text = "Experience: $experience"
            tvEducation.text = "Education: $education"
            tvEkycStatus.text = "eKYC Status: $ekycStatus"
            tvEkycDate.text = "eKYC Date: $ekycDate"
        }

        // ✅ Back Button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // ✅ Logout button → clear session and return to MainActivity
        binding.btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}
