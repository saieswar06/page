package com.example.page.teacher

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.page.api.*
import com.example.page.databinding.ActivityAddTeacherBinding
import kotlinx.coroutines.launch

/**
 * Activity for adding a new teacher.
 */
class AddTeacherActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTeacherBinding
    private val centersMap = mutableMapOf<String, CenterResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTeacherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up click listeners.
        binding.btnCloseAdd.setOnClickListener { finish() }
        binding.btnSaveTeacher.setOnClickListener { addTeacher() }

        // Load the list of centers.
        loadCenters()

        // Set up the center spinner item click listener.
        binding.spinnerCenter.setOnItemClickListener { _, _, position, _ ->
            val selectedCenterName = binding.spinnerCenter.adapter.getItem(position) as String
            val selectedCenter = centersMap[selectedCenterName]
            if (selectedCenter != null) {
                // If a center is selected, populate the center details card.
                populateCenterDetails(selectedCenter)
                binding.cardCenterDetails.visibility = View.VISIBLE
            } else {
                binding.cardCenterDetails.visibility = View.GONE
            }
        }
    }

    /**
     * Populates the center details card with the selected center's information.
     */
    private fun populateCenterDetails(center: CenterResponse) {
        binding.tvDetailCenterName.text = center.center_name ?: "N/A"
        binding.tvDetailCenterCode.text = center.center_code ?: "N/A"
        binding.tvDetailState.text = center.state ?: "N/A"
        binding.tvDetailDistrict.text = center.district ?: "N/A"
        binding.tvDetailMandal.text = center.mandal ?: "N/A"
        binding.tvDetailLocality.text = center.locality ?: "N/A"
    }

    /**
     * Loads the list of centers from the server.
     */
    private fun loadCenters() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance(this@AddTeacherActivity).getCenters(1)
                if (response.isSuccessful && response.body()?.success == true) {
                    val centers = response.body()?.data ?: emptyList()

                    if (centers.isEmpty()) {
                        Toast.makeText(this@AddTeacherActivity, "No centers available", Toast.LENGTH_LONG).show()
                        return@launch
                    }

                    // Filter out centers with missing names or codes.
                    val validCenters = centers.filter { !it.center_name.isNullOrEmpty() && !it.center_code.isNullOrEmpty() }
                    val centerNames = validCenters.map { it.center_name!! }

                    // Map center names to center objects for easy lookup.
                    validCenters.forEach {
                        centersMap[it.center_name!!] = it
                    }

                    // Create and set the adapter for the center spinner.
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
            } catch (t: Throwable) {
                Toast.makeText(
                    this@AddTeacherActivity,
                    "Failed to load centers: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("AddTeacher", "Failed to load centers", t)
            }
        }
    }

    /**
     * Adds a new teacher.
     */
    private fun addTeacher() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim().ifEmpty { null }
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val centerName = binding.spinnerCenter.text.toString().trim()

        // Validate that all required fields are filled.
        if (name.isEmpty() || phone.isEmpty() || password.isEmpty() || centerName.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate that the password is at least 8 characters long.
        if (password.length < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the center code for the selected center.
        val centerCode = centersMap[centerName]?.center_code
        if (centerCode == null) {
            Toast.makeText(this, "Please select a valid center from the list", Toast.LENGTH_LONG).show()
            return
        }

        // Create the add teacher request.
        val addTeacherRequest = AddTeacherRequest(
            name = name,
            email = email,
            phone = phone,
            defaultPassword = password,
            centerCode = centerCode
        )

        binding.btnSaveTeacher.isEnabled = false

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.getInstance(this@AddTeacherActivity).addTeacher(addTeacherRequest)
                binding.btnSaveTeacher.isEnabled = true

                if (response.isSuccessful && response.body()?.success == true) {
                    // If the request is successful, show a success message and finish the activity.
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
            } catch (t: Throwable) {
                binding.btnSaveTeacher.isEnabled = true
                Toast.makeText(
                    this@AddTeacherActivity,
                    "Error: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("AddTeacher", "Network error", t)
            }
        }
    }
}
