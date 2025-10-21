package com.example.page.admin.centers

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.R
import com.example.page.api.AddCenterRequest
import com.example.page.api.ApiResponse
import com.example.page.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddCenterActivity : AppCompatActivity() {

    private lateinit var etCenterName: EditText
    private lateinit var etAddress: EditText
    private lateinit var etEmail: EditText
    private lateinit var etMobile: EditText
    private lateinit var etPassword: EditText
    private lateinit var etLat: EditText
    private lateinit var etLng: EditText
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_center)

        // Initialize views
        etCenterName = findViewById(R.id.etCenterName)
        etAddress = findViewById(R.id.etAddress)
        etEmail = findViewById(R.id.etEmail)
        etMobile = findViewById(R.id.etMobile)
        etPassword = findViewById(R.id.etPassword)
        etLat = findViewById(R.id.etLat)
        etLng = findViewById(R.id.etLng)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)

        btnSave.setOnClickListener {
            validateAndSave()
        }
    }

    private fun validateAndSave() {
        val name = etCenterName.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val mobile = etMobile.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val latStr = etLat.text.toString().trim()
        val lngStr = etLng.text.toString().trim()

        // Validation
        if (name.isEmpty()) {
            etCenterName.error = "Required"
            etCenterName.requestFocus()
            return
        }

        if (address.isEmpty()) {
            etAddress.error = "Required"
            etAddress.requestFocus()
            return
        }

        // Email validation
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Invalid email"
            etEmail.requestFocus()
            return
        }

        // Mobile validation
        if (mobile.isNotEmpty() && mobile.length < 10) {
            etMobile.error = "At least 10 digits"
            etMobile.requestFocus()
            return
        }

        val latitude = latStr.toDoubleOrNull()
        val longitude = lngStr.toDoubleOrNull()

        val request = AddCenterRequest(
            center_name = name,
            address = address,
            email = email.ifEmpty { null },
            mobile = mobile.ifEmpty { null },
            password = password.ifEmpty { null },
            latitude = latitude,
            longitude = longitude,
            stateCode = 23,
            districtCode = 1,
            blockCode = 1,
            sectorCode = 1
        )

        addCenter(request)
    }

    private fun addCenter(request: AddCenterRequest) {
        progressBar.visibility = View.VISIBLE
        btnSave.isEnabled = false

        Log.d("AddCenter", "Request: $request")

        RetrofitClient.getInstance(this).addCenter(request)
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true

                    if (response.isSuccessful && response.body()?.success == true) {
                        val msg = response.body()?.message ?: "Center added!"
                        Toast.makeText(this@AddCenterActivity, "✅ $msg", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val error = response.errorBody()?.string()
                        Log.e("AddCenter", "Error: $error")
                        Toast.makeText(
                            this@AddCenterActivity,
                            "❌ Failed to add center",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    Log.e("AddCenter", "Error", t)
                    Toast.makeText(
                        this@AddCenterActivity,
                        "⚠️ Network error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}