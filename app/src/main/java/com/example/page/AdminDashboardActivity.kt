package com.example.page

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_dashboard)

        // Retrieve any data from login (optional)
        val name = intent.getStringExtra("name") ?: "Supervisor"

        // Set greeting
        findViewById<TextView>(R.id.tvWelcome).text = "Welcome, $name"
    }
}
