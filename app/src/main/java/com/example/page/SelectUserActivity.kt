package com.example.page

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SelectUserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_user)

        // ✅ Find the views
        val optionWorker = findViewById<LinearLayout>(R.id.optionWorker)
        val optionSupervisor = findViewById<LinearLayout>(R.id.optionSupervisor)
        val optionBeneficiary = findViewById<LinearLayout>(R.id.optionBeneficiary)

        // ✅ ECCE Worker → opens normal login
        optionWorker.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("USER_TYPE", "WORKER")
            startActivity(intent)
        }

        // ✅ Supervisor / CDPO / DPO → opens Admin-style Department Login Page
        optionSupervisor.setOnClickListener {
            val intent = Intent(this, SupervisorLoginActivity::class.java)
            intent.putExtra("USER_TYPE", "SUPERVISOR")
            startActivity(intent)
        }

        // ✅ Beneficiary → you can later connect to its own login (optional)
        optionBeneficiary.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("USER_TYPE", "BENEFICIARY")
            startActivity(intent)
        }
    }
}
