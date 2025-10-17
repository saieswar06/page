package com.example.page

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.databinding.ActivitySelectUserBinding

class SelectUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectUserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.optionWorker.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        binding.optionSupervisor.setOnClickListener {
            startActivity(Intent(this, SupervisorLoginActivity::class.java))
        }

        binding.optionBeneficiary.setOnClickListener {
            Toast.makeText(this, "Beneficiary module coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}
