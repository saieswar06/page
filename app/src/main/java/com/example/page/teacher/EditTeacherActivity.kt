package com.example.page.teacher

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.page.api.*
import com.example.page.databinding.ActivityEditTeacherBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch

class EditTeacherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditTeacherBinding
    private var teacher: TeacherModel? = null
    private val centersMap = mutableMapOf<String, CenterResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        teacher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("teacher", TeacherModel::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("teacher")
        }

        teacher?.let {
            binding.etName.setText(it.name)
            binding.etEmail.setText(it.email ?: "")
            binding.etPhone.setText(it.phone)
            binding.spinnerCenter.setText(it.centerName ?: "", false)
            binding.tvDetailState.text = it.state ?: "N/A"
            binding.tvDetailDistrict.text = it.district ?: "N/A"
            binding.tvDetailMandal.text = it.mandal ?: "N/A"
        }

        binding.btnCloseEdit.setOnClickListener { finish() }
        binding.btnSaveTeacher.setOnClickListener { validateAndSaveChanges() }

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
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance(this@EditTeacherActivity).getCenters(1)
                if (response.isSuccessful && response.body()?.success == true) {
                    val centers = response.body()?.data ?: emptyList()

                    val centerNames = centers.mapNotNull { it.center_name }
                    centers.forEach { center ->
                        if (center.center_name != null) {
                            centersMap[center.center_name] = center
                        }
                    }

                    val adapter = ArrayAdapter(
                        this@EditTeacherActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        centerNames
                    )
                    binding.spinnerCenter.setAdapter(adapter)

                    teacher?.centerName?.let { currentCenterName ->
                        val currentCenter = centersMap[currentCenterName]
                        if (currentCenter != null) {
                            populateCenterDetails(currentCenter)
                            binding.cardCenterDetails.visibility = View.VISIBLE
                        }
                    }

                } else {
                    Toast.makeText(
                        this@EditTeacherActivity,
                        "Failed to load centers: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (t: Throwable) {
                Toast.makeText(
                    this@EditTeacherActivity,
                    "Failed to load centers: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun validateAndSaveChanges() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val centerName = binding.spinnerCenter.text.toString().trim()

        val centerCode = centersMap[centerName]?.center_code
        if (centerCode == null) {
            Toast.makeText(this, "Please select a valid center", Toast.LENGTH_SHORT).show()
            return
        }

        val updateTeacherRequest = UpdateTeacherRequest(
            name = name,
            email = email,
            phone = phone,
            defaultPassword = if (password.isNotEmpty() && password != "******") password else null,
            centerCode = centerCode
        )

        if (password.isNotEmpty() && password != "******" && password.length < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            return
        }

        updateTeacher(updateTeacherRequest)
    }

    private fun updateTeacher(updateTeacherRequest: UpdateTeacherRequest) {
        val teacherId = teacher?.uid ?: return

        binding.btnSaveTeacher.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance(this@EditTeacherActivity).updateTeacher(teacherId, updateTeacherRequest)
                binding.btnSaveTeacher.isEnabled = true

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        this@EditTeacherActivity,
                        response.body()?.message ?: "Teacher updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody != null) {
                        try {
                            val gson = Gson()
                            val errorResponse = gson.fromJson(errorBody, ApiResponse::class.java)
                            errorResponse.error ?: errorResponse.message ?: "An unknown error occurred"
                        } catch (e: Exception) {
                            response.message()
                        }
                    } else {
                        response.body()?.error ?: response.body()?.message ?: "An unknown error occurred"
                    }
                    Toast.makeText(this@EditTeacherActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                binding.btnSaveTeacher.isEnabled = true
                Toast.makeText(
                    this@EditTeacherActivity,
                    "Error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("EditTeacher", "Network error", t)
            }
        }
    }
}