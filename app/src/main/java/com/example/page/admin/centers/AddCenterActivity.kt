package com.example.page.admin.centers

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.api.AddCenterRequest
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityAddCenterBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddCenterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCenterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCenterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSave.setOnClickListener { saveCenter() }
    }

    private fun saveCenter() {
        val centerName = binding.etCenterName.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val mobile = binding.etMobile.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val lat = binding.etLat.text.toString().toDoubleOrNull() ?: 0.0
        val lng = binding.etLng.text.toString().toDoubleOrNull() ?: 0.0

        if (centerName.isEmpty() || email.isEmpty() || mobile.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val request = AddCenterRequest(
            centerName = centerName,
            email = email,
            mobile = mobile,
            password = password,
            address = address,
            latitude = lat,
            longitude = lng
        )

        binding.progressBar.visibility = View.VISIBLE

        RetrofitClient.getInstance(this).addCenter(request).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Toast.makeText(this@AddCenterActivity, "Center added successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@AddCenterActivity, "Failed to add center", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@AddCenterActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
