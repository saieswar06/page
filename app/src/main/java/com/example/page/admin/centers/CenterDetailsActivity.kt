package com.example.page.admin.centers

import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.page.R
import com.example.page.databinding.ActivityCenterDetailsBinding

class CenterDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCenterDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCenterDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Smooth entry animation
        val anim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in)
        binding.root.startAnimation(anim)

        // Read values from intent safely
        binding.tvCenterName.text = intent.getStringExtra("center_name") ?: "N/A"
        binding.tvCenterCode.text = intent.getStringExtra("center_code") ?: "N/A"
        binding.tvState.text = intent.getStringExtra("state") ?: "N/A"
        binding.tvDistrict.text = intent.getStringExtra("district") ?: "N/A"
        binding.tvMandal.text = intent.getStringExtra("mandal") ?: "N/A"
        binding.tvLocality.text = intent.getStringExtra("locality") ?: "N/A"
        binding.tvLatitude.text = intent.getStringExtra("latitude") ?: "N/A"
        binding.tvLongitude.text = intent.getStringExtra("longitude") ?: "N/A"
        binding.tvNumTeachers.text = intent.getStringExtra("teacher_count") ?: "0"
        binding.tvTeachers.text = intent.getStringExtra("teachers") ?: "No teachers available"

        // Close buttons
        binding.btnCloseHeader.setOnClickListener { finishWithAnim() }
        binding.btnCloseFooter.setOnClickListener { finishWithAnim() }
    }

    private fun finishWithAnim() {
        val anim = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        binding.root.startAnimation(anim)
        binding.root.postDelayed({ finish() }, 250)
    }
}
