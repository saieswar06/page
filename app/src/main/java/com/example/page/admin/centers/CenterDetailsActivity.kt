package com.example.page.admin.centers

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.R
import com.example.page.SupervisorLoginActivity
import com.example.page.api.CenterDetailsResponse
import com.example.page.api.RetrofitClient
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CenterDetailsActivity : AppCompatActivity() {

    private lateinit var tvCenterName: TextView
    private lateinit var tvCenterCode: TextView
    private lateinit var tvState: TextView
    private lateinit var tvDistrict: TextView
    private lateinit var tvMandal: TextView
    private lateinit var tvLocality: TextView
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvTeachersCount: TextView
    private lateinit var tvTeachersNames: TextView

    private lateinit var btnCloseHeader: ImageButton
    private lateinit var btnCloseFooter: Button
    private lateinit var progressBar: ProgressBar

    private var centerId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_center_details)

        initializeViews()

        centerId = intent.getIntExtra("CENTER_ID", -1)

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        if (prefs.getString("token", null).isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SupervisorLoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        if (centerId == -1) {
            Toast.makeText(this, "Invalid center ID", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        btnCloseHeader.setOnClickListener { finish() }
        btnCloseFooter.setOnClickListener { finish() }

        loadCenterDetails()
    }

    private fun initializeViews() {
        tvCenterName = findViewById(R.id.tv_center_name)
        tvCenterCode = findViewById(R.id.tv_center_code)
        tvState = findViewById(R.id.tv_state)
        tvDistrict = findViewById(R.id.tv_district)
        tvMandal = findViewById(R.id.tv_mandal)
        tvLocality = findViewById(R.id.tv_locality)
        tvLatitude = findViewById(R.id.tv_latitude)
        tvLongitude = findViewById(R.id.tv_longitude)
        tvTeachersCount = findViewById(R.id.tv_teachers_count)
        tvTeachersNames = findViewById(R.id.tv_teachers_names)

        btnCloseHeader = findViewById(R.id.btn_close_header)
        btnCloseFooter = findViewById(R.id.btn_close_footer)
        progressBar = findViewById(R.id.detailsProgressBar)
    }

    private fun loadCenterDetails() {
        progressBar.visibility = View.VISIBLE

        RetrofitClient.getInstance(this)
            .getCenterDetails(centerId)
            .enqueue(object : Callback<CenterDetailsResponse> {
                override fun onResponse(
                    call: Call<CenterDetailsResponse>,
                    response: Response<CenterDetailsResponse>
                ) {
                    progressBar.visibility = View.GONE

                    if (response.isSuccessful) {
                        val centerDetailsResponse = response.body()

                        if (centerDetailsResponse != null) {
                            val jsonResponse = Gson().toJson(centerDetailsResponse)
                            Log.d("CenterDetailsActivity", "Parsed Response: $jsonResponse")
                        }

                        if (centerDetailsResponse?.success == true) {
                            val data = centerDetailsResponse.data
                            if (data != null) {
                                tvCenterName.text = data.center_name ?: "N/A"
                                tvCenterCode.text = data.center_code ?: "N/A"
                                tvState.text = data.state ?: "N/A"
                                tvDistrict.text = data.district ?: "N/A"
                                tvMandal.text = data.mandal ?: "N/A"
                                tvLocality.text = data.locality ?: "N/A"
                                tvLatitude.text = data.latitude?.toString() ?: "N/A"
                                tvLongitude.text = data.longitude?.toString() ?: "N/A"

                                val teachers = data.teachers
                                if (teachers.isNullOrEmpty()) {
                                    tvTeachersCount.text = "0"
                                    tvTeachersNames.text = "N/A"
                                } else {
                                    tvTeachersCount.text = teachers.size.toString()
                                    // Try 'name' first, fallback to 'full_name'
                                    val names = teachers.mapNotNull {
                                        it.name ?: it.full_name
                                    }.filter { it.isNotBlank() }
                                    tvTeachersNames.text = if (names.isEmpty()) {
                                        "Name not available"
                                    } else {
                                        names.joinToString(", ")
                                    }
                                }
                            } else {
                                Toast.makeText(this@CenterDetailsActivity, "No details found.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorMessage = centerDetailsResponse?.message ?: "Response indicates failure."
                            Log.w("CenterDetailsActivity", "API Response not successful: $errorMessage")
                            Toast.makeText(this@CenterDetailsActivity, "Failed to load details: $errorMessage", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("CenterDetailsActivity", "API Error: ${response.code()} ${response.message()} - $errorBody")
                        Toast.makeText(this@CenterDetailsActivity, "Error: ${response.message()}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<CenterDetailsResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    Log.e("CenterDetailsActivity", "Network Error", t)
                    Toast.makeText(
                        this@CenterDetailsActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}