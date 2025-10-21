package com.example.page

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.page.databinding.ActivitySelectUserBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Defensive version of SelectUserActivity
 * - Protects against null view binding issues
 * - Logs exceptions instead of silent crash
 * - Keeps your existing button logic
 */
class SelectUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectUserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // Inflate layout safely
            binding = ActivitySelectUserBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Worker login
            binding.optionWorker.setOnClickListener {
                try {
                    startActivity(Intent(this, MainActivity::class.java))
                } catch (t: Throwable) {
                    Log.e("SelectUserActivity", "Failed to open MainActivity", t)
                    Toast.makeText(this, "Error opening Worker login", Toast.LENGTH_SHORT).show()
                }
            }

            // Supervisor login
            binding.optionSupervisor.setOnClickListener {
                try {
                    startActivity(Intent(this, SupervisorLoginActivity::class.java))
                } catch (t: Throwable) {
                    Log.e("SelectUserActivity", "Failed to open SupervisorLoginActivity", t)
                    Toast.makeText(this, "Error opening Supervisor login", Toast.LENGTH_SHORT).show()
                }
            }

            // Beneficiary
            binding.optionBeneficiary.setOnClickListener {
                Toast.makeText(this, "Beneficiary module coming soon", Toast.LENGTH_SHORT).show()
            }

            // Example: background safe init (optional)
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    // Add light setup tasks here if needed
                    Log.d("SelectUserActivity", "Background init complete")
                } catch (e: Exception) {
                    Log.e("SelectUserActivity", "Background init failed", e)
                }
            }

        } catch (t: Throwable) {
            Log.e("SelectUserActivity", "Error during onCreate", t)
            try {
                Toast.makeText(this, "Startup error: ${t.message}", Toast.LENGTH_LONG).show()
            } catch (_: Exception) { }
            throw t // let global handler (App.kt) log it too
        }
    }
}
