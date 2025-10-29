package com.example.page.teacher

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.api.*
import com.example.page.databinding.ActivityAddTeacherBinding
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddTeacherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTeacherBinding
    private val centersMap = mutableMapOf<String, CenterResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCloseAdd.setOnClickListener { finish() }
        binding.btnSaveTeacher.setOnClickListener { addTeacher() }

        loadCenters()

        binding.spinnerCenter.setOnItemClickListener { _, _, position, _ ->
            val selectedCenterName = binding.spinnerCenter.adapter.getItem(position) as String
            val selectedCenter = centersMap[selectedCenterName]
            if (selectedCenter != null) {
                populateCenterDetails(selectedCenter)
                binding.cardCenterDetails.visibility = View.VISIBLE
            } else {
                binding.cardCenterDetails.visibility = View.GONE
            }
        }
    }

    private fun populateCenterDetails(center: CenterResponse) {
        binding.tvDetailCenterName.text = center.center_name ?: "N/A"
        binding.tvDetailCenterCode.text = center.center_code ?: "N/A"
        binding.tvDetailState.text = center.state ?: "N/A"
        binding.tvDetailDistrict.text = center.district ?: "N/A"
        binding.tvDetailMandal.text = center.mandal ?: "N/A"
        binding.tvDetailLocality.text = center.locality ?: "N/A"
    }

    private fun loadCenters() {
        RetrofitClient.getInstance(this).getCenters().enqueue(object : Callback<ApiResponse<List<CenterResponse>>> {
            override fun onResponse(
                call: Call<ApiResponse<List<CenterResponse>>>,
                response: Response<ApiResponse<List<CenterResponse>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val centers = response.body()?.data ?: emptyList()

                    if (centers.isEmpty()) {
                        Toast.makeText(this@AddTeacherActivity, "No centers available", Toast.LENGTH_LONG).show()
                        return
                    }

                    val validCenters = centers.filter { !it.center_name.isNullOrEmpty() && !it.center_code.isNullOrEmpty() }
                    val centerNames = validCenters.map { it.center_name!! }

                    validCenters.forEach {
                        centersMap[it.center_name!!] = it
                    }

                    val adapter = ArrayAdapter(
                        this@AddTeacherActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        centerNames
                    )
                    binding.spinnerCenter.setAdapter(adapter)

                } else {
                    Toast.makeText(
                        this@AddTeacherActivity,
                        "Failed to load centers: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<CenterResponse>>>, t: Throwable) {
                Toast.makeText(
                    this@AddTeacherActivity,
                    "Failed to load centers: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("AddTeacher", "Failed to load centers", t)
            }
        })
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

        if (password.length < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            return
        }

        val centerCode = centersMap[centerName]?.center_code
        if (centerCode == null) {
            Toast.makeText(this, "Please select a valid center from the list", Toast.LENGTH_LONG).show()
            return
        }

        val addTeacherRequest = AddTeacherRequest(
            name = name,
            email = email,
            phone = phone,
            defaultPassword = password,
            centerCode = centerCode
        )

        binding.btnSaveTeacher.isEnabled = false

        RetrofitClient.getInstance(this).addTeacher(addTeacherRequest).enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                binding.btnSaveTeacher.isEnabled = true

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@AddTeacherActivity,
                        response.body()?.message ?: "Teacher added successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val errorMessage = response.body()?.error ?: response.body()?.message ?: "An unknown error occurred"
                    Toast.makeText(this@AddTeacherActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                binding.btnSaveTeacher.isEnabled = true
                Toast.makeText(
                    this@AddTeacherActivity,
                    "Error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("AddTeacher", "Network error", t)
            }
        })
    }
}