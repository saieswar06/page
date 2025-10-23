package com.example.page.teacher

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.api.ApiResponse
import com.example.page.api.RetrofitClient
import com.example.page.api.TeacherModel
import com.example.page.databinding.ActivityEditTeacherBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditTeacherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditTeacherBinding
    private var teacher: TeacherModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        teacher = intent.getParcelableExtra("teacher")

        teacher?.let { t ->
            binding.etName.setText(t.name)
            binding.etEmail.setText(t.email)
            binding.etPhone.setText(t.phone)
            binding.etPassword.setText("******")
            binding.spinnerCenter.setText(t.centerName ?: "")
        }

        binding.btnCloseEdit.setOnClickListener { finish() }

        binding.btnSaveTeacher.setOnClickListener {
            updateTeacher()
        }
    }

    private fun updateTeacher() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val centerName = binding.spinnerCenter.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty() || centerName.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedTeacher = TeacherModel(
            uid = teacher?.uid,
            userId = teacher?.userId,
            name = name,
            email = email,
            phone = phone,
            centerCode = teacher?.centerCode,
            centerName = centerName,
            status = teacher?.status
        )

        val token = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            .getString("token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired, please login again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        RetrofitClient.getInstance(this)
            .updateTeacher("Bearer $token", teacher?.uid ?: 0, updatedTeacher)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@EditTeacherActivity,
                            "Teacher updated successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(
                            this@EditTeacherActivity,
                            "Update failed: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Toast.makeText(
                        this@EditTeacherActivity,
                        "Error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
