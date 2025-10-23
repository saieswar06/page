package com.example.page.teacher

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.api.ApiResponse
import com.example.page.api.RetrofitClient
import com.example.page.api.TeacherModel
import com.example.page.databinding.ActivityAddTeacherBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddTeacherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTeacherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCloseAdd.setOnClickListener { finish() }

        binding.btnSaveTeacher.setOnClickListener {
            addTeacher()
        }
    }

    private fun addTeacher() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim().ifEmpty { null }
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val centerName = binding.spinnerCenter.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty() || password.isEmpty() || centerName.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val teacher = TeacherModel(
            uid = null,
            userId = null,
            name = name,
            email = email,
            phone = phone,
            centerCode = null,
            centerName = centerName,
            status = 1
        )

        val token = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .getString("token", null)

        RetrofitClient.getInstance(this)
            .addTeacher("Bearer $token", teacher)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@AddTeacherActivity,
                            "Teacher added successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@AddTeacherActivity,
                            "Add failed: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(
                        this@AddTeacherActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
