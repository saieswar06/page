package com.example.page

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.page.api.LoginRequest
import com.example.page.api.LoginResponse
import com.example.page.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val CHANNEL_ID = "login_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mobileEt = findViewById<EditText>(R.id.MobileNumber)
        val passwordEt = findViewById<EditText>(R.id.password)
        val loginBtn = findViewById<Button>(R.id.loginButton)
        val googleButton = findViewById<Button>(R.id.googleSignInButton)

        // ✅ Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }

        // ✅ Normal Login Button
        loginBtn.setOnClickListener {
            val mobile = mobileEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()

            if (mobile.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter mobile and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = LoginRequest(mobile = mobile, password = password)

            RetrofitClient.instance.login(request)
                .enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            val data = response.body()!!
                            Toast.makeText(this@MainActivity, "You are logged in successfully!", Toast.LENGTH_LONG).show()
                            showNotification(data.user.email)
                            mobileEt.text.clear()
                            passwordEt.text.clear()
                        } else {
                            Toast.makeText(this@MainActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        Toast.makeText(this@MainActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        // ✅ When “Sign in with Google” is clicked → show popup
        googleButton.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(this)
            builder.setTitle("Sign in with Google")
            builder.setMessage("Enter your Gmail to check if you are registered")

            val input = android.widget.EditText(this)
            input.hint = "Enter your Gmail"
            input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            builder.setView(input)

            builder.setPositiveButton("Sign In") { dialog, _ ->
                val email = input.text.toString().trim()

                if (email.isEmpty()) {
                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                checkEmailInBackend(email)
                dialog.dismiss()
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }
    }

    // ✅ Function to check email in backend
    private fun checkEmailInBackend(email: String) {
        val body = mapOf("email" to email)

        RetrofitClient.instance.checkEmail(body)
            .enqueue(object : retrofit2.Callback<Map<String, String>> {
                override fun onResponse(call: retrofit2.Call<Map<String, String>>, response: retrofit2.Response<Map<String, String>>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Login Successful!", Toast.LENGTH_LONG).show()
                        showNotification(email)
                    } else if (response.code() == 404) {
                        Toast.makeText(this@MainActivity, "Invalid Email — not registered in backend", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Something went wrong (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<Map<String, String>>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ✅ Show success notification
    private fun showNotification(email: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Login Notifications",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Login Successful")
            .setContentText("Welcome, $email")
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(1001, notification)
        }
    }
}
