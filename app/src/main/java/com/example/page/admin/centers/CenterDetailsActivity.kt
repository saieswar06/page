package com.example.page.admin.centers

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
//import androidx.glance.visibility
import com.example.page.R
import com.example.page.api.CenterDetailsResponse
import com.example.page.api.CenterResponse
import com.example.page.api.RetrofitClient
import com.example.page.databinding.ActivityCenterDetailsBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CenterDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCenterDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCenterDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start entry animation
        val anim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        binding.root.startAnimation(anim)

        // Get the center ID passed from the previous activity.
        val centerId = intent.getIntExtra("CENTER_ID", -1)

        // Check if the ID is valid. If not, show an error and close the activity.
        if (centerId == -1) {
            Toast.makeText(this, "Error: Invalid Center ID.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Fetch the details from the database using the ID.
        fetchCenterDetails(centerId)

        // Close buttons
        binding.btnCloseHeader.setOnClickListener { finishWithAnim() }
        binding.btnCloseFooter.setOnClickListener { finishWithAnim() }
    }

    /**
     * Fetches center details from the server using Retrofit.
     */
    private fun fetchCenterDetails(id: Int) {
        setLoadingState(true)

        RetrofitClient.getInstance(this)
            .getCenterDetails(id) // Call the new API endpoint
            .enqueue(object : Callback<CenterDetailsResponse> {
                override fun onResponse(call: Call<CenterDetailsResponse>, response: Response<CenterDetailsResponse>) {
                    setLoadingState(false)

                    if (response.isSuccessful && response.body()?.success == true) {
                        val center = response.body()?.data
                        if (center != null) {
                            // If data is received, populate the UI fields.
                            populateUi(center)
                        } else {
                            handleFetchError("Center data is empty.")
                        }
                    } else {
                        handleFetchError("Failed to fetch details: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<CenterDetailsResponse>, t: Throwable) {
                    setLoadingState(false)
                    handleFetchError("Network error: ${t.message}")
                }
            })
    }

    /**
     * Populates the TextViews with data from the fetched CenterResponse object.
     */
    private fun populateUi(center: CenterResponse) {
        binding.tvCenterName.text = center.center_name ?: "N/A"
        binding.tvCenterCode.text = center.center_code?.toString() ?: "N/A"
        binding.tvState.text = center.state ?: "N/A"
        binding.tvDistrict.text = center.district ?: "N/A"
        binding.tvMandal.text = center.mandal ?: "N/A"
        binding.tvLocality.text = center.locality ?: "N/A"
        binding.tvLatitude.text = center.latitude?.toString() ?: "N/A"
        binding.tvLongitude.text = center.longitude?.toString() ?: "N/A"
        binding.tvNumTeachers.text = "1" // Placeholder, update if API provides this
        binding.tvTeachers.text = center.email ?: "No teachers available"
    }

    /**
     * Manages the visibility of the progress bar.
     */
    private fun setLoadingState(isLoading: Boolean) {
        binding.detailsProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    /**
     * Displays an error message and closes the activity.
     */
    private fun handleFetchError(errorMessage: String) {
        Log.e("CenterDetailsActivity", errorMessage)
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        finish() // Close the activity on error
    }

    private fun finishWithAnim() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        binding.root.startAnimation(anim)
        binding.root.postDelayed({ finish() }, 250)
    }
}
